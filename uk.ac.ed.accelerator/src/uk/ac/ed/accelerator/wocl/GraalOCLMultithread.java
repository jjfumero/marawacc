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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.jocl.CL;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

import uk.ac.ed.accelerator.cache.OCLKernelPackage;
import uk.ac.ed.accelerator.common.GraalAcceleratorError;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.OCLVendor;
import uk.ac.ed.accelerator.utils.VirtualIDManager;

public final class GraalOCLMultithread implements GraalAcceleratorError {

    /**
     * Graal OpenCL API. GraalOCL class is a singleton to initialise the platform and take advantage
     * of cache system. If the kernel was created previously, it uses the binary compiled
     * previously.
     */
    private static GraalOCLMultithread instance = null;

    private String envPlatform;
    private cl_device_id device = null;
    private cl_platform_id platform = null;
    private boolean isInitialize = false;
    private boolean kernelTimeBenchmarking = false;

    private OCLWarmingUP daemon;

    private HashMap<Integer, SimpleOCL> threads;
    private HashMap<Integer, OCLKernelPackage> oclCache;

    public float[] getFicticious() {
        long tid = Thread.currentThread().getId();
        return threads.get(tid).getFicticious();
    }

    public static GraalOCLMultithread getIntance() {
        if (instance == null) {
            instance = new GraalOCLMultithread();
        }
        return instance;
    }

    private GraalOCLMultithread() {
        initializePlatform();
    }

    private void createAndStartDaemon() {
        daemon = new OCLWarmingUP();
        daemon.start();
    }

    private void initializePlatform() {

        String warmup = System.getenv("GRAAL_WARMINGUP");
        if (warmup != null) {
            if (warmup.equals("1")) {
                createAndStartDaemon();
            } else {
                daemon = null;
            }
        } else if (GraalAcceleratorOptions.offloadWarmingUp) {
            createAndStartDaemon();
        }

        long deviceType = GraalAcceleratorOptions.useCPU ? CL.CL_DEVICE_TYPE_CPU : CL.CL_DEVICE_TYPE_GPU;
        // Object with all devices available in the system
        OpenCLDevices graalDevices = OpenCLDevices.getInstance();
        envPlatform = System.getenv("GRAAL_GPU_PLATFORM");

        boolean filterGPUType = false;
        OCLVendor vendor = null;
        if ((envPlatform != null) && (envPlatform.equals("NVIDIA"))) {
            filterGPUType = true;
            vendor = OCLVendor.NVIDIA;
        } else if ((envPlatform != null) && (envPlatform.equals("AMD"))) {
            filterGPUType = true;
            vendor = OCLVendor.AMD;
        }

        if (!isInitialize) {
            try {
                if (filterGPUType) {
                    device = (cl_device_id) graalDevices.getDeviceByPlatformType(deviceType, vendor).getDevice();
                    platform = (cl_platform_id) graalDevices.getDeviceByPlatformType(deviceType, vendor).getPlatform();
                    System.out.println("Using: " + graalDevices.getDeviceByPlatformType(deviceType, vendor));
                } else {
                    device = (cl_device_id) graalDevices.getDeviceByTypeAt(0, deviceType).getDevice();
                    platform = (cl_platform_id) graalDevices.getDeviceByTypeAt(0, deviceType).getPlatform();
                }
            } catch (Exception e) {
                // Trying with the first device available
                System.out.println("[WARNING] DeviceType " + getType(deviceType) + " not available.");
                device = (cl_device_id) graalDevices.getListDevices().get(0).getDevice();
                platform = (cl_platform_id) graalDevices.getListDevices().get(0).getPlatform();
            }
            isInitialize = true;
        }

        String kernelTime = System.getenv("GRAAL_OCL_KERNEL");
        if ((kernelTime != null) && (kernelTime.equals("1"))) {
            this.kernelTimeBenchmarking = true;
        }
    }

    private static String getType(long deviceType) {
        if (deviceType == CL.CL_DEVICE_TYPE_CPU) {
            return "CL_DEVICE_TYPE_CPU";
        }
        if (deviceType == CL.CL_DEVICE_TYPE_GPU) {
            return "CL_DEVICE_TYPE_GPU";
        }
        return "UNKNOW";
    }

    public cl_context oclCreateContext() {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        if (threads == null) {
            threads = new HashMap<>();
            oclCache = new HashMap<>();
        }
        if (!threads.containsKey(virtualID)) {
            SimpleOCL oclThread = new SimpleOCL(platform, device, kernelTimeBenchmarking);
            threads.put(virtualID, oclThread);
            OCLKernelPackage cache = new OCLKernelPackage();
            oclCache.put(virtualID, cache);
        }
        return threads.get(virtualID).oclCreateContext();
    }

    public cl_command_queue oclCreateCommandQueue() {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        return threads.get(virtualID).oclCreateCommandQueue();
    }

    public cl_program oclCreateProgram(String kernelSource, @SuppressWarnings("unused") UUID uuidKernel) {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        cl_program program = threads.get(virtualID).oclCreateProgram(kernelSource);
        oclCache.get(virtualID).setKernelProgram(program);
        return program;
    }

    public void oclBuildProgram() throws Exception {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        threads.get(virtualID).oclBuildProgram();
    }

    public cl_mem[] oclAllocateMemory(List<OCLGraalAcceleratorVar> clParams) {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        return threads.get(virtualID).oclAllocateMemory(clParams);
    }

    public void launchKernel() throws Exception {
        if (daemon != null) {
            daemon.set(false);
        }
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        threads.get(virtualID).launchKernel();
        if (daemon != null) {
            daemon.join();
        }
    }

    public void readParameters(List<OCLGraalAcceleratorVar> clParams) throws Exception {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        threads.get(virtualID).readParameters(clParams);
    }

    public void free() {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        threads.get(virtualID).free();
    }

    public void freeOCLObjects() {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        threads.get(virtualID).freeOCLObjects();
    }

    public void freeOCLMemory() {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        threads.get(virtualID).freeOCLMemory();
    }

    public static void clean() {
        long tid = Thread.currentThread().getId();
        @SuppressWarnings("unused")
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        // threads.get(virtualID).clean();
    }

    public void oclCreateKernel(String kernelName) {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        threads.get(virtualID).oclCreateKernel(kernelName);
    }

    public void setCodeInCache(String oclCode) {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        oclCache.get(virtualID).setKernelCode(oclCode);
    }

    /*
     * Get the OCLCache for Thread id
     */
    public OCLKernelPackage getOCLCache() {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        return oclCache.get(virtualID);
    }

    /*
     * Get the kernel time in nanoseconds
     */
    public long getProfilingKernelTimeNanoseconds() {
        long tid = Thread.currentThread().getId();
        int virtualID = VirtualIDManager.getInstance().getVirtualID(tid);
        return threads.get(virtualID).getProfilingKernelTimeNanoseconds();
    }
}
