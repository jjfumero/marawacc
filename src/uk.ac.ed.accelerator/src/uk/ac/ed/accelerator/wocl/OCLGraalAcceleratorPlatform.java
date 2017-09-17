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

import java.util.ArrayList;

import org.jocl.CL;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import uk.ac.ed.accelerator.cache.OCLKernelPackage;
import uk.ac.ed.accelerator.common.GraalAcceleratorDevice;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorPlatform;
import uk.ac.ed.accelerator.common.OCLVendor;

/**
 * Graal Accelerator Platform for OpenCL. It contains the list of real OpenCL platforms and devices
 * available and reachable. This is single-device, therefore it keeps the current device and
 * platform in private variables.
 *
 */
public class OCLGraalAcceleratorPlatform extends GraalAcceleratorPlatform {

    private String envGPUPlatform;
    private String envCPUPlatform;
    private cl_device_id device = null;
    private cl_platform_id platform = null;
    private long deviceType;
    private OCLVendor vendor;
    private OpenCLDevices devices;

    private OCLKernelPackage oclCache;
    private OCLWarmingUP daemon;

    @Override
    public int getNumCurrentCurrentDevices() {
        return graalDevices.size();
    }

    @Override
    public int getNumTotalDevices() {
        return devices.getListDevices().size();
    }

    public OCLGraalAcceleratorPlatform() {
        super();
        devices = OpenCLDevices.getInstance();
        initializePlatform();
        initializeCache();
    }

    private void createAndStartKernelDaemon() {
        daemon = new OCLWarmingUP();
        daemon.start();
    }

    private void getEnvironmentVariableWarmingUp() {
        String warmup = System.getenv("GRAAL_WARMINGUP");
        if (warmup != null) {
            if (warmup.equals("1") || warmup.equals("TRUE") || warmup.equals("true")) {
                createAndStartKernelDaemon();
            } else {
                daemon = null;
            }
        } // Run the daemon also for OffloafWarningUp from VM
        else if (GraalAcceleratorOptions.offloadWarmingUp) {
            createAndStartKernelDaemon();
        }
    }

    private boolean getVendorPlatform(boolean filterGPUType) {
        // Option specific by using: -XX:+UseACC in the VM
        if (!filterGPUType && GraalAcceleratorOptions.useACC) {
            if (GraalAcceleratorOptions.printOCLInfo) {
                System.out.println("Using Accelerator (Exploring AMD by default)");
            }
            vendor = OCLVendor.AMD;
            return true;
        } else if (!GraalAcceleratorOptions.useACC) {
            if (GraalAcceleratorOptions.printOCLInfo) {
                System.out.println("Using Intel CPU");
            }
            return false;
        }
        return false;
    }

    private void initPlatformAndDeviceByVendor() {
        platform = (cl_platform_id) devices.getDeviceByPlatformType(deviceType, vendor).getPlatform();
        device = (cl_device_id) devices.getDeviceByPlatformType(deviceType, vendor).getDevice();

        if (!GraalAcceleratorOptions.multiOpenCLDevice && GraalAcceleratorOptions.printOCLInfo) {
            System.out.println("Using: " + devices.getDeviceByPlatformType(deviceType, vendor));
        }
    }

    private void initializeCPUPlatform(boolean message) {
        // Trying with the first device available
        if (!GraalAcceleratorOptions.multiOpenCLDevice && message && GraalAcceleratorOptions.printOCLInfo) {
            System.out.println("[WARNING] DeviceType CL_DEVICE_TYPE_GPU not available, Using first in the list: \n " + devices.getListDevices().get(0));
        }
        device = (cl_device_id) devices.getListDevices().get(0).getDevice();
        platform = (cl_platform_id) devices.getListDevices().get(0).getPlatform();
    }

    private static String getBoolean(String property) {
        if (System.getProperty(property) == null) {
            return null;
        } else {
            return (System.getProperty(property).toLowerCase());
        }
    }

    private static String getCPUPlatformName() {
        String platformName = System.getenv("GRAAL_CPU_PLATFORM");
        if (platformName == null) {
            // Get property (-Dmarwacc.cpu.platform=intel | -Dmarawacc.cpu.platform=amd)
            platformName = getBoolean("marawacc.cpu.platform");
        }
        return platformName;
    }

    private static String getGPUPlatformName() {
        String platformName = System.getenv("GRAAL_GPU_PLATFORM");
        if (platformName == null) {
            // Try to get property
            // (-Dmarwacc.gpu.platform=nvidia | -Dmarawacc.gpu.platform=amd)
            platformName = getBoolean("marawacc.gpu.platform");
        }
        return platformName;
    }

    private void getFirstOpenCLDevice(boolean filterGPUType, boolean filterCPUType) {
        if (filterGPUType) {
            // First try is with AMD if UseACC is enabled
            this.deviceType = CL.CL_DEVICE_TYPE_GPU;
            initPlatformAndDeviceByVendor();
            OCLGraalAcceleratorDevice dev = (OCLGraalAcceleratorDevice) devices.getDeviceByPlatformType(deviceType, vendor);
            this.graalDevices.add(dev);
        } else if (filterCPUType) {
            this.deviceType = CL.CL_DEVICE_TYPE_CPU;
            initPlatformAndDeviceByVendor();
            OCLGraalAcceleratorDevice dev = (OCLGraalAcceleratorDevice) devices.getDeviceByPlatformType(deviceType, vendor);
            this.graalDevices.add(dev);
        } else {
            // Filter is not set, therefore we try the first OpenCL CPU available.
            this.deviceType = CL.CL_DEVICE_TYPE_CPU;
            vendor = devices.getListDevices().get(0).getVendor();
            OCLGraalAcceleratorDevice dev = (OCLGraalAcceleratorDevice) devices.getListDevices().get(0);
            this.graalDevices.add(dev);
            initializeCPUPlatform(false);
        }
    }

