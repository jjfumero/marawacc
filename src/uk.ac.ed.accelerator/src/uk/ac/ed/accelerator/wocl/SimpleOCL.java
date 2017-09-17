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

import java.util.List;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

import uk.ac.ed.accelerator.common.GraalAcceleratorError;
import uk.ac.ed.accelerator.common.GraalAcceleratorInternalConstants;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.utils.PipelineIndexInfo;

public class SimpleOCL implements GraalAcceleratorInternalConstants {

    private cl_device_id device = null;
    private cl_platform_id platform = null;
    private cl_command_queue queue = null;
    private cl_context context = null;
    private cl_program program = null;
    private cl_kernel kernel = null;
    private cl_mem[] memObjects;
    private cl_event kernelEvent;
    @SuppressWarnings("unused") private boolean isInitialize = false;
    private boolean isProgramBuilt = false;
    private boolean isCommandCreated = false;
    private boolean isContextCreated = false;
    @SuppressWarnings("unused") private boolean isProgramCreated = false;
    private boolean isKernelCreated = false;
    private boolean kernelTimeBenchmarking = false;
    private byte[][] binaryDatas;
    private long[] binaryDataSizes;

    @SuppressWarnings("unused") private static Pointer classPtr;
    private static float[] fictociuous;

    // Variables to block access
    private boolean isCreatingProgram;
    private boolean isBuildingProgram;
    private boolean isCreatingKernel;

    public float[] getFicticious() {
        return fictociuous;
    }

    /**
     * Threads share platform and device. It could be other device in the same platform as well
     */
    public SimpleOCL(cl_platform_id platform, cl_device_id device, boolean kernelTimeBenchmarking) {
        this.platform = platform;
        this.device = device;
        this.kernelTimeBenchmarking = kernelTimeBenchmarking;
    }

