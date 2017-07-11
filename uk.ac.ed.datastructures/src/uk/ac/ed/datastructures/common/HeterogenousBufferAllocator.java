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

package uk.ac.ed.datastructures.common;

import java.nio.ByteBuffer;

import org.jocl.CL;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_mem;

import uk.ac.ed.accelerator.common.GraalAcceleratorDevice;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorPlatform;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.common.GraalMetaAccelerator;
import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorDevice;

/**
 * Create a Java {@link ByteBuffer} with OpeNCL pinned memory. The resulting buffer resides in the
 * host side of OpenCL.
 *
 * @author juan.fumero
 *
 */
public final class HeterogenousBufferAllocator {

    /*
     * We create pinned memory attached on one device in OpenCL. This is, indeed, in the host side.
     */
    private static cl_context context;
    private static cl_command_queue commandQueue;
    private static OCLGraalAcceleratorDevice device;

    private static boolean init = false;

    /**
     * Allocate memory with pinned memory in OpenCL. This logic corresponds to the host side.
     *
     * @param size
     * @param sizeOfElement
     * @param javaTypes
     * @return {@link ByteBuffer}
     */
    public static ByteBuffer allocateBuffer(int size, int sizeOfElement, JavaDataTypeSizes javaTypes) {
        if (GraalAcceleratorOptions.useACC) {
            return oclBufferAllocationForOpenCL(size, javaTypes);
        } else {
            return ByteBuffer.allocateDirect(size * sizeOfElement);
        }
    }

    private static ByteBuffer oclBufferAllocationForOpenCL(int size, JavaDataTypeSizes javaTypesSize) {
        if (!init) {
            discoverOpenCLPlatforms();
        }
        cl_mem hostBuffer = CL.clCreateBuffer(context, CL.CL_MEM_ALLOC_HOST_PTR, size * javaTypesSize.getOCLSize(), null, null);
        ByteBuffer byteBuffer = CL.clEnqueueMapBuffer(commandQueue, hostBuffer, CL.CL_TRUE, CL.CL_MAP_WRITE, 0, size * javaTypesSize.getOCLSize(), 0, null, null, null);
        byteBuffer.order(device.getEndianess());
        return byteBuffer;
    }

    private static void joinBackGroundDaemonThreads() {
        if (GraalMetaAccelerator.running) {
            try {
                GraalMetaAccelerator.graalInitThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            GraalMetaAccelerator.running = false;
        }

        if (GraalMetaAccelerator.running) {
            try {
                GraalMetaAccelerator.graalInitThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            GraalMetaAccelerator.running = false;
        }
    }

    private static void waitForPlatformInitialization() {
        joinBackGroundDaemonThreads();
        while (!GraalAcceleratorSystem.getInstance().isSystemInitialized()) {
            // wait
        }
        while (GraalAcceleratorSystem.getInstance().getPlatform() == null) {
            // wait
        }
        while (!GraalAcceleratorSystem.getInstance().getPlatform().isDevicesDiscovered()) {
            // wait
        }
    }

    private static GraalAcceleratorPlatform getPlatform() {
        return GraalAcceleratorSystem.getInstance().getPlatform();
    }

    private static int getNumDevicesPlatform() {
        return GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
    }

    private static OCLGraalAcceleratorDevice getFirstDevice() {
        GraalAcceleratorPlatform platform = getPlatform();
        device = (OCLGraalAcceleratorDevice) platform.getDevice();
        return device;
    }

    private static OCLGraalAcceleratorDevice getDevice(int idx) {
        GraalAcceleratorPlatform platform = getPlatform();
        GraalAcceleratorDevice device2 = platform.getDevice(idx);
        return (OCLGraalAcceleratorDevice) device2;
    }

    /**
     * Candidate to move to the platform initialization. Here we only need the first device.
     */
    private static void initAllTheDevicesAvailable() {
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            for (int i = 0; i < getNumDevicesPlatform(); i++) {
                OCLGraalAcceleratorDevice dev = getDevice(i);
                dev.createCommandQueue();
                dev.getContext();
            }
        }
    }

    private static void initValuesFirstDevice() {
        getFirstDevice();
        device.createCommandQueue();
        context = device.getContext();
        commandQueue = device.getCommandQueue();
    }

    private static void discoverOpenCLPlatforms() {
        waitForPlatformInitialization();
        initAllTheDevicesAvailable();
        initValuesFirstDevice();
        init = true;
    }

    // No instance, only use the static methods
    private HeterogenousBufferAllocator() {
    }
}