    private void recoverInitializationAndTryGPUAndCPUFromDefaultFlags() {
        if (GraalAcceleratorOptions.useACC) {
            try {
                vendor = OCLVendor.NVIDIA;
                this.deviceType = CL.CL_DEVICE_TYPE_GPU;
                initPlatformAndDeviceByVendor();
                OCLGraalAcceleratorDevice dev = (OCLGraalAcceleratorDevice) devices.getDeviceByPlatformType(deviceType, vendor);
                graalDevices.add(dev);
            } catch (Exception e) {
                this.deviceType = CL.CL_DEVICE_TYPE_CPU;
                vendor = devices.getListDevices().get(0).getVendor();
                OCLGraalAcceleratorDevice dev = (OCLGraalAcceleratorDevice) devices.getListDevices().get(0);
                graalDevices.add(dev);
                initializeCPUPlatform(true);
            }
        } else {
            this.deviceType = CL.CL_DEVICE_TYPE_CPU;
            vendor = devices.getListDevices().get(0).getVendor();
            OCLGraalAcceleratorDevice dev = (OCLGraalAcceleratorDevice) devices.getListDevices().get(0);
            graalDevices.add(dev);
            initializeCPUPlatform(true);
        }
    }

    private void initializePlatform() {

        getEnvironmentVariableWarmingUp();

        this.deviceType = GraalAcceleratorOptions.useCPU ? CL.CL_DEVICE_TYPE_CPU : CL.CL_DEVICE_TYPE_GPU;

        // Object with all devices available in the system
        this.envCPUPlatform = getCPUPlatformName();

        boolean filterCPUType = false;
        if ((envCPUPlatform != null) && (envCPUPlatform.toLowerCase().equals("intel"))) {
            filterCPUType = true;
            this.deviceType = CL.CL_DEVICE_TYPE_CPU;
            vendor = OCLVendor.INTEL;
        } else if ((envCPUPlatform != null) && (envCPUPlatform.toLowerCase().equals("amd"))) {
            filterCPUType = true;
            this.deviceType = CL.CL_DEVICE_TYPE_CPU;
            vendor = OCLVendor.AMD;
        }

        boolean filterGPUType = false;
        if (filterCPUType == false) {
            // Object with all devices available in the system
            this.envGPUPlatform = getGPUPlatformName();
            if ((envGPUPlatform != null) && (envGPUPlatform.toLowerCase().equals("nvidia"))) {
                filterGPUType = true;
                this.deviceType = CL.CL_DEVICE_TYPE_GPU;
                vendor = OCLVendor.NVIDIA;
            } else if ((envGPUPlatform != null) && (envGPUPlatform.toLowerCase().equals("AMD"))) {
                filterGPUType = true;
                this.deviceType = CL.CL_DEVICE_TYPE_GPU;
                vendor = OCLVendor.AMD;
            }
        }

        if (!filterGPUType && !filterCPUType) {
            filterGPUType = getVendorPlatform(filterGPUType);
        }

        // We first try to find an available GPU.
        // We pick up the first on the list of GPU available.
        // If the OpenCL platform does not find GPUs, we try to get the first OpeNCL CPU
        // available.
        try {
            getFirstOpenCLDevice(filterGPUType, filterCPUType);
        } catch (RuntimeException e) {
            recoverInitializationAndTryGPUAndCPUFromDefaultFlags();
        }

        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            if (devices.isMultideviceAvailable()) {
                this.graalDevices.clear();
                ArrayList<GraalAcceleratorDevice> firstDevicesForMultiGPU = devices.getFirstDevicesForMultiGPU();

                if (GraalAcceleratorOptions.printOCLInfo) {
                    System.out.println("[INFO] Multi-device support, using:");
                }

                for (GraalAcceleratorDevice dev : firstDevicesForMultiGPU) {
                    this.graalDevices.add(dev);
                    if (GraalAcceleratorOptions.printOCLInfo) {
                        System.out.println(dev);
                    }
                }

            } else {
                if (GraalAcceleratorOptions.printOCLInfo) {
                    System.out.println("[INFO] No multiple devices on the same platform");
                    System.out.println("[INFO] Using first device available");
                    System.out.println(this.graalDevices.get(0));
                    System.out.println(" ---------------------------------");

                }
                GraalAcceleratorOptions.multiOpenCLDevice = false;
            }
        }
        devicesDiscovered = true;
    }

    @Override
    public boolean isMultiDeviceAvailable() {
        return devices.isMultideviceAvailable();
    }

    @Override
    public ArrayList<GraalAcceleratorDevice> getDevicesForMultiGPU(int idx) {
        return devices.getDevicesForMultiGPU(idx);
    }

    private void initializeCache() {
        if (oclCache == null) {
            oclCache = new OCLKernelPackage();
        }
    }

    @Override
    public cl_platform_id getPlatformID() {
        return this.platform;
    }

    @Override
    public void clean() {
        this.devicesDiscovered = false;
        this.oclCache = null;
        this.graalDevices.clear();
        graalDevices = null;
    }

    @Override
    public String toString() {
        return device.toString();
    }
}
