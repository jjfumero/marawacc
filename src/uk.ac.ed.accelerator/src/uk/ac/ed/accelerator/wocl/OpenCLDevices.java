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
import java.util.HashMap;
import java.util.Map;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import uk.ac.ed.accelerator.common.GraalAcceleratorDevice;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.OCLVendor;

/**
 * It manages a HashMap with a list of devices classified by type: GPU, CPU or ALL.
 *
 * <code>
 * --------------------
 * KEY : VALUE
 * -------------------
 * CPU: cpu1, cpu2, ...
 * GPU: GPU1, GPU2, ...
 * </code>
 *
 */
public final class OpenCLDevices {

    private static OpenCLDevices instance = null;
    private int numPlatforms;
    private int numDevices;

    /**
     * Indexing the {@link OCLGraalAcceleratorDevice}s.
     */
    private static int totalDeviceCounter;

    private HashMap<Long, ArrayList<GraalAcceleratorDevice>> graalOCLDev;
    private ArrayList<ArrayList<GraalAcceleratorDevice>> multiDeviceList;

    public static OpenCLDevices getInstance() {
        if (instance == null) {
            instance = new OpenCLDevices();
        }
        return instance;
    }

    /**
     * Note: Option for multiple-device in OpenCL. The cheapest option is to have a common context (it
     * assumes all the devices are under the same platform). To do so, we need to following:
     *
     * <code>
     * 1. One context per device
     * 2. One command queue per device
     * 3. The device list > 1 for the same platform
     * 4. A kernel per device
     * 5. Duplicate the number of buffers of size 1/2 of the real input size.
     * </code>
     */
    private void setMultiDeviceList(int numGPUDevices, cl_device_id[] devices, cl_platform_id platform, OCLVendor platformType) {
        if (multiDeviceList == null) {
            multiDeviceList = new ArrayList<>();
        }
        ArrayList<GraalAcceleratorDevice> gpuGraalDevices;
        if (!multiDeviceList.isEmpty()) {
            gpuGraalDevices = multiDeviceList.get(0);
        } else {
            gpuGraalDevices = new ArrayList<>();
        }
        System.out.println("Setting multidevice: " + numGPUDevices);
        for (int i = 0; i < numGPUDevices; i++) {
            long type = getLong(devices[i], CL.CL_DEVICE_TYPE);
            OCLGraalAcceleratorDevice oclGraalAcceleratorDevice = new OCLGraalAcceleratorDevice(platform, devices[i], platformType, type, totalDeviceCounter++);
            gpuGraalDevices.add(oclGraalAcceleratorDevice);
        }
        if (!multiDeviceList.isEmpty()) {
            multiDeviceList.set(0, gpuGraalDevices);
        } else {
            multiDeviceList.add(gpuGraalDevices);
        }
    }

    private cl_platform_id[] getPlatforms() {
        int[] numPlatformsArray = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatformsArray);
        this.numPlatforms = numPlatformsArray[0];

        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        CL.clGetPlatformIDs(platforms.length, platforms, null);

