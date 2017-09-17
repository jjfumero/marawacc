/*
 * Copyright (c) 2013, 2017, The University of Edinburgh. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package uk.ac.ed.accelerator.wocl;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

import uk.ac.ed.accelerator.cache.OCLBufferMemoryCache;
import uk.ac.ed.accelerator.cache.OCLKernelCache;
import uk.ac.ed.accelerator.cache.OCLKernelPackage;
import uk.ac.ed.accelerator.common.GraalAcceleratorDevice;
import uk.ac.ed.accelerator.common.GraalAcceleratorError;
import uk.ac.ed.accelerator.common.GraalAcceleratorInternalConstants;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorVar;
import uk.ac.ed.accelerator.common.OCLVendor;
import uk.ac.ed.accelerator.common.ParallelSkeleton;
import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.accelerator.utils.LocalStageOCLInfo;
import uk.ac.ed.accelerator.utils.LoggerMarawacc;
import uk.ac.ed.accelerator.utils.OpenCLUtils;
import uk.ac.ed.accelerator.utils.StageInfo;
import uk.ac.ed.accelerator.wocl.PipelineTimeDescritor.Stage;

public class OCLGraalAcceleratorDevice extends GraalAcceleratorDevice {

    // OpenCL specific
    private cl_context context;
    private cl_command_queue queue;
    private cl_device_id device;
    private cl_platform_id platform;
    private OCLVendor vendor;

    private boolean isBuildingProgram;
    @SuppressWarnings("unused") private boolean isCreatingProgram;
    private boolean isCreatingKernel;

    // Data cache
    private int maxOCLStages;
    private int tokenRing;

    ArrayList<OCLGraalAcceleratorVar> clParams;

    private OCLDeviceInfo deviceInfo;

    public OCLGraalAcceleratorDevice(cl_platform_id platform, cl_device_id currentDevice, OCLVendor vendor, long deviceType, int id) {
        super(id);
        this.device = currentDevice;
        this.platform = platform;
        this.vendor = vendor;
        this.type = deviceType;
        this.deviceInfo = new OCLDeviceInfo(platform, currentDevice);
        init();
    }

    private void init() {

        // Properties
        this.name = OpenCLUtils.getString(device, CL.CL_DEVICE_NAME);
        this.vendorName = OpenCLUtils.getString(device, CL.CL_DEVICE_VENDOR);
        this.version = OpenCLUtils.getString(device, CL.CL_DEVICE_VERSION);

        // Flags
        this.isBuildingProgram = false;
        this.isCreatingKernel = false;
        this.isCreatingProgram = false;

        createContext();
    }

    @Override
    public void setDeviceReference(Object deviceReference) {
        this.device = (cl_device_id) deviceReference;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public OCLVendor getVendor() {
        return this.vendor;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public long getType() {
        return this.type;
    }

    @Override
    public String getVendorName() {
        return this.vendorName;
    }

    @Override
    public cl_platform_id getPlatform() {
        return this.platform;
    }

    @Override
    public cl_device_id getDevice() {
        return this.device;
    }

    @Override
    public OCLDeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    // @formatter:off
    /**
     * OpenCL 1.2 specification: cl_device_type - bitfield.
     *
     * See: https://www.khronos.org/registry/cl/api/1.2/cl.h
     *
     *   #define CL_DEVICE_TYPE_DEFAULT                      (1 << 0)
     *   #define CL_DEVICE_TYPE_CPU                          (1 << 1)
     *   #define CL_DEVICE_TYPE_GPU                          (1 << 2)
     *   #define CL_DEVICE_TYPE_ACCELERATOR                  (1 << 3)
     *   #define CL_DEVICE_TYPE_CUSTOM                       (1 << 4)
     *   #define CL_DEVICE_TYPE_ALL                          0xFFFFFFFF
     *
     */
    // @formatter:on
    @Override
    public String getDeviceType() {
        if (type == (1 << 0)) {
            return "DEFAULT";
        } else if (type == (1 << 1)) {
            return "CPU";
        } else if (type == (1 << 2)) {
            return "GPU";
        } else if (type == (1 << 3)) {
            return "ACCELERATOR";
        } else if (type == (1 << 4)) {
            return "CUSTOM";
        } else if (type == 0xFFFFFFFF) {
            return "ALL";
        } else {
            return "UNKNOWN";
        }
    }

    @Override
    protected synchronized cl_context createContext() {

        if (context == null) {
            int[] status = new int[1];

            System.out.println("\tCreating context for device: " + this.device);
            context = CL.clCreateContext(null, 1, new cl_device_id[]{device}, null, null, status);
            if (status[0] != CL.CL_SUCCESS) {
                throw new RuntimeException("[ERROR]: clCreateContext");
            }
        }
        return context;
    }

    @Override
    public synchronized cl_command_queue createCommandQueue() {

        if (context == null) {
            createContext();
        }

        if (queue == null) {
            long properties = CL.CL_QUEUE_PROFILING_ENABLE | CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
            int[] status = new int[1];
            queue = CL.clCreateCommandQueue(context, device, properties, status);
            if (status[0] != CL.CL_SUCCESS) {
                throw new RuntimeException("[ERROR]: clCreateCommandQueue");
            }
        }
        return queue;
    }

    public synchronized cl_command_queue getCommandQueue() {
        if (queue == null) {
            createCommandQueue();
        }
        return this.queue;
    }

    public cl_context getContext() {
        return context;
    }

    @Override
    public synchronized cl_program createProgram(UUID uuidKernel, int cacheIndex) throws RuntimeException {

        isCreatingProgram = true;

        boolean wasKernelRegistered = false;
        wasKernelRegistered = OCLKernelCache.getInstance().isInCache(uuidKernel);

        assert wasKernelRegistered == true;

        OCLKernelPackage oclCache = OCLKernelCache.getInstance().get(uuidKernel, cacheIndex);
        oclCache.setCreatingProgram(true);
        OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);

        if (context == null) {
            LoggerMarawacc.warning("Context is NULL");
            oclCache.setCreatingProgram(false);
            OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);
            return null;
        }

        boolean isProgramCreated = (oclCache.getKernelProgram() == null) ? false : true;

        cl_program program = null;
        if (!isProgramCreated) {
            oclCache.setCreatingProgram(true);
            OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);
            String kernelSource = oclCache.getKernelCode();
            int[] status = new int[1];

            program = CL.clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, status);

            if (status[0] != CL.CL_SUCCESS) {
                GraalAcceleratorError.printError("[ERROR]: clCreateProgramWithSource");
                return null;
            }
            oclCache.setKernelProgram(program);
            isProgramCreated = true;
            oclCache.setCreatingProgram(false);
            OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);
        }

        oclCache.setCreatingProgram(false);
        OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);
        isCreatingProgram = false;
        return program;
    }

    @Override
    public void buildProgram(UUID uuidKernel, int cacheIndex) throws Exception {

        isBuildingProgram = true;

        boolean wasKernelRegistered = false;
        wasKernelRegistered = OCLKernelCache.getInstance().isInCache(uuidKernel);

        if (!wasKernelRegistered) {
            isBuildingProgram = false;
            throw new Exception("Build program exception - the kernel has not been registered");
        }

        OCLKernelPackage oclCache = OCLKernelCache.getInstance().get(uuidKernel, cacheIndex);
        boolean isProgramInCache = oclCache.isBinaryCreated();

        if (isProgramInCache) {
            return;
        }

        cl_program program = oclCache.getKernelProgram();

        if (program == null) {
            isBuildingProgram = false;
            throw new Exception("Program variable is null");
        }

        if (!isProgramInCache) {
            if (GraalAcceleratorOptions.printOCLInfo) {
                System.out.println("[OpenCL] Compiling OpenCL kernel -> " + oclCache.getKernelName() + " :: Kernel #" + cacheIndex);
            }

            oclCache.setBuildingProgram(true);
            OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);

            try {
                long start = System.nanoTime();
                if (GraalAcceleratorOptions.relaxMathKernel) {
                    CL.clBuildProgram(program, 1, new cl_device_id[]{device}, "-cl-fast-relaxed-math", null, null);
                } else {
                    CL.clBuildProgram(program, 1, new cl_device_id[]{device}, null, null, null);
                }
                long total = System.nanoTime() - start;
                Profiler.getInstance().writeInBuffer(ProfilerType.OCL_DRIVER_COMPILE_KERNEL, "total", total);

                long[] binaryDataSizes = new long[1];
                int numDevices = 1;
                CL.clGetProgramInfo(program, CL.CL_PROGRAM_BINARY_SIZES, numDevices * Sizeof.size_t, Pointer.to(binaryDataSizes), null);

                byte[][] binaryDatas = new byte[numDevices][];
                for (int i = 0; i < numDevices; i++) {
                    int binaryDataSize = (int) binaryDataSizes[i];
                    binaryDatas[i] = new byte[binaryDataSize];
                }

                Pointer binarydataPointers = Pointer.to(binaryDatas[0]);
                Pointer pointerToBinaryDataPointer = Pointer.to(binarydataPointers);

                CL.clGetProgramInfo(program, CL.CL_PROGRAM_BINARIES, numDevices * Sizeof.POINTER, pointerToBinaryDataPointer, null);

                oclCache.setBinaryData(binaryDatas);
                oclCache.setBinaryDataSize(binaryDataSizes);
                oclCache.setKernelProgram(program);
                oclCache.setBinaryCreated();
                OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);

            } catch (Exception e) {
                GraalAcceleratorError.printError("[ERROR]: clBuildProgram", e);
                e.printStackTrace();
                isBuildingProgram = false;
                return;
            }
            oclCache.setBuildingProgram(false);
            oclCache.setKernelProgram(program);
            OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);
        }
        isBuildingProgram = false;
    }

    @Override
    public cl_kernel createKernel(UUID uuidKernel, int cacheIndex) {
        isCreatingKernel = true;

        cl_kernel kernel = null;

        boolean wasKernelRegistered = false;
        wasKernelRegistered = OCLKernelCache.getInstance().isInCache(uuidKernel);

        if (!wasKernelRegistered) {
            isCreatingKernel = false;
            return null;
        }

        OCLKernelPackage oclCache = OCLKernelCache.getInstance().get(uuidKernel, cacheIndex);
        boolean isKernelCreated = (oclCache.getKernelBinary() == null) ? false : true;

        if (isKernelCreated) {
            isCreatingKernel = false;
            return oclCache.getKernelBinary();
        } else {
            cl_program program = oclCache.getKernelProgram();
            String kernelName = oclCache.getKernelName();
            kernel = CL.clCreateKernel(program, kernelName, null);
            oclCache.setKernelBinary(kernel);
            OCLKernelCache.getInstance().insert(uuidKernel, cacheIndex, oclCache);
            isCreatingKernel = false;
            return kernel;
        }
    }

    public boolean isBuildingProgram() {
        return isBuildingProgram;
    }

    public boolean isProgramBuilt(UUID uuid, int cacheIndex) {
        return OCLKernelCache.getInstance().get(uuid, cacheIndex).isBinaryCreated();
    }

    public boolean isCreatingProgram(UUID uuid, int cacheIndex) {
        return OCLKernelCache.getInstance().get(uuid, cacheIndex).isCreatingProgram();
    }

    public boolean isCreatingKernel() {
        return isCreatingKernel;
    }

    public cl_kernel getKernel(UUID uuidKernel, int cacheIndex) {
        return OCLKernelCache.getInstance().get(uuidKernel, cacheIndex).getKernelBinary();
    }

    @Override
    public GraalAcceleratorVar registerOffloadVariable(GraalAcceleratorVar oclVar) {
        oclVar.setCommandQueue(queue);
        oclVar.setContext(context);
        oclVar.setDevice(this);
        this.addDeviceVar(oclVar);
        return oclVar;
    }

    @Override
    public void clean() {
        this.isBuildingProgram = false;
        this.isCreatingKernel = false;
        this.isCreatingProgram = false;
    }

    /**
     * Circular buffer. Get the next buffer available following the circular stages.
     *
     */
    private cl_mem[] getOCLListMemory(UUID uuidData, ParallelSkeleton functionOperation) {

        if (functionOperation == ParallelSkeleton.MAP) {
            return OCLBufferMemoryCache.getOCLListMemory(uuidData, tokenRing);

        } else {

            if ((maxOCLStages == GraalAcceleratorInternalConstants.FINAL_STAGE)) {
                tokenRing = GraalAcceleratorInternalConstants.MAX_CONCURRENT_OCL_STAGES;
            } else if (tokenRing > (GraalAcceleratorInternalConstants.MAX_CONCURRENT_OCL_STAGES - 1)) {
                tokenRing = 0;
            }

            cl_mem[] oclMemory = OCLBufferMemoryCache.getOCLListMemory(uuidData, tokenRing);
            tokenRing++;
            return oclMemory;
        }

    }

    private void preAllocationPipeline(ArrayList<OCLGraalAcceleratorVar> oclParams, UUID uuidData, ParallelSkeleton functionOperation) {

        int numOCLParameters = oclParams.size();

        Vector<cl_mem[]> vectorBuffer = new Vector<>();
        tokenRing = 0;

        if (functionOperation == ParallelSkeleton.MAP) {
            cl_mem[] mem = new cl_mem[numOCLParameters];
            int j = 0;
            for (OCLGraalAcceleratorVar oclVariable : oclParams) {
                int chunkPipeline = oclVariable.getArrayLength();
                oclVariable.createDeviceBuffers(chunkPipeline);
                mem[j] = oclVariable.getMemObject();
                j++;
            }
            vectorBuffer.add(mem);
        } else if (functionOperation == ParallelSkeleton.PIPELINE) {
            this.maxOCLStages = GraalAcceleratorInternalConstants.FINAL_STAGE;
            for (int i = 0; i < maxOCLStages; i++) {
                cl_mem[] mem = new cl_mem[numOCLParameters];
                int j = 0;
                for (OCLGraalAcceleratorVar oclVariable : oclParams) {
                    int chunkPipeline = oclVariable.getArrayLength();
                    oclVariable.createDeviceBuffers(chunkPipeline);
                    mem[j] = oclVariable.getMemObject();
                    j++;
                }
                vectorBuffer.add(mem);
            }
        }

        OCLBufferMemoryCache.put(uuidData, vectorBuffer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LocalStageOCLInfo allocateBufferAndWriteBuffer(ArrayList<?> openclParameters, int[] fromTo, UUID uuidData, ParallelSkeleton functionOperation) {

        clParams = (ArrayList<OCLGraalAcceleratorVar>) openclParameters;

        registerVar(clParams);

        // Pre-allocation
        if (!OCLBufferMemoryCache.isInCache(uuidData)) {
            preAllocationPipeline(clParams, uuidData, functionOperation);
        }

        ArrayList<cl_event> eventList = new ArrayList<>();
        ArrayList<Pointer> pointerList = new ArrayList<>();

        cl_mem[] oclMemory = getOCLListMemory(uuidData, functionOperation);

        // Data-transfer (write)
        int i = 0;
        for (OCLGraalAcceleratorVar oclVariable : clParams) {
            cl_mem buffer = new cl_mem();
            buffer = oclMemory[i];
            StageInfo stage = oclVariable.writeBufferByJavaThreadNew(buffer, fromTo);

            if (stage.getOCLevent() != null) {
                oclMemory[i] = stage.getOCLmem();
                eventList.add(stage.getOCLevent());
            }
            pointerList.add(stage.getPointer());
            i++;
        }

        return (new LocalStageOCLInfo(oclMemory, eventList.toArray(new cl_event[eventList.size()]), pointerList.toArray(new Pointer[pointerList.size()])));
    }

    public ArrayList<cl_event> readParametersByJavaThread(ArrayList<OCLGraalAcceleratorVar> params, Pointer[] pointers, cl_event[] eventKernel, int[] fromTo, cl_mem[] memObjects) throws Exception {
        ArrayList<cl_event> readEvents = new ArrayList<>();
        int i = 0;
        for (OCLGraalAcceleratorVar oclVariable : params) {
            if (oclVariable.getDirection() == GraalOCLConstants.COPY_OUT) {
                long start = System.nanoTime();
                PipelineTimeDescritor.getInstance().put(Stage.C_FINE_START_CALL_TO_READ, start);
                cl_event event = oclVariable.readParameterByJavaThread(pointers[i], eventKernel, fromTo, memObjects[i]);
                readEvents.add(event);
            }
            i++;
        }
        return readEvents;
    }

    private void registerVar(ArrayList<OCLGraalAcceleratorVar> params) {
        // Register Vars
        for (int i = 0; i < params.size(); i++) {
            OCLGraalAcceleratorVar var = params.get(i);
            this.registerOffloadVariable(var);
        }
    }

    public ByteOrder getEndianess() {
        long endianness = OpenCLUtils.getLong(device, CL.CL_DEVICE_ENDIAN_LITTLE);
        if (endianness == 0) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    @Override
    public String toString() {
        String str = "Device " + this.id + " : \n";
        str += "  \\_ platform name: " + this.vendor.toString() + " \n";
        str += "  \\_ type:   : " + getDeviceType() + "\n";
        str += "  \\_ platform: " + platform + "\n";
        str += "  \\_ device  : " + device + "\n";
        str += "  \\_ context : " + context + "\n";
        return str;
    }
}
