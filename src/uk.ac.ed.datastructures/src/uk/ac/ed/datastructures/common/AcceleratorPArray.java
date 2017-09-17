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

import java.nio.Buffer;
import java.util.ArrayList;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_event;
import org.jocl.cl_mem;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorPlatform;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.accelerator.utils.OpenCLUtils;
import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorDevice;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * {@link PArray} for OpenNCL device. It contains the logic to copy in and out the OpenCL buffers to
 * the target device.
 *
 * It also contains a few optimisations such as sequences, which I do not copy the full array, but
 * just the sequence information, and primitive array. This is not actually an optimisation for Java
 * but for Truffle languages.
 *
 * @param <T>
 */
public class AcceleratorPArray<T> extends PArray<T> {

    private int size;

    // One PArray, multiple parrays in OpenCL (just buffers)
    private PArray<T> array;

    ListOfDeviceBuffers listBuffers = new ListOfDeviceBuffers();
    AcceleratorBuffer acceleratorBuffer = new AcceleratorBuffer(0);
    private ArrayList<DataSize> listDataSize = new ArrayList<>();

    private boolean openCLBufferAllocated = false;
    private boolean isPrimitiveArray = false;
    private boolean split = false;
    private int numDevices = 1;

    @TruffleBoundary
    public AcceleratorPArray(int size, RuntimeObjectTypeInfo type) {
        this(size, type, StorageMode.DEFAULT, true);
    }

    @TruffleBoundary
    public AcceleratorPArray(int size, RuntimeObjectTypeInfo type, StorageMode mode, boolean split) {
        // Warning, there is an extra allocation => more memory footprints, but there is no
        // allocation with buffers.
        super(size, type, mode, false);

        this.size = size;
        this.array = null;
        this.type = type;
        this.mode = mode;
        this.split = split;
        numDevices = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
    }

    @Override
    @TruffleBoundary
    public void setPrimitive(boolean primitiveArray) {
        this.isPrimitiveArray = primitiveArray;
    }

    @Override
    public boolean isPrimitiveArray() {
        return isPrimitiveArray;
    }

    @TruffleBoundary
    public void setArray(PArray<T> array) {
        this.array = array;
        this.mode = array.mode;
    }

    @TruffleBoundary
    public PArray<T> getArray() {
        return array;
    }

    @TruffleBoundary
    public ArrayList<cl_mem> getOpenCLBuffersWithMetadata(int idx) {
        return listBuffers.getAcceleratorBuffer(idx).getOpenCLBuffersWithMetadata();
    }

    @TruffleBoundary
    public int computeSizes() {
        computeTotalSize(type, 0, 0);
        int bytesCounter = 0;
        for (int i = 0; i < listDataSize.size(); i++) {
            bytesCounter += listDataSize.get(i).getBytes();
        }
        return bytesCounter;
    }

    @TruffleBoundary
    public void allocateOpenCLBuffer(long flags) {
        if (!openCLBufferAllocated) {

            allocateOpenCLBuffer(flags, type, 0, 0);
            listBuffers.addAcceleratorBuffer(acceleratorBuffer);

            if (GraalAcceleratorOptions.multiOpenCLDevice) {
                // Add buffers for the second device and so on
                for (int i = 1; i < numDevices; i++) {
                    acceleratorBuffer = null;
                    acceleratorBuffer = new AcceleratorBuffer(i);
                    allocateOpenCLBuffer(flags, type, i, 0);
                    listBuffers.addAcceleratorBuffer(acceleratorBuffer);
                }
            }

            if (flags != CL.CL_MEM_WRITE_ONLY) {
                allocateUtilityBuffer();
            }
            acceleratorBuffer = null;
            openCLBufferAllocated = true;
        }
    }

    @TruffleBoundary
    private static void checkStatus(int[] status, String message) {
        checkStatus(status[0], message);
    }

    @TruffleBoundary
    private static void checkStatus(int status, String message) {
        if (status != CL.CL_SUCCESS) {
            System.out.println("[OPENCL ERROR] : " + message);
            // Abort application
            System.exit(0);
        }
    }

