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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

/*
 * Based on JOCL Samples:
 * http://www.jocl.org/samples/samples.html
 *
 */
public class OCLDeviceInfo {

    private cl_platform_id platform;
    private cl_device_id device;

    private String deviceName;
    private String vendorName;
    private String driverVersion;
    private String deviceType;
    private int maxComputeUnits;
    private long maxWorkItemDimensions;
    private long[] maxWorkItemSizes;
    private long maxWorkGroupSize;
    private long maxClockFrequency;
    private int addressBits;

    private long maxMemAllocSize;
    private long globalMemSize;
    private int errorCorrectionSupport;
    private int localMemType;
    private long localMemSize;
    private long maxConstantBufferSize;
    private String queueProperties;

    // Non specific of JOCL
    private ByteOrder endianess;

    // Vector Types
    private HashMap<Integer, Integer> vectorTypes;

    private static final int[] oclTypePreferredVectors = new int[]{
                    CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR,
                    CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT,
                    CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT,
                    CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG,
                    CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT,
                    CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE,
    };

    public OCLDeviceInfo(cl_platform_id platform, cl_device_id device) {
        this.platform = platform;
        this.device = device;
        this.queryInfo();
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
    private static String getDeviceType(long type) {
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

    private static String getProperties(long queueProperties) {
        StringBuffer s = new StringBuffer();
        if ((queueProperties & CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE) != 0)
            s.append("CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE, ");
        if ((queueProperties & CL.CL_QUEUE_PROFILING_ENABLE) != 0)
            s.append("CL_QUEUE_PROFILING_ENABLE");
        return s.toString();
    }

    public void queryInfo() {
        deviceName = getString(device, CL.CL_DEVICE_NAME);
        vendorName = getString(device, CL.CL_DEVICE_VENDOR);
        driverVersion = getString(device, CL.CL_DRIVER_VERSION);
        long type = getLong(device, CL.CL_DEVICE_TYPE);
        deviceType = getDeviceType(type);
        maxComputeUnits = getInt(device, CL.CL_DEVICE_MAX_COMPUTE_UNITS);
        maxWorkItemDimensions = getLong(device, CL.CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
        maxWorkItemSizes = getSizes(device, CL.CL_DEVICE_MAX_WORK_ITEM_SIZES, 3);
        maxWorkGroupSize = getSize(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE);
        maxClockFrequency = getLong(device, CL.CL_DEVICE_MAX_CLOCK_FREQUENCY);
        addressBits = getInt(device, CL.CL_DEVICE_ADDRESS_BITS);
        maxMemAllocSize = getLong(device, CL.CL_DEVICE_MAX_MEM_ALLOC_SIZE);
        globalMemSize = getLong(device, CL.CL_DEVICE_GLOBAL_MEM_SIZE);
        errorCorrectionSupport = getInt(device, CL.CL_DEVICE_ERROR_CORRECTION_SUPPORT);
        localMemType = getInt(device, CL.CL_DEVICE_LOCAL_MEM_TYPE);
        localMemSize = getLong(device, CL.CL_DEVICE_LOCAL_MEM_SIZE);
        maxConstantBufferSize = getLong(device, CL.CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
        long queueType = getLong(device, CL.CL_DEVICE_QUEUE_PROPERTIES);
        queueProperties = getProperties(queueType);

        long endianness = getLong(device, CL.CL_DEVICE_ENDIAN_LITTLE);
        if (endianness == 0) {
            endianess = ByteOrder.BIG_ENDIAN;
        } else {
            endianess = ByteOrder.LITTLE_ENDIAN;
        }

        this.vectorTypes = new HashMap<>();
        for (int i = 0; i < oclTypePreferredVectors.length; i++) {
            int size = getInt(device, oclTypePreferredVectors[i]);
            vectorTypes.put(oclTypePreferredVectors[i], size);
        }
    }

    /**
     *
     * @return ByteOrder
     */
    public ByteOrder getEndianess() {
        return endianess;
    }

    /**
     * @return the platform
     */
    public cl_platform_id getPlatform() {
        return platform;
    }

    /**
     * @return the device
     */
    public cl_device_id getDevice() {
        return device;
    }

    /**
     * @return the deviceName
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * @return the vendorName
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * @return the driverVersion
     */
    public String getDriverVersion() {
        return driverVersion;
    }

    /**
     * @return the deviceType
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * @return the maxComputeUnits
     */
    public int getMaxComputeUnits() {
        return maxComputeUnits;
    }

    /**
     * @return the maxWorkItemDimensions
     */
    public long getMaxWorkItemDimensions() {
        return maxWorkItemDimensions;
    }

    /**
     * @return the maxWorkItemSizes
     */
    public long[] getMaxWorkItemSizes() {
        return maxWorkItemSizes;
    }

    /**
     * @return the maxWorkGroupSize
     */
    public long getMaxWorkGroupSize() {
        return maxWorkGroupSize;
    }

    /**
     * @return the maxClockFrequency
     */
    public long getMaxClockFrequency() {
        return maxClockFrequency;
    }

    /**
     * @return the addressBits
     */
    public int getAddressBits() {
        return addressBits;
    }

    /**
     * @return the maxMemAllocSize
     */
    public long getMaxMemAllocSize() {
        return maxMemAllocSize;
    }

    /**
     * @return the globalMemSize
     */
    public long getGlobalMemSize() {
        return globalMemSize;
    }

    /**
     * @return the errorCorrectionSupport
     */
    public int getErrorCorrectionSupport() {
        return errorCorrectionSupport;
    }

    /**
     * @return the localMemType
     */
    public int getLocalMemType() {
        return localMemType;
    }

    /**
     * @return the localMemSize
     */
    public long getLocalMemSize() {
        return localMemSize;
    }

    /**
     * @return the maxConstantBufferSize
     */
    public long getMaxConstantBufferSize() {
        return maxConstantBufferSize;
    }

    /**
     * @return the queueProperties
     */
    public String getQueueProperties() {
        return queueProperties;
    }

    private static int getInt(cl_device_id device, int paramName) {
        return getInts(device, paramName, 1)[0];
    }

    private static int[] getInts(cl_device_id device, int paramName, int numValues) {
        int values[] = new int[numValues];
        CL.clGetDeviceInfo(device, paramName, Sizeof.cl_int * numValues, Pointer.to(values), null);
        return values;
    }

    private static long getLong(cl_device_id device, int paramName) {
        return getLongs(device, paramName, 1)[0];
    }

    private static long[] getLongs(cl_device_id device, int paramName, int numValues) {
        long values[] = new long[numValues];
        CL.clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues, Pointer.to(values), null);
        return values;
    }

    private static String getString(cl_device_id device, int paramName) {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        CL.clGetDeviceInfo(device, paramName, 0, null, size);
        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int) size[0]];
        CL.clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);
        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

    private static long getSize(cl_device_id device, int paramName) {
        return getSizes(device, paramName, 1)[0];
    }

    private static long[] getSizes(cl_device_id device, int paramName, int numValues) {
        // The size of the returned data has to depend on
        // the size of a size_t, which is handled here
        ByteBuffer buffer = ByteBuffer.allocate(numValues * Sizeof.size_t).order(ByteOrder.nativeOrder());
        CL.clGetDeviceInfo(device, paramName, Sizeof.size_t * numValues, Pointer.to(buffer), null);
        long values[] = new long[numValues];
        if (Sizeof.size_t == 4) {
            for (int i = 0; i < numValues; i++) {
                values[i] = buffer.getInt(i * Sizeof.size_t);
            }
        } else {
            for (int i = 0; i < numValues; i++) {
                values[i] = buffer.getLong(i * Sizeof.size_t);
            }
        }
        return values;
    }

    public HashMap<Integer, Integer> getDeviceVectorTypes() {
        return this.vectorTypes;
    }
}
