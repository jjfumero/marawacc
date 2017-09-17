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
package uk.ac.ed.jpai.graal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.ac.ed.accelerator.cache.OCLKernelCache;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorPlatform;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.ocl.OCLRuntimeUtils;
import uk.ac.ed.accelerator.ocl.helper.ArrayUtil;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.accelerator.utils.OpenCLUtils;
import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorDevice;
import uk.ac.ed.datastructures.common.AcceleratorPArray;
import uk.ac.ed.datastructures.common.JavaDataTypeSizes;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.cache.GraphCache;

import com.oracle.graal.nodes.StructuredGraph;

public class GraalOpenCLExecutor {

    private int idxCounterIN;
    private int idxKernel;
    private int idxCounterIntermediate;
    private int idxCounterSCOPE;
    private int idxCounterOUT;
    private boolean outputBufferAllocated;

    private ArrayList<ArrayList<cl_mem>> scopedVariableBuffers = new ArrayList<>();
    private ArrayList<ArrayList<ScopeInfoProperties>> scopeVarList = new ArrayList<>();

    @SuppressWarnings("rawtypes") private AcceleratorPArray acceleratorInput = null;
    @SuppressWarnings("rawtypes") private AcceleratorPArray acceleratorOutput = null;
    private AcceleratorPArray<Integer> deoptBuffer = null;
    @SuppressWarnings("rawtypes") private PArray outputArray = null;
    @SuppressWarnings("rawtypes") private PArray outputDeopt = null;

    public long getGlobalMaxGPUMemory() {
        OCLGraalAcceleratorDevice device = (OCLGraalAcceleratorDevice) GraalAcceleratorSystem.getInstance().getPlatform().getDevice();
        return device.getDeviceInfo().getGlobalMemSize();
    }

    public <T> AcceleratorPArray<T> copyToDevice(PArray<T> input, RuntimeObjectTypeInfo inputType) {
        return copyToDevice(input, inputType, input.size(), 0);
    }

    @SuppressWarnings("unchecked")
    public <T> AcceleratorPArray<T> copyToDevice(PArray<T> input, RuntimeObjectTypeInfo inputType, int size, int offset) {

        if (acceleratorInput == null) {
            acceleratorInput = new AcceleratorPArray<>(size, inputType, StorageMode.OPENCL_BYTE_BUFFER, true);

            // link up the ByteBuffer of the array with the cl_mem of acceleratorArray
            acceleratorInput.setArray(input);

            acceleratorInput.setSequence(input.isSequence());
            acceleratorInput.setTotalSize(input.getTotalSizeWhenSequence());

            acceleratorInput.allocateOpenCLBuffer(CL.CL_MEM_READ_ONLY);
        }

        acceleratorInput.copyToDevice(idxCounterIN++, offset);
        return acceleratorInput;
    }

    private <T> void setPrimitiveArray(PArray<T> input, RuntimeObjectTypeInfo runtimeTypeInfo, int idx, RuntimeObjectTypeInfo mainOutputType, int realSize) {
        if (runtimeTypeInfo.getClassObject() == Double.class) {
            double[] out = new double[realSize];
            acceleratorOutput.setDoubleArray(idx, out);
        } else if (runtimeTypeInfo.getClassObject() == Integer.class) {
            int[] out = new int[realSize];
            acceleratorOutput.setIntArray(idx, out);
        } else if (PArray.TUPLESET.contains(runtimeTypeInfo.getClassObject())) {
            // Inspect nested types
            RuntimeObjectTypeInfo[] nestedTypes = mainOutputType.getNestedTypes();
            int i = idx;
            for (RuntimeObjectTypeInfo r : nestedTypes) {
                setPrimitiveArray(input, r, i, mainOutputType, realSize);
                i++;
            }
        } else {
            throw new RuntimeException("Data type not suppoted yet: " + mainOutputType.getClassObject());
        }
    }