    private void createBuffer(cl_context context, long flags, int bufferSize, Class<?> klass) {
        int[] status = new int[1];
        cl_mem memoryObject = CL.clCreateBuffer(context, flags, bufferSize, null, status);
        checkStatus(status, "openclCreateBuffer");
        acceleratorBuffer.addCLMem(memoryObject);
        acceleratorBuffer.addType(klass);  // Types for primitives
    }

    private static int getSizeForPartition(int totalSize, int totalPartitions, int partition) {
        if (partition == (totalPartitions - 1)) {
            return ((totalSize / totalPartitions) + (totalSize % totalPartitions));
        } else {
            return totalSize / totalPartitions;
        }
    }

    @TruffleBoundary
    private void computeTotalSize(RuntimeObjectTypeInfo t, int deviceIndex, int tupleIndex) {
        if (t.isScalarType()) {
            int sizeBuffer = 0;
            try {
                sizeBuffer = array.isSequence(tupleIndex) ? this.size : array.size(tupleIndex);
            } catch (NullPointerException e) {
                sizeBuffer = this.size;
            }

            DataSize dataSize = new DataSize(sizeBuffer, t);
            listDataSize.add(dataSize);

        } else if (t.isTupleType()) {
            int nextTuple = tupleIndex;
            for (RuntimeObjectTypeInfo nestedType : t.getNestedTypes()) {
                computeTotalSize(nestedType, deviceIndex, nextTuple);
                nextTuple++;
            }
        }
    }

    @TruffleBoundary
    private void allocateOpenCLBuffer(long flags, RuntimeObjectTypeInfo t, int deviceIndex, int tupleIndex) {
        if (t.isScalarType()) {
            int sizeBuffer = 0;
            try {
                sizeBuffer = array.isSequence(tupleIndex) ? this.size : array.size(tupleIndex);
            } catch (NullPointerException e) {
                sizeBuffer = this.size;
            }
            int totalSize = t.getOCLSize() * sizeBuffer;

            if (split && GraalAcceleratorOptions.multiOpenCLDevice) {
                // Get the first size in the list
                totalSize = getSizeForPartition(totalSize, numDevices, deviceIndex);
            }

            cl_context context = getContext(deviceIndex);
            createBuffer(context, flags, totalSize, t.getClassObject());

        } else if (t.isTupleType()) {
            // Create as many buffers as primitive in the Tuple
            int nextTuple = tupleIndex;
            for (RuntimeObjectTypeInfo nestedType : t.getNestedTypes()) {
                allocateOpenCLBuffer(flags, nestedType, deviceIndex, nextTuple);
                nextTuple++;
            }
        }
    }