    public cl_context oclCreateContext() {
        if (!isContextCreated) {
            cl_context_properties properties = new cl_context_properties();
            properties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);
            try {
                context = CL.clCreateContext(properties, 1, new cl_device_id[]{device}, null, null, null);
            } catch (Exception e) {
                GraalAcceleratorError.printError("[ERROR]: clCreateContext");
                return null;
            }
            isContextCreated = true;
        }
        return context;
    }

    public cl_command_queue oclCreateCommandQueue() {
        if (context == null) {
            return null;
        }
        if (!isCommandCreated) {
            try {
                queue = CL.clCreateCommandQueue(context, device, CL.CL_QUEUE_PROFILING_ENABLE, null);
            } catch (Exception e) {
                GraalAcceleratorError.printError("[ERROR]: clCreateCommandQueue");
                return null;
            }
            isCommandCreated = true;
        }
        return queue;
    }

    public cl_program oclCreateProgram(String kernelSource) {
        isCreatingProgram = true;
        if (context == null) {
            isCreatingProgram = false;
            return null;
        }
        if (!isProgramBuilt) {
            try {
                program = CL.clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, null);
            } catch (Exception e) {
                GraalAcceleratorError.printError("[ERROR]: clCreateProgramWithSource");
                return null;
            }
            isProgramCreated = true;
        } else {
            // Program is in binary format:
            // program = CL.clCreateProgramWithBinary(context, 1, new cl_device_id[]{device},
            // binaryDataSizes, binaryDatas, null, null);
        }
        isCreatingProgram = false;
        return program;
    }

    public void oclBuildProgram() throws Exception {
        isBuildingProgram = true;
        if (program == null) {
            isBuildingProgram = false;
            throw new Exception("Program variable is null");
        }
        if (!isProgramBuilt) {
            try {
                CL.clBuildProgram(program, 1, new cl_device_id[]{device}, null, null, null);
                binaryDataSizes = new long[1];
                int numDevices = 1;
                CL.clGetProgramInfo(program, CL.CL_PROGRAM_BINARY_SIZES, numDevices * Sizeof.size_t, Pointer.to(binaryDataSizes), null);

                binaryDatas = new byte[numDevices][];
                for (int i = 0; i < numDevices; i++) {
                    int binaryDataSize = (int) binaryDataSizes[i];
                    binaryDatas[i] = new byte[binaryDataSize];
                }

                Pointer binarydataPointers = Pointer.to(binaryDatas[0]);
                Pointer pointerToBinaryDataPointer = Pointer.to(binarydataPointers);

                CL.clGetProgramInfo(program, CL.CL_PROGRAM_BINARIES, numDevices * Sizeof.POINTER, pointerToBinaryDataPointer, null);

            } catch (Exception e) {
                GraalAcceleratorError.printError("[ERROR]: clBuildProgram", e);
                isBuildingProgram = false;
                return;
            }
            isProgramBuilt = true;
        } else {
            // CL.clBuildProgram(program, 1, new cl_device_id[]{device}, null, null, null);
        }
        isBuildingProgram = false;
    }

    public cl_mem[] oclAllocateMemory(List<OCLGraalAcceleratorVar> clParams) {
        if (context == null) {
            return null;
        }
        // Allocate the memory objects for the input- and output data
        memObjects = new cl_mem[clParams.size()];
        for (int i = 0; i < memObjects.length; i++) {
            OCLGraalAcceleratorVar clParam = clParams.get(i);
            if (clParam.getObjectClass() != null) {
                memObjects[i] = oclAllocateMemoryStruct(clParam);
            } else {
                long flags = CL.CL_MEM_COPY_HOST_PTR;
                if (clParam.isArray()) {
                    if (clParam.isReadOnly()) {
                        flags |= CL.CL_MEM_READ_ONLY;
                    } else {
                        flags |= CL.CL_MEM_READ_WRITE;
                    }
                } else {
                    flags |= CL.CL_MEM_READ_ONLY;
                }
                memObjects[i] = CL.clCreateBuffer(context, flags, clParam.getType() * clParam.getArrayLength(), clParam.getPointer(), null);
            }
            CL.clSetKernelArg(kernel, i, Sizeof.cl_mem, Pointer.to(memObjects[i]));
        }
        return memObjects;
    }

    @SuppressWarnings("unused")
    private cl_mem oclAllocateMemoryStruct(OCLGraalAcceleratorVar param) {
        // the unique struct available is tuple
        Class<?> klass = param.getObjectClass();

        int n = param.getArrayLength();

        n *= 2;                     // XXX: why 2? It is prepared for Tuple2. Introduce instrocpection in the class
        fictociuous = new float[n];
        classPtr = Pointer.to(fictociuous);
        long flags = 0;
        if (param.isReadOnly()) {
            flags |= CL.CL_MEM_USE_HOST_PTR | CL.CL_MEM_READ_ONLY;
        } else {
            flags |= CL.CL_MEM_READ_WRITE; // FIXME: need three types
        }
        param.setReadOnly(false);
        // timer.start();
        cl_mem oclMem = CL.clCreateBuffer(context, flags, n * Sizeof.cl_float, null, null);
        // timer.end();
        // TimeDescriptor.getInstance().put(TimeDescriptor.Time.TUPLES_BUFFER2,
        // timer.getTotalTimeNanoseconds());
        return oclMem;
    }

    private void launchKernelForBenchmarking() {
        long[] globalWorkSize = new long[]{GraalAcceleratorOptions.workSize};
        for (int i = 0; i < MAX_KERNEL_ITERATIONS; i++) {
            kernelEvent = new cl_event();
            CL.clFinish(queue);
            CL.clEnqueueNDRangeKernel(queue, kernel, 1, null, globalWorkSize, null, 0, null, kernelEvent);
            CL.clFinish(queue);
            CL.clWaitForEvents(1, new cl_event[]{kernelEvent});
            long[] timeStart = new long[1];
            long[] timeEnd = new long[1];
            CL.clGetEventProfilingInfo(kernelEvent, CL.CL_PROFILING_COMMAND_START, Sizeof.cl_long, Pointer.to(timeStart), null);
            CL.clGetEventProfilingInfo(kernelEvent, CL.CL_PROFILING_COMMAND_END, Sizeof.cl_long, Pointer.to(timeEnd), null);
            long totaltime = timeEnd[0] - timeStart[0];
            System.out.println("Kernel Time: " + totaltime + " (ns)");
        }
    }

    public void launchKernel() throws Exception {
        if (context == null) {
            throw new Exception("Program variable is null");
        }
        if (queue == null) {
            throw new Exception("Queue variable is null");
        }
        if (kernel == null) {
            throw new Exception("Kenel variable is null");
        }
        if (GraalAcceleratorOptions.workSize == 0) {
            GraalAcceleratorOptions.workSize = 1;
        }

        if (this.kernelTimeBenchmarking) {
            launchKernelForBenchmarking();
        } else {
            PipelineIndexInfo info = PipelineIndexInfo.getInstance();
            long tid = Thread.currentThread().getId();
            int[] fromTo = info.getFromTo(tid);
            int worksize = fromTo[1] - fromTo[0];
            long[] globalWorkSize = new long[]{worksize};
            cl_event kernelEventPrivate = new cl_event();
            CL.clEnqueueNDRangeKernel(queue, kernel, 1, null, globalWorkSize, null, 0, null, kernelEventPrivate);
            CL.clFinish(queue);
            CL.clWaitForEvents(1, new cl_event[]{kernelEventPrivate});
        }
    }

    public void readParameters(List<OCLGraalAcceleratorVar> clParams) throws Exception {
        if (queue == null) {
            throw new Exception("Queue variable is null");
        }
        for (int i = 0; i < memObjects.length; i++) {
            OCLGraalAcceleratorVar clParam = clParams.get(i);
            if (!clParam.isReadOnly()) {
                CL.clEnqueueReadBuffer(queue, memObjects[i], CL.CL_TRUE, 0, clParam.getType() * clParam.getArrayLength(), clParam.getPointer(), 0, null, null);
            }
        }
    }

    public void free() {
        freeOCLMemory();
        freeOCLObjects();
        clean();
    }

    public void freeOCLObjects() {
        CL.clReleaseKernel(kernel);
        CL.clReleaseProgram(program);
        CL.clReleaseCommandQueue(queue);
        CL.clReleaseContext(context);
    }

    public void freeOCLMemory() {
        for (int i = 0; i < memObjects.length; i++) {
            CL.clReleaseMemObject(memObjects[i]);
        }
    }

    public void clean() {
        isCommandCreated = false;
        isContextCreated = false;
        isKernelCreated = false;
        isProgramCreated = false;
        isProgramBuilt = false;
    }

    public void oclCreateKernel(String kernelName) {
        if (!isKernelCreated) {
            kernel = CL.clCreateKernel(program, kernelName, null);
            isKernelCreated = true;
        }
    }

    // get the kernel time in nanoseconds
    public long getProfilingKernelTimeNanoseconds() {
        if (kernelEvent != null) {
            long[] timeStart = new long[1];
            long[] timeEnd = new long[1];
            CL.clGetEventProfilingInfo(kernelEvent, CL.CL_PROFILING_COMMAND_START, Sizeof.cl_long, Pointer.to(timeStart), null);
            CL.clGetEventProfilingInfo(kernelEvent, CL.CL_PROFILING_COMMAND_END, Sizeof.cl_long, Pointer.to(timeEnd), null);
            long totaltime = timeEnd[0] - timeStart[0];
            // System.out.println("Kernel Time: " + totaltime + " (ns)");
            kernelEvent = null;
            return totaltime;
        } else {
            return -1;
        }
    }

    public boolean isBuildingProgram() {
        return isBuildingProgram;
    }

    public boolean isCreatingProgram() {
        return isCreatingProgram;
    }

    public boolean isCreatingKernel() {
        return isCreatingKernel;
    }
}