    private <T> void allocateAcceleratorPArray(AcceleratorPArray<T> input, RuntimeObjectTypeInfo outputType) {
        if (input.isSequence()) {
            acceleratorOutput = new AcceleratorPArray<>(input.getTotalSizeWhenSequence(), outputType, StorageMode.OPENCL_BYTE_BUFFER, true);
            if (GraalAcceleratorOptions.newPArraysPrimitive) {
                setPrimitiveArray(input, outputType, 0, outputType, input.getTotalSizeWhenSequence());
                acceleratorOutput.setPrimitive(true);
            }
        } else if (input.isPrimitiveArray()) {
            acceleratorOutput = new AcceleratorPArray<>(input.size(), outputType, StorageMode.OPENCL_BYTE_BUFFER, true);
            setPrimitiveArray(input, outputType, 0, outputType, input.size());
            acceleratorOutput.setPrimitive(true);
        } else {
            acceleratorOutput = new AcceleratorPArray<>(input.size(), outputType, StorageMode.OPENCL_BYTE_BUFFER, true);
        }
    }

    @SuppressWarnings("unchecked")
    public <T, R> AcceleratorPArray<R> executeOnTheDevice(StructuredGraph graph, AcceleratorPArray<T> input, RuntimeObjectTypeInfo outputType, Object[] scopeVars) {

        UUID uuidKernel = GraphCache.INSTANCE.getUUID(graph);

        if (acceleratorOutput == null) {
            allocateAcceleratorPArray(input, outputType);
        }

        // Check scope variables and use pinned memory for them
        if (scopeVars != null && scopedVariableBuffers.isEmpty()) {
            createPinnedBuffersForScopedVariables(scopeVars);
        }

        // Create the PArray buffer for deopts
        if (deoptBuffer == null && GraalAcceleratorOptions.deoptGuardsEnabled) {
            boolean partitionForMultidevice = false;
            deoptBuffer = new AcceleratorPArray<>(1, TypeFactory.Integer(), StorageMode.OPENCL_BYTE_BUFFER, partitionForMultidevice);
        }

        if (!outputBufferAllocated) {
            acceleratorOutput.allocateOpenCLBuffer(CL.CL_MEM_WRITE_ONLY);
            outputBufferAllocated = true;
        }
        acceleratorOutput.copyMetaDataToDevice(idxCounterIntermediate++);

        if (GraalAcceleratorOptions.deoptGuardsEnabled) {
            deoptBuffer.allocateOpenCLBuffer(CL.CL_MEM_WRITE_ONLY);
        }

        if ((GraalAcceleratorOptions.debugCacheGPUCode) && (OCLKernelCache.getInstance().isInCache(uuidKernel))) {
            System.out.println("[DEBUG] Kernel in Cache: " + uuidKernel);
        }

        if (!scopeVarList.isEmpty()) {
            writeScopeVars(idxCounterSCOPE++);
        }

        executeKernel(idxKernel++, uuidKernel, input, acceleratorOutput);

        return acceleratorOutput;
    }

    private static void checkStatus(int status, String message) {
        if (status != CL.CL_SUCCESS) {
            System.err.println("[OPENCL ERROR] : " + message);
            System.exit(0);
        }
    }

    /**
     * Set arguments into the OpenCL kernel.
     *
     * @param kernel
     * @param in
     * @param out
     */
    private <inT, outT> void setArguments(cl_kernel kernel, int deviceIndex, AcceleratorPArray<inT> in, AcceleratorPArray<outT> out, AcceleratorPArray<Integer> deopt) {
        int argumentNumber = 0;
        int status = 0;
        for (cl_mem inputBuffer : in.getOpenCLBuffersWithMetadata(deviceIndex)) {
            status |= CL.clSetKernelArg(kernel, argumentNumber, Sizeof.cl_mem, Pointer.to(inputBuffer));
            argumentNumber++;
        }
        if (scopedVariableBuffers != null && scopedVariableBuffers.size() >= 1) {
            for (cl_mem scopedBuffer : scopedVariableBuffers.get(deviceIndex)) {
                status |= CL.clSetKernelArg(kernel, argumentNumber, Sizeof.cl_mem, Pointer.to(scopedBuffer));
                argumentNumber++;
            }
        }
        for (cl_mem outputBuffer : out.getOpenCLBuffersWithMetadata(deviceIndex)) {
            status |= CL.clSetKernelArg(kernel, argumentNumber, Sizeof.cl_mem, Pointer.to(outputBuffer));
            argumentNumber++;
        }

        if (deopt != null) {
            cl_mem d = deopt.getOpenCLBuffersWithMetadata(deviceIndex).get(0);
            status |= CL.clSetKernelArg(kernel, argumentNumber, Sizeof.cl_mem, Pointer.to(d));
            argumentNumber++;
        }
        checkStatus(status, "clSetKernelArgs");
    }