    /**
     * Allocate the buffers for the metadata
     */
    @TruffleBoundary
    private void allocateUtilityBuffer() {

        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            for (int i = 0; i < numDevices; i++) {
                cl_context context = getContext(i);
                int[] status = new int[1];
                cl_mem clCreateBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, JavaDataTypeSizes.INT.getOCLSize(), null, status);
                listBuffers.getAcceleratorBuffer(i).addMetadata(clCreateBuffer);
                checkStatus(status, "openclCreateBuffer for metadata");
            }

        } else {
            cl_context context = getContext();
            int[] status = new int[1];
            cl_mem clCreateBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, JavaDataTypeSizes.INT.getOCLSize(), null, status);
            listBuffers.getAcceleratorBuffer().addMetadata(clCreateBuffer);
            checkStatus(status, "openclCreateBuffer for metadata");
        }
    }

    @TruffleBoundary
    public void copyToDevice(int idx) {
        copyToDevice(idx, 0);
    }

    @TruffleBoundary
    public void copyToDevice(int idx, int offset) {
        copyDataToDevice(idx, offset);
        copyMetaDataToDevice(idx, offset);
    }

    private Pointer getPointer(Class<?> c, int idx) {
        // Types supported for the R Front-end
        if (c == Integer.class) {
            return Pointer.to(array.asIntegerArray(idx));
        } else if (c == Double.class) {
            return Pointer.to(array.asDoubleArray(idx));
        }
        throw new RuntimeException("Data type not supported yet: " + c);
    }

    @TruffleBoundary
    private void copyDataToDevice(int idx, int offset) {
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            copyDataToDeviceMulti(idx, offset);
        } else {
            copyDataToDeviceMono(idx, offset);
        }
    }

    private void copyDataToDeviceMulti(int readIndex, @SuppressWarnings("unused") int offset) {
        for (int devIndex = 0; devIndex < numDevices; devIndex++) {

            cl_command_queue queue = getOpenCLDevice(devIndex).getCommandQueue();

            RuntimeObjectTypeInfo[] dataTypesArrays = typeAsArray();

            ArrayList<cl_mem> clBuffers = listBuffers.getAcceleratorBuffer(devIndex).getBuffer();
            ArrayList<Class<?>> listTypes = listBuffers.getAcceleratorBuffer(devIndex).getListOfTypes();

            int status = 0;
            for (int j = 0; j < clBuffers.size(); j++) {
                cl_mem clBuffer = clBuffers.get(j);
                Pointer basePtr = null;
                if (array.isPrimitiveArray(j)) {
                    basePtr = getPointer(listTypes.get(j), j);
                    this.isPrimitiveArray = true;
                } else {
                    basePtr = Pointer.to(getArrayReference(j));
                }
                Pointer pointer = basePtr.withByteOffset(dataTypesArrays[j].getOCLSize() * offset());
                cl_event event = new cl_event();

                int sizeBuffer = array.isSequence(j) ? this.size : array.size(j);
                int totalSize = dataTypesArrays[j].getOCLSize() * sizeBuffer;

                int ref = totalSize / numDevices;

                if (devIndex > 0) {
                    totalSize = (totalSize / numDevices) + (totalSize % numDevices);
                } else {
                    totalSize /= numDevices;
                }

                if (devIndex != 0) {
                    pointer = basePtr.withByteOffset(ref);
                }

                if (GraalAcceleratorOptions.debugMultiDevice) {
                    System.out.println("\t copy from : " + pointer + " to ptr +" + totalSize);
                }

                status = CL.clEnqueueWriteBuffer(queue, clBuffer, CL.CL_FALSE, 0, totalSize, pointer, 0, null, event);

                checkStatus(status, "clEnqueueWriteBuffer");
                status = CL.clFlush(queue);
                checkStatus(status, "clFlush");

                if (GraalAcceleratorOptions.profileOffload) {
                    CL.clSetEventCallback(event, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_WRITE_BUFFER, readIndex, devIndex), null);
                }
            }

        }
    }

    private void copyDataToDeviceMono(int readIndex, int offset) {
        cl_command_queue queue = getOpenCLDevice().getCommandQueue();

        RuntimeObjectTypeInfo[] dataTypesArrays = typeAsArray();

        ArrayList<cl_mem> clBuffers = listBuffers.getAcceleratorBuffer().getBuffer();
        ArrayList<Class<?>> listTypes = listBuffers.getAcceleratorBuffer().getListOfTypes();

        for (int i = 0; i < clBuffers.size(); ++i) {

            cl_mem clBuffer = clBuffers.get(i);

            Pointer basePtr = null;
            if (array.isPrimitiveArray(i)) {
                basePtr = getPointer(listTypes.get(i), i);
                this.isPrimitiveArray = true;
            } else {
                basePtr = Pointer.to(getArrayReference(i));
            }
            Pointer ptr = basePtr.withByteOffset(dataTypesArrays[i].getOCLSize() * offset);
            cl_event event = new cl_event();
            int sizeBuffer = array.isSequence(i) ? this.size : array.size(i);

            int totalSize = dataTypesArrays[i].getOCLSize() * sizeBuffer;

            int status = CL.clEnqueueWriteBuffer(queue, clBuffer, CL.CL_FALSE, 0, totalSize, ptr, 0, null, event);
            checkStatus(status, "clEnqueueWriteBuffer");
            status = CL.clFlush(queue);
            checkStatus(status, "clFlush");

            if (GraalAcceleratorOptions.profileOffload) {
                CL.clSetEventCallback(event, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_WRITE_BUFFER, readIndex, 0), null);
            }
        }
    }

    @TruffleBoundary
    public void copyMetaDataToDevice(int idx) {
        copyMetaDataToDevice(idx, 0);
    }

    @TruffleBoundary
    public void copyMetaDataToDevice(int idx, int offset) {
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            copyMetaDataToDeviceMultidevice(idx, offset);
        } else {
            copyMetaDataToDeviceMono(idx, offset);
        }
    }

    @TruffleBoundary
    public void copyMetaDataToDeviceMultidevice(int idx, @SuppressWarnings("unused") int offset) {

        for (int deviceIndex = 0; deviceIndex < numDevices; deviceIndex++) {

            cl_command_queue queue = getOpenCLDevice(deviceIndex).getCommandQueue();

            ArrayList<cl_mem> metadataBuffers = listBuffers.getAcceleratorBuffer(deviceIndex).getMetadata();

            for (int i = 0; i < metadataBuffers.size(); ++i) {
                cl_mem clBuffer = metadataBuffers.get(i);

                int[] auxiliarArray = new int[]{size};
                if (this.isSequence()) {
                    auxiliarArray[0] = getTotalSizeWhenSequence();
                }

                // Recompute the size
                if (deviceIndex > 0) {
                    auxiliarArray[0] = (auxiliarArray[0] / numDevices) + (auxiliarArray[0] % numDevices);
                } else {
                    auxiliarArray[0] /= numDevices;
                }

                if (GraalAcceleratorOptions.debugMultiDevice) {
                    System.out.println("\tCopy metadata: " + auxiliarArray[0]);
                }

                Pointer ptr = Pointer.to(auxiliarArray);
                cl_event event = new cl_event();
                int status = CL.clEnqueueWriteBuffer(queue, clBuffer, CL.CL_FALSE, 0, JavaDataTypeSizes.INT.getOCLSize(), ptr, 0, null, event);
                checkStatus(status, "clEnqueueWriteBuffer for metadata");
                CL.clFlush(queue);
                checkStatus(status, "clFlush");

                if (GraalAcceleratorOptions.profileOffload) {
                    CL.clSetEventCallback(event, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_WRITE_BUFFER_METADATA, idx, deviceIndex), null);
                }
            }
        }
    }

    @TruffleBoundary
    public void copyMetaDataToDeviceMono(int idx, @SuppressWarnings("unused") int offset) {
        cl_command_queue queue = getOpenCLDevice().getCommandQueue();

        ArrayList<cl_mem> metadataBuffers = listBuffers.getAcceleratorBuffer().getMetadata();

        for (int i = 0; i < metadataBuffers.size(); ++i) {
            cl_mem clBuffer = metadataBuffers.get(i);

            int[] auxiliarArray = new int[]{size};
            if (this.isSequence()) {
                auxiliarArray[0] = getTotalSizeWhenSequence();
            }
            Pointer ptr = Pointer.to(auxiliarArray);
            cl_event event = new cl_event();
            int status = CL.clEnqueueWriteBuffer(queue, clBuffer, CL.CL_FALSE, 0, JavaDataTypeSizes.INT.getOCLSize(), ptr, 0, null, event);
            checkStatus(status, "clEnqueueWriteBuffer for metadata");
            CL.clFlush(queue);
            checkStatus(status, "clFlush for metadata");

            if (GraalAcceleratorOptions.profileOffload) {
                CL.clSetEventCallback(event, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_WRITE_BUFFER_METADATA, idx, 0), null);
            }
        }
    }

    @TruffleBoundary
    public void copyToHost(int idx) {
        copyToHost(idx, 0);
    }

    @TruffleBoundary
    public void copyToHost(int idx, int offset) {
        ArrayList<ArrayList<cl_event>> readEvents = null;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            readEvents = copyToHostMultidevice(idx, offset);
        } else {
            readEvents = copyToHostMonodevice(idx, offset);
        }
        OpenCLUtils.waitForEvents(readEvents);
    }

    public static class CopyThread extends Thread {

        private cl_command_queue queue;
        private Pointer pointer;
        private cl_event event;
        private int status;
        private int totalSize;
        private cl_mem oclBuffer;
        private int writeIndex;
        private int deviceIndex;

        public CopyThread(cl_command_queue queue, Pointer pointer, cl_event event, int totalSize, cl_mem oclBuffer, int writeIndex, int deviceIndex) {
            super();
            this.queue = queue;
            this.pointer = pointer;
            this.event = event;
            this.totalSize = totalSize;
            this.oclBuffer = oclBuffer;
            this.writeIndex = writeIndex;
            this.deviceIndex = deviceIndex;
        }

        @Override
        public void run() {
            // This has to be CL.CL_TRUE because:
            // [JOCL] "Non-blocking read operations may only be performed using pointers to direct
            // buffers"
            status = CL.clEnqueueReadBuffer(queue, oclBuffer, CL.CL_TRUE, 0, totalSize, pointer, 0, null, event);
            if (GraalAcceleratorOptions.profileOffload) {
                CL.clSetEventCallback(event, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_READ_BUFFER, writeIndex, deviceIndex), null);
            }
        }

        public int getStatus() {
            return status;
        }

        public cl_event getEvent() {
            return event;
        }
    }

    @TruffleBoundary
    public ArrayList<ArrayList<cl_event>> copyToHostMultidevice(int writeIndex, @SuppressWarnings("unused") int offset) {

        RuntimeObjectTypeInfo[] types = typeAsArray();

        ArrayList<ArrayList<cl_event>> eventList = new ArrayList<>();

        CopyThread[] copyThreads = new CopyThread[numDevices];

        for (int deviceIndex = 0; deviceIndex < numDevices; deviceIndex++) {

            cl_command_queue queue = getOpenCLDevice(deviceIndex).getCommandQueue();

            ArrayList<cl_mem> clBuffers = listBuffers.getAcceleratorBuffer(deviceIndex).getBuffer();
            ArrayList<Class<?>> listTypes = listBuffers.getAcceleratorBuffer(deviceIndex).getListOfTypes();

            ArrayList<cl_event> events = new ArrayList<>();

            for (int i = 0; i < clBuffers.size(); ++i) {
                cl_mem oclBuffer = clBuffers.get(i);

                Pointer basePointer = null;
                if (this.isPrimitiveArray(i)) {
                    basePointer = getPointer(listTypes.get(i), i);
                } else {
                    basePointer = Pointer.to(getArrayReference(i));
                }

                Pointer pointer = basePointer.withByteOffset(types[i].getOCLSize() * offset());

                cl_event event = new cl_event();
                int status = 0;

                int totalSize = types[i].getOCLSize() * this.size;
                int reference = totalSize / numDevices;

                if (split) {
                    if (deviceIndex > 0) {
                        pointer = basePointer.withByteOffset(reference);
                        totalSize = (totalSize / numDevices) + (totalSize % numDevices);
                    } else {
                        totalSize /= numDevices;
                    }
                }

                if (GraalAcceleratorOptions.debugMultiDevice) {
                    System.out.println("\t copy back from : " + pointer + " to ptr +" + totalSize);
                }

                if (this.isPrimitiveArray(i)) {
                    copyThreads[deviceIndex] = new CopyThread(queue, pointer, event, totalSize, oclBuffer, writeIndex, deviceIndex);
                    copyThreads[deviceIndex].start();
                } else {
                    status = CL.clEnqueueReadBuffer(queue, oclBuffer, CL.CL_FALSE, 0, totalSize, pointer, 0, null, event);
                    checkStatus(status, "clEnqueueReadBuffer");
                    events.add(event);
                    if (GraalAcceleratorOptions.profileOffload) {
                        CL.clSetEventCallback(event, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_READ_BUFFER, writeIndex, deviceIndex), null);
                    }
                }
            }
            eventList.add(events);
        }

        try {
            for (Thread t : copyThreads) {
                if (t != null) {
                    t.join();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return eventList;
    }

    @TruffleBoundary
    public ArrayList<ArrayList<cl_event>> copyToHostMonodevice(int writeIndex, int offset) {
        cl_command_queue queue = getOpenCLDevice().getCommandQueue();

        RuntimeObjectTypeInfo[] types = typeAsArray();

        ArrayList<cl_mem> clBuffers = listBuffers.getAcceleratorBuffer().getBuffer();
        ArrayList<Class<?>> listTypes = listBuffers.getAcceleratorBuffer().getListOfTypes();

        for (int i = 0; i < clBuffers.size(); ++i) {

            cl_mem oclBuffer = clBuffers.get(i);

            Pointer basePointer = null;
            if (this.isPrimitiveArray(i)) {
                basePointer = getPointer(listTypes.get(i), i);
            } else {
                basePointer = Pointer.to(getArrayReference(i));
            }

            Pointer pointer = basePointer.withByteOffset(types[i].getOCLSize() * offset);
            cl_event event = new cl_event();
            int totalSize = types[i].getOCLSize() * this.size;

            int status = CL.clEnqueueReadBuffer(queue, oclBuffer, CL.CL_TRUE, 0, totalSize, pointer, 0, null, event);
            checkStatus(status, "clEnqueueReadBuffer");

            if (GraalAcceleratorOptions.profileOffload) {
                CL.clSetEventCallback(event, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_READ_BUFFER, writeIndex, 0), null);
            }
        }

        return new ArrayList<>();
    }

    private static GraalAcceleratorPlatform getGraalOCLPlatform() {
        GraalAcceleratorSystem system = GraalAcceleratorSystem.getInstance();
        GraalAcceleratorPlatform platform = system.getPlatform();
        return platform;
    }

    private static OCLGraalAcceleratorDevice getOpenCLDevice() {
        GraalAcceleratorPlatform platform = getGraalOCLPlatform();
        return (OCLGraalAcceleratorDevice) platform.getDevice();
    }

    /**
     * For multiple device.
     */
    private static OCLGraalAcceleratorDevice getOpenCLDevice(int idx) {
        GraalAcceleratorPlatform platform = getGraalOCLPlatform();
        return (OCLGraalAcceleratorDevice) platform.getDevice(idx);
    }

    @TruffleBoundary
    private RuntimeObjectTypeInfo[] typeAsArray() {
        if (type.isScalarType()) {
            return new RuntimeObjectTypeInfo[]{type};
        } else if (type.isTupleType()) {
            return type.getNestedTypes();
        } else {
            System.err.println("[Not Implemented yet] ==> " + getClassObject().getName());
            throw new NotImplementedException();
        }
    }

    @TruffleBoundary
    private static cl_context getContext() {
        return getOpenCLDevice().getContext();
    }

    private static cl_context getContext(int idx) {
        return getOpenCLDevice(idx).getContext();
    }

    @Override
    public void clear() {
        listBuffers.clean();
    }

    @Override
    public void put(int index, T e) {
        array.put(index, e);
    }

    @Override
    public T get(int index) {
        return array.get(index);
    }

    @Override
    public int offset() {
        return array.offset();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Buffer getArrayReference() {
        return array.getArrayReference();
    }

    @Override
    public Buffer getArrayReference(int idxArray) {
        return array.getArrayReference(idxArray);
    }

    @Override
    public boolean isSequence() {
        return array.isSequence();
    }

    @Override
    public boolean isSequence(int idx) {
        return array.isSequence(idx);
    }

    @Override
    public void setSequence(boolean sequence) {
        setSequence(0, sequence);
    }

    @Override
    public void setSequence(int idx, boolean sequence) {
        array.setSequence(idx, sequence);
    }

    @Override
    public int grade() {
        return array.grade();
    }

    @Override
    public Class<?> getClassObject() {
        return array.getClassObject();
    }

    @Override
    public ArraySlice<T>[] splitInChunksOfSize(int chunkSize) {
        return array.splitInChunksOfSize(chunkSize);
    }

    @Override
    public ArraySlice<T>[] splitInFixedNumberOfChunks(int numberOfChunks) {
        return array.splitInFixedNumberOfChunks(numberOfChunks);
    }
}