        return platforms;
    }

    private static class MetaInfoDevice {
        private cl_platform_id platform;
        private cl_device_id[] devices;
        private int numDevices;
        private OCLVendor platformType;

        public MetaInfoDevice(cl_platform_id platform, cl_device_id[] devices, int numDevices, OCLVendor platformType) {
            super();
            this.platform = platform;
            this.devices = devices;
            this.numDevices = numDevices;
            this.platformType = platformType;
        }

        public cl_platform_id getPlatform() {
            return platform;
        }

        public cl_device_id[] getDevices() {
            return devices;
        }

        public int getNumDevices() {
            return numDevices;
        }

        public OCLVendor getPlatformType() {
            return platformType;
        }

    }

    // Initialise multiple GPU
    private void initializeMultidevice() {

        cl_platform_id[] platforms = getPlatforms();

        ArrayList<MetaInfoDevice> meta = new ArrayList<>();

        int gpuCounter = 0;

        for (int i = 0; i < platforms.length; i++) {

            cl_platform_id platform = platforms[i];
            OCLVendor platformType = getPlatformVendor(platform);

            // Get the number of devices
            int[] numberOfDevices = new int[1];
            try {
                CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_GPU, 0, null, numberOfDevices);
            } catch (CLException e) {
                System.err.println("Platform: " + i + " is NOT A GPU Platform ");
                continue;
            }
            int numGPUDevices = numberOfDevices[0];

            // Allocate devices
            cl_device_id[] devices = new cl_device_id[numGPUDevices];

            try {
                CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_GPU, numGPUDevices, devices, null);
                gpuCounter += numGPUDevices;
                meta.add(new MetaInfoDevice(platform, devices, numGPUDevices, platformType));
            } catch (CLException e) {
                System.out.println("ERROR: There is GPU ");
                continue;
            }
        }

        if (gpuCounter > 1) {
            for (MetaInfoDevice m : meta) {
                setMultiDeviceList(m.getNumDevices(), m.getDevices(), m.getPlatform(), m.getPlatformType());
            }
        }
    }

    private void initCPUOnly(cl_platform_id platform) {
        OCLVendor platformType = getPlatformVendor(platform);
        try {
            int[] numDevicesInPlatform = new int[1];
            CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_CPU, 0, null, numDevicesInPlatform);
            int numDevicesPerPlatform = numDevicesInPlatform[0];
            cl_device_id[] devices = new cl_device_id[numDevicesPerPlatform];
            CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_CPU, numDevicesPerPlatform, devices, null);

            for (int j = 0; j < numDevicesPerPlatform; j++) {
                long deviceType = getLong(devices[j], CL.CL_DEVICE_TYPE);
                insertElementInHash(devices[j], deviceType, platform, platformType, totalDeviceCounter++);
                this.numDevices++;
            }
        } catch (CLException e) {
            // No CPU Available
        }
    }

    private void combineCPUAndMultiDeviceLists(cl_platform_id platform) {
        // Include CPU ONLY
        initCPUOnly(platform);
        // Add inspected GPU

        ArrayList<GraalAcceleratorDevice> gpuList = multiDeviceList.get(0);

        for (int logicGPUIndex = 0; logicGPUIndex < gpuList.size(); logicGPUIndex++) {

            GraalAcceleratorDevice graalAcceleratorDevice = gpuList.get(logicGPUIndex);

            ArrayList<GraalAcceleratorDevice> deviceTypeList = null;
            if (graalOCLDev.containsKey(graalAcceleratorDevice.getType())) {
                deviceTypeList = graalOCLDev.get(graalAcceleratorDevice.getType());
            } else {
                deviceTypeList = new ArrayList<>();
            }

            // Since multiDeviceList can handle gpus from different platforms, we need to check for
            // repetitions
            boolean found = false;
            if (!deviceTypeList.isEmpty()) {
                for (GraalAcceleratorDevice dev : deviceTypeList) {
                    if (dev.equals(graalAcceleratorDevice)) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                // only add a new one if not found
                deviceTypeList.add(graalAcceleratorDevice);
                graalOCLDev.put(graalAcceleratorDevice.getType(), deviceTypeList);
                numDevices++;
            }
        }
    }

    private void initAllDevicesSamePlatform(cl_platform_id platform, OCLVendor platformType) {
        int[] numDevicesInPlatform = new int[1];
        CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, 0, null, numDevicesInPlatform);
        int numDevicesPerPlatform = numDevicesInPlatform[0];
        cl_device_id[] devices = new cl_device_id[numDevicesPerPlatform];
        CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, numDevicesPerPlatform, devices, null);

        for (cl_device_id dev : devices) {
            long deviceType = getLong(dev, CL.CL_DEVICE_TYPE);
            insertElementInHash(dev, deviceType, platform, platformType, totalDeviceCounter++);
            this.numDevices++;
        }
    }

    private void initializeAllPlatforms() {
        this.numDevices = 0;

        // get a list of all OpenCL Platforms
        cl_platform_id[] platforms = getPlatforms();

        for (cl_platform_id platform : platforms) {
            OCLVendor platformType = getPlatformVendor(platform);

            if (GraalAcceleratorOptions.ignoreOpenCLVendor.equals(platformType.toString().toLowerCase())) {
                System.out.println("Ignoring PLATFORM: " + platformType);
                continue;
            }

            if (multiDeviceList != null && !multiDeviceList.isEmpty()) {
                combineCPUAndMultiDeviceLists(platform);
            } else {
                initAllDevicesSamePlatform(platform, platformType);
            }
        }
    }

    private OpenCLDevices() {

        this.graalOCLDev = new HashMap<>();
        CL.setExceptionsEnabled(true);

        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            initializeMultidevice();
        }

        if (GraalAcceleratorOptions.printOCLInfo) {
            System.out.println("Init all platforms");
        }

        initializeAllPlatforms();

        if (GraalAcceleratorOptions.printOCLInfo) {
            System.out.println("====================================");
            System.out.println("     OpenCL Devices available");
            System.out.println("====================================");
            System.out.println(this);
            System.out.println("====================================");
        }
    }

    /**
     * Returns true if there is, at least, one list of devices on the same platform.
     */
    public boolean isMultideviceAvailable() {
        if (multiDeviceList == null) {
            return false;
        }
        return !multiDeviceList.isEmpty();
    }

    /**
     * Get the list of devices on the first platform. It returns at least two devices, otherwise, this
     * list will be empty.
     */
    public ArrayList<GraalAcceleratorDevice> getFirstDevicesForMultiGPU() {
        return multiDeviceList.get(0);
    }

    /**
     * Return the list idx of devices on the same platform to with an application multi-device.
     *
     * @param idx
     * @return {@link ArrayList}
     */
    public ArrayList<GraalAcceleratorDevice> getDevicesForMultiGPU(int idx) {
        return multiDeviceList.get(idx);
    }

    private void insertElementInHash(cl_device_id device, long deviceType, cl_platform_id platform, OCLVendor platformType, int id) {
        ArrayList<GraalAcceleratorDevice> deviceList = null;
        if (this.graalOCLDev.containsKey(deviceType)) {
            deviceList = this.graalOCLDev.get(deviceType);
        } else {
            deviceList = new ArrayList<>();
        }
        deviceList.add(new OCLGraalAcceleratorDevice(platform, device, platformType, deviceType, id));
        this.graalOCLDev.put(deviceType, deviceList);
    }

    private static OCLVendor getPlatformVendor(cl_platform_id platform) {
        String platformName = getPlatformName(platform);
        OCLVendor type = null;
        if (platformName.toLowerCase().startsWith(("amd"))) {
            type = OCLVendor.AMD;
        } else if (platformName.toLowerCase().startsWith(("nvidia"))) {
            type = OCLVendor.NVIDIA;
        } else if (platformName.toLowerCase().startsWith("intel")) {
            type = OCLVendor.INTEL;
        } else if (platformName.toLowerCase().startsWith("experimental opencl 2.1")) {
            type = OCLVendor.INTEL;
        }
        return type;
    }

    private static String getPlatformName(cl_platform_id platform) {
        long[] size = new long[1];
        CL.clGetPlatformInfo(platform, CL.CL_PLATFORM_NAME, 0, null, size);
        byte[] buffer = new byte[(int) size[0]];
        CL.clGetPlatformInfo(platform, CL.CL_PLATFORM_NAME, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, buffer.length - 1);
    }

    private static long getLong(cl_device_id device, int paramName) {
        return getLongs(device, paramName, 1)[0];
    }

    private static long[] getLongs(cl_device_id device, int paramName, int numValues) {
        long[] values = new long[numValues];
        CL.clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues, Pointer.to(values), null);
        return values;
    }

    public ArrayList<GraalAcceleratorDevice> getListDevices() {
        ArrayList<GraalAcceleratorDevice> listDevices = new ArrayList<>();
        for (Map.Entry<Long, ArrayList<GraalAcceleratorDevice>> e : graalOCLDev.entrySet()) {
            listDevices.addAll(e.getValue());
        }
        return listDevices;
    }

    public GraalAcceleratorDevice getDeviceByTypeAt(int index, long deviceType) {
        GraalAcceleratorDevice device = null;
        if (graalOCLDev.containsKey(deviceType)) {
            try {
                device = graalOCLDev.get(deviceType).get(index);
            } catch (IndexOutOfBoundsException e) {

            }
        }
        return device;
    }

    public GraalAcceleratorDevice getDeviceByPlatformType(long deviceType, OCLVendor vendor) {
        GraalAcceleratorDevice device = null;
        if (graalOCLDev.containsKey(deviceType)) {

            ArrayList<GraalAcceleratorDevice> devices = graalOCLDev.get(deviceType);
            for (GraalAcceleratorDevice dev : devices) {
                if (dev.getVendor() == vendor) {
                    device = dev;
                    break;
                }
            }
        }
        return device;
    }

    public int getNumberOfDevices() {
        return this.numDevices;
    }

    @Override
    public String toString() {
        String str = "";
        for (GraalAcceleratorDevice graaldev : getListDevices()) {
            str += graaldev.toString();
        }
        return str;
    }
}