    private static GraalAcceleratorPlatform getGraalOCLPlatform() {
        // There is no guarantee the GraalAcceleratorPlatform is prepared. The check is needed
        try {
            OCLRuntimeUtils.waitForTheOpenCLInitialization();
        } catch (InterruptedException e) {
            throw new RuntimeException("OpenCL device not found");
        }
        GraalAcceleratorSystem system = GraalAcceleratorSystem.getInstance();
        GraalAcceleratorPlatform platform = system.getPlatform();
        return platform;
    }

    /**
     * For multiple device.
     */
    private static OCLGraalAcceleratorDevice getOpenCLDevice(int idx) {
        GraalAcceleratorPlatform platform = getGraalOCLPlatform();
        return (OCLGraalAcceleratorDevice) platform.getDevice(idx);
    }

    /**
     * Compute local work size.
     *
     * @param kernel
     * @param size
     * @return long[]
     */
    private static long[] computeLocalWorkSize(cl_kernel kernel, int idx, long size) {
        long[] localWorkSize = new long[1];
        cl_device_id device = getOpenCLDevice(idx).getDevice();
        long[] kernelWorkGroupSize = new long[1];
        CL.clGetKernelWorkGroupInfo(kernel, device, CL.CL_KERNEL_WORK_GROUP_SIZE, Sizeof.cl_ulong, Pointer.to(kernelWorkGroupSize), null);
        if (kernelWorkGroupSize[0] > size) {
            localWorkSize[0] = size;
        } else {
            localWorkSize[0] = kernelWorkGroupSize[0];
        }
        return localWorkSize;
    }

    private <inT, outT> void executeKernelIntoDevice(int kernelIndex, int deviceIndex, cl_kernel kernel, AcceleratorPArray<inT> input, AcceleratorPArray<outT> outputLocal) {

        setArguments(kernel, deviceIndex, input, outputLocal, deoptBuffer);

        long size = input.isSequence() ? input.getTotalSizeWhenSequence() : input.size();
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            int numMaxDevices = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
            if (deviceIndex > 0) {
                size = (size / numMaxDevices) + (size % numMaxDevices);
            } else {
                size = (size / numMaxDevices);
            }
        }

        long[] globalWorkSize = new long[]{size};
        long[] localWorkSize = computeLocalWorkSize(kernel, deviceIndex, size);

        cl_command_queue commandQueue = getOpenCLDevice(deviceIndex).getCommandQueue();

        if (GraalAcceleratorOptions.printOCLInfo) {
            // System.out.println("[OpenCL] Running kernel with #" + size + " threads");
        }

        // Run the kernel
        cl_event kernelEvent = new cl_event();
        int status;
        if ((size % localWorkSize[0]) != 0) {
            localWorkSize = null;
        }

