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

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public class OCLWarmingUP extends Thread {

    private boolean emptyKernel;
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_kernel kernel;
    private cl_program program;

    // Kernel with no computation
    private static final String Kernel = "__kernel void warmup() {  }";

    public OCLWarmingUP() {
        emptyKernel = true;
        oclInit();
    }

    private void oclInit() {
        cl_platform_id[] platforms = new cl_platform_id[1];
        CL.clGetPlatformIDs(platforms.length, platforms, null);
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platforms[0]);

        try {
            context = CL.clCreateContextFromType(contextProperties, CL.CL_DEVICE_TYPE_GPU, null, null, null);
            if (context == null) {
                // If no context for a GPU device could be created,
                // try to create one for a CPU device.
                System.out.println("Not GPU");
                context = CL.clCreateContextFromType(contextProperties, CL.CL_DEVICE_TYPE_ALL, null, null, null);
                if (context == null) {
                    System.err.println("Unable to create a context");
                    return;
                }
            }
        } catch (Exception e) {
            context = CL.clCreateContextFromType(contextProperties, CL.CL_DEVICE_TYPE_ALL, null, null, null);
        }

        CL.setExceptionsEnabled(true);

        long[] numBytes = new long[1];
        CL.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, 0, null, numBytes);

        int numDevices = (int) numBytes[0] / Sizeof.cl_device_id;
        cl_device_id[] devices = new cl_device_id[numDevices];
        CL.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, numBytes[0], Pointer.to(devices), null);

        commandQueue = CL.clCreateCommandQueue(context, devices[0], CL.CL_QUEUE_PROFILING_ENABLE, null);

        String programSource = Kernel;

        program = CL.clCreateProgramWithSource(context, 1, new String[]{programSource}, null, null);
        CL.clBuildProgram(program, 0, null, null, null, null);

        kernel = CL.clCreateKernel(program, "warmup", null);
    }

    public synchronized void set(boolean operation) {
        this.emptyKernel = operation;
    }

    @Override
    public void run() {
        long[] globalWorkSize = new long[]{20000};
        while (emptyKernel) {
            CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, null, 0, null, null);
        }
    }
}