        status = CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, kernelEvent);
        checkStatus(status, "clEnqueueNDRangeKernel");
        status = CL.clFlush(commandQueue);
        checkStatus(status, "clFlush Kernel");

        if (GraalAcceleratorOptions.profileOffload) {
            CL.clSetEventCallback(kernelEvent, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_KERNEL, kernelIndex, deviceIndex), null);
        }
    }

    private <inT, outT> void executeKernel(int kernelIndex, UUID uuidKernel, AcceleratorPArray<inT> input, AcceleratorPArray<outT> outputLocal) {

        int numKernels = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            numKernels = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }

        for (int i = 0; i < numKernels; i++) {
            // System.out.println("Running kernel: " + i);
            cl_kernel kernel = OCLKernelCache.getInstance().get(uuidKernel, i).getKernelBinary();
            executeKernelIntoDevice(kernelIndex, i, kernel, input, outputLocal);
        }
    }

    private <T> void setPrimitiveArrayOUT(PArray<T> input, RuntimeObjectTypeInfo runtimeTypeInfo, int idx, RuntimeObjectTypeInfo outputType, int offset) {
        if (runtimeTypeInfo.getClassObject() == Integer.class) {
            outputArray.setIntArray(idx, input.asIntegerArray(idx));
        } else if (runtimeTypeInfo.getClassObject() == Double.class) {
            outputArray.setDoubleArray(idx, input.asDoubleArray(idx));
        } else if (PArray.TUPLESET.contains(runtimeTypeInfo.getClassObject())) {
            // Inspect nested types
            RuntimeObjectTypeInfo[] nestedTypes = runtimeTypeInfo.getNestedTypes();
            int i = idx;
            for (RuntimeObjectTypeInfo r : nestedTypes) {
                setPrimitiveArrayOUT(input, r, i, outputType, offset);
                i++;
            }
        } else {
            throw new RuntimeException("Data type not supported yet: " + outputType.getClassObject());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> PArray<T> copyToHost(PArray<T> input, RuntimeObjectTypeInfo outputType) {

        if (outputArray == null) {
            if (input.isPrimitiveArray()) {
                // no allocation
                outputArray = new PArray<>(input.size(), outputType, StorageMode.OPENCL_BYTE_BUFFER, false);
                setPrimitiveArrayOUT(input, outputType, 0, outputType, 0);
                outputArray.setPrimitive(true);
            } else {
                outputArray = new PArray<>(input.size(), outputType, StorageMode.OPENCL_BYTE_BUFFER, true);
            }
        }

        // link up the cl_mem from input with the ByteBuffer from output
        ((AcceleratorPArray<T>) input).setArray(outputArray);
        ((AcceleratorPArray<T>) input).copyToHost(idxCounterOUT++);

        return outputArray;
    }

    @SuppressWarnings("unchecked")
    public PArray<Integer> getDeoptBuffer() {
        if (GraalAcceleratorOptions.deoptGuardsEnabled) {

            if (outputDeopt == null) {
                outputDeopt = new PArray<>(1, TypeFactory.Integer(), StorageMode.OPENCL_BYTE_BUFFER, true);
            }
            // link up the cl_mem from input with the ByteBuffer from output
            deoptBuffer.setArray(outputDeopt);
            deoptBuffer.copyToHost(idxCounterOUT);

            return outputDeopt;
        } else {
            return null;
        }
    }

    private static class ScopeInfoProperties {
        private Pointer pointer;
        private int size;

        public ScopeInfoProperties(Pointer pointer, int size) {
            this.pointer = pointer;
            this.size = size;
        }

        public Pointer getPointer() {
            return pointer;
        }

        public int getSize() {
            return size;
        }
    }

    private static boolean isArraySimple(Class<?> array) {
        if (array == float[].class) {
            return true;
        } else if (array == double[].class) {
            return true;
        } else if (array == int[].class) {
            return true;
        } else if (array == short[].class) {
            return true;
        } else if (array == int[].class) {
            return true;
        } else if (array == long[].class) {
            return true;
        } else if (array == boolean[].class) {
            return true;
        }
        return false;
    }

    private class SizeBuffer {
        public int size;
        public int totalSize;

        public SizeBuffer(int size, int totalSize) {
            this.size = size;
            this.totalSize = totalSize;
        }

        public int getSize() {
            return size;
        }

        public int getTotalSize() {
            return totalSize;
        }
    }

    private SizeBuffer getSizes(String type, Object array) {
        int size;
        int totalSize;
        if (type.equals("[I")) {
            size = ((int[]) array).length;
            totalSize = JavaDataTypeSizes.INT.getOCLSize() * ((int[]) array).length;
        } else if (type.equals("[J")) {
            size = ((long[]) array).length;
            totalSize = JavaDataTypeSizes.LONG.getOCLSize() * ((long[]) array).length;
        } else if (type.equals("[F")) {
            size = ((float[]) array).length;
            totalSize = JavaDataTypeSizes.FLOAT.getOCLSize() * ((float[]) array).length;
        } else if (type.equals("[D")) {
            size = ((double[]) array).length;
            totalSize = JavaDataTypeSizes.DOUBLE.getOCLSize() * ((double[]) array).length;
        } else if (type.equals("[S")) {
            size = ((short[]) array).length;
            totalSize = JavaDataTypeSizes.SHORT.getOCLSize() * ((short[]) array).length;
        } else {
            throw new NotImplementedException();
        }
        return new SizeBuffer(size, totalSize);
    }

    public long getLong(cl_device_id device, int paramName) {
        return getLongs(device, paramName, 1)[0];
    }

    public long[] getLongs(cl_device_id device, int paramName, int numValues) {
        long[] values = new long[numValues];
        CL.clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues, Pointer.to(values), null);
        return values;
    }

    public ByteOrder getEndianess() {
        long endianness = getLong(getOpenCLDevice(0).getDevice(), CL.CL_DEVICE_ENDIAN_LITTLE);
        if (endianness == 0) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    /**
     * Create pinned memory for the scope variables.
     */
    private ByteBuffer createHostPinnedMemoryForVarScope(int totalSize) {
        cl_mem hostData = CL.clCreateBuffer(getOpenCLDevice(0).getContext(), CL.CL_MEM_ALLOC_HOST_PTR, totalSize, null, null);
        ByteOrder order = getEndianess();
        ByteBuffer pinnedData = CL.clEnqueueMapBuffer(getOpenCLDevice(0).getCommandQueue(), hostData, CL.CL_FALSE, CL.CL_MAP_WRITE, 0, totalSize, 0, null, null, null);
        pinnedData.order(order);
        return pinnedData;
    }

    private static void copyDataIntoPinnedBuffer(Object array, ByteBuffer pinnedData, int size) {
        // Copy data
        if (array.getClass() == byte[].class) {
            // directly passed to the pinned buffer
            pinnedData.put((byte[]) array);
        } else {
            for (int i = 0; i < size; i++) {
                if (array.getClass() == float[].class) {
                    pinnedData.putFloat(i * JavaDataTypeSizes.FLOAT.getOCLSize(), ((float[]) array)[i]);
                } else if (array.getClass() == int[].class) {
                    pinnedData.putInt(i * JavaDataTypeSizes.INT.getOCLSize(), ((int[]) array)[i]);
                } else if (array.getClass() == long[].class) {
                    pinnedData.putLong(i * JavaDataTypeSizes.LONG.getOCLSize(), ((long[]) array)[i]);
                } else if (array.getClass() == short[].class) {
                    pinnedData.putShort(i * JavaDataTypeSizes.SHORT.getOCLSize(), ((short[]) array)[i]);
                } else if (array.getClass() == double[].class) {
                    pinnedData.putDouble(i * JavaDataTypeSizes.DOUBLE.getOCLSize(), ((double[]) array)[i]);
                }
            }
        }
    }

    private void createOpenCLBuffersForScopeVars(ByteBuffer pinnedData, int totalSize, int deviceIndex) {

        cl_context context = getOpenCLDevice(deviceIndex).getContext();

        ArrayList<cl_mem> clMemList = new ArrayList<>();
        ArrayList<ScopeInfoProperties> propertiesList = new ArrayList<>();

        // Data buffer with pinned memory
        cl_mem dataBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, totalSize, null, null);
        Pointer dataPtr = Pointer.to(pinnedData);

        clMemList.add(dataBuffer);

        ScopeInfoProperties info = new ScopeInfoProperties(dataPtr, totalSize);
        propertiesList.add(info);

        // utility buffer buffer
        int[] utilityArray = {totalSize, 0};
        cl_mem utilityBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, JavaDataTypeSizes.INT.getOCLSize() * 2, null, null);
        Pointer utilityPtr = Pointer.to(utilityArray);
        clMemList.add(utilityBuffer);

        ScopeInfoProperties info2 = new ScopeInfoProperties(utilityPtr, JavaDataTypeSizes.INT.getOCLSize() * 2);
        propertiesList.add(info2);

        scopedVariableBuffers.get(deviceIndex).addAll(clMemList);
        scopeVarList.get(deviceIndex).addAll(propertiesList);
    }

    private void initScopeLists(int maxDevices) {
        if (scopedVariableBuffers.isEmpty()) {
            for (int i = 0; i < maxDevices; i++) {
                ArrayList<cl_mem> aux = new ArrayList<>();
                scopedVariableBuffers.add(aux);
            }
        }
        if (scopeVarList.isEmpty()) {
            for (int i = 0; i < maxDevices; i++) {
                ArrayList<ScopeInfoProperties> aux = new ArrayList<>();
                scopeVarList.add(aux);
            }
        }
    }

    private void createPinnedBuffersForScopedVariables(Object[] extraParametersFromTheScope) {

        int numDevices = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            numDevices = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }

        initScopeLists(numDevices);

        for (Object scopedVar : extraParametersFromTheScope) {
            if (scopedVar.getClass().isArray()) {
                // get read only flat reference
                Object array = null;
                if (isArraySimple(scopedVar.getClass())) {
                    array = scopedVar;
                } else {
                    // Flatten only if it is > 1D
                    // NOTE: This is time consuming, but there is no other way of the Java user
                    // passes an array > 1D as input.
                    array = ArrayUtil.flattenArray(scopedVar, true);
                }

                String type = array.getClass().getName();
                SizeBuffer sizeBuffer = getSizes(type, array);
                int size = sizeBuffer.getSize();
                int totalSize = sizeBuffer.getTotalSize();
                ByteBuffer pinnedData = createHostPinnedMemoryForVarScope(totalSize);

                copyDataIntoPinnedBuffer(array, pinnedData, size);

                for (int i = 0; i < numDevices; i++) {
                    // System.out.println("Allocating OpenCL pinned memory for scope vars -> " + i);
                    // int sizeBuf = getSizeForPartition(totalSize, numDevices, i);

                    // Note: scope variables are copied entirely
                    createOpenCLBuffersForScopeVars(pinnedData, totalSize, i);
                }

            } else {
                throw new UnsupportedOperationException("Scope var not supported. Only supported arrays and scalars.");
            }
        }
    }

    private static void writeIntoBuffer(int deviceIndex, int writeIndex, cl_command_queue commandQueue, ArrayList<ScopeInfoProperties> scopeVarDeviceList, ArrayList<cl_mem> scopeBufferDeviceList) {
        int bufferIndex = 0;
        for (ScopeInfoProperties s : scopeVarDeviceList) {
            cl_event writeEvent = new cl_event();
            CL.clEnqueueWriteBuffer(commandQueue, scopeBufferDeviceList.get(bufferIndex), CL.CL_FALSE, 0, s.getSize(), s.getPointer(), 0, null, writeEvent);
            CL.clFlush(commandQueue);
            bufferIndex++;
            if (GraalAcceleratorOptions.profileOffload) {
                CL.clSetEventCallback(writeEvent, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_WRITE_BUFFER, writeIndex, deviceIndex), null);
            }
        }
    }

    private void writeScopeVars(int writeIndex) {

        int numDevices = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            numDevices = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }
        for (int i = 0; i < numDevices; i++) {
            cl_command_queue queue = getOpenCLDevice(i).getCommandQueue();
            writeIntoBuffer(i, writeIndex, queue, scopeVarList.get(i), scopedVariableBuffers.get(i));
        }
    }

    public void setNewAllocation(boolean newAllocation) {
        if (newAllocation) {
            acceleratorInput = null;
            acceleratorOutput = null;
            outputArray = null;
            outputDeopt = null;
            outputBufferAllocated = false;
        }
    }
}
