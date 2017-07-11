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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.HashSet;

import uk.ac.ed.datastructures.interop.InteropTable;
import uk.ac.ed.datastructures.tuples.Tuple;
import uk.ac.ed.datastructures.tuples.Tuple1;
import uk.ac.ed.datastructures.tuples.Tuple10;
import uk.ac.ed.datastructures.tuples.Tuple11;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;
import uk.ac.ed.datastructures.tuples.Tuple4;
import uk.ac.ed.datastructures.tuples.Tuple5;
import uk.ac.ed.datastructures.tuples.Tuple6;
import uk.ac.ed.datastructures.tuples.Tuple7;
import uk.ac.ed.datastructures.tuples.Tuple8;
import uk.ac.ed.datastructures.tuples.Tuple9;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * PArray base implementation.
 *
 * <p>
 * To take control of the data management and to be able to apply optimisations, we implemented our
 * own array class which we named {@link PArray} for Portable Array (portable across devices).
 * </p>
 *
 * <p>
 * PArray< T > is a generic Java class, where T is the type of the elements stored in the array. T
 * can either be the wrapper of a primitive type, e.g., {@link Float}, our own tuple type class,
 * e.g., {@link Tuple2}< Float, Float > or a nested array type for representing multidimensional
 * data.
 * </p>
 *
 * <p>
 * Our array class follows a value semantics, i.e., instead of references to values copies of the
 * values are stored in the array. This enables us to avoid marshaling and directly pass a pointer
 * to our internal storage to the OpenCL implementation, as no Java code can holds a reference to
 * our arrays.
 * </p>
 *
 * @param <T>
 */
public class PArray<T> {

    /**
     * Storage mode. It creates pinned memory if OPENCL_BYTE_BUFFER options is passed as well as
     * DEFAULT. Otherwise it creates a {@link ByteBuffer} with no pinned memory.
     *
     */
    public enum StorageMode {
        DEFAULT("DEFAULT"),
        JAVA_NATIVE_BUFFERS("JAVA_NATIVE_BUFFERS"),
        OPENCL_BYTE_BUFFER("OPENCL_BYTE_BUFFER"),
        JAVA_BYTE_BUFFER("JAVA_BYTE_BUFFER"),
        JAVA_OBJECT("JAVA_OBJECT");             // Use marshal and un-marshal operations

        private String mode;

        StorageMode(String mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            return this.mode;
        }
    }

    private ArrayImplementation<T> arrayImplementation;

    // Size when the sequence is passed
    private int totalSize;

    @CompilationFinal protected StorageMode mode;
    protected RuntimeObjectTypeInfo type;

    public static final HashSet<Class<?>> TUPLESET = initializeSetTuples();

    public static HashSet<Class<?>> initializeSetTuples() {
        HashSet<Class<?>> set = new HashSet<>();
        set.add(Tuple1.class);
        set.add(Tuple2.class);
        set.add(Tuple3.class);
        set.add(Tuple4.class);
        set.add(Tuple5.class);
        set.add(Tuple6.class);
        set.add(Tuple7.class);
        set.add(Tuple8.class);
        set.add(Tuple9.class);
        set.add(Tuple10.class);
        set.add(Tuple11.class);
        return set;
    }

    protected PArray() {
    }

    @TruffleBoundary
    public PArray(int size, RuntimeObjectTypeInfo t) {
        this(size, t, StorageMode.DEFAULT, true);
    }

    @TruffleBoundary
    public PArray(int size, RuntimeObjectTypeInfo t, StorageMode mode) {
        this(size, t, mode, true);
    }

    @TruffleBoundary
    public PArray(int size, RuntimeObjectTypeInfo t, boolean init) {
        this(size, t, StorageMode.OPENCL_BYTE_BUFFER, init);
    }

    @TruffleBoundary
    public PArray(int size, RuntimeObjectTypeInfo t, InteropTable op) {
        this(size, t, StorageMode.DEFAULT, op);
    }

    @TruffleBoundary
    private static String printLineInfo() {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        String where = ste.getClassName() + "#" + ste.getMethodName() + ":" + ste.getLineNumber() + " ";
        return where;
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public PArray(int size, RuntimeObjectTypeInfo type, StorageMode mode, boolean init) {

        assert (type != null);

        this.mode = mode;
        this.type = type;
        switch (mode) {
            case JAVA_NATIVE_BUFFERS:
                if (type.getClassObject() == Float.class) {
                    arrayImplementation = (ArrayImplementation<T>) new FloatBufferArray(size);
                } else if (type.getClassObject() == Double.class) {
                    arrayImplementation = (ArrayImplementation<T>) new DoubleBufferArray(size);
                } else if (type.getClassObject() == Integer.class) {
                    arrayImplementation = (ArrayImplementation<T>) new IntegerBufferArray(size);
                } else if (type.getClassObject() == Long.class) {
                    arrayImplementation = (ArrayImplementation<T>) new LongBufferArray(size);
                } else if (type.getClassObject() == Byte.class) {
                    arrayImplementation = (ArrayImplementation<T>) new ByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Character.class) {
                    arrayImplementation = (ArrayImplementation<T>) new CharBufferArray(size);
                } else if (type.getClassObject() == Boolean.class) {
                    arrayImplementation = (ArrayImplementation<T>) new BooleanBufferArray(size, mode, init);
                } else if (type.getClassObject() == Short.class) {
                    arrayImplementation = (ArrayImplementation<T>) new ShortBufferArray(size);
                } else if (type.getClassObject() == Tuple2.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple2Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple3.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple3Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple4.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple4Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple5.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple5Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple6.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple6Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple7.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple7Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple8.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple8Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple9.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple9Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple10.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple10Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple11.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple11Array<>(size, type.getNestedTypes(), mode, init);
                } else {
                    System.err.println("[Not Implemented yet!!] " + type.getClassObject().getName() + printLineInfo());
                    throw new RuntimeException("[Not Implemented yet!!] " + type.getClassObject().getName() + printLineInfo());
                }
                break;
            case DEFAULT:
            case OPENCL_BYTE_BUFFER:
            case JAVA_BYTE_BUFFER:
                if (type.getClassObject() == Float.class) {
                    arrayImplementation = (ArrayImplementation<T>) new FloatByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Double.class) {
                    arrayImplementation = (ArrayImplementation<T>) new DoubleByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Integer.class) {
                    arrayImplementation = (ArrayImplementation<T>) new IntegerByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Long.class) {
                    arrayImplementation = (ArrayImplementation<T>) new LongByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Byte.class) {
                    arrayImplementation = (ArrayImplementation<T>) new ByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Character.class) {
                    arrayImplementation = (ArrayImplementation<T>) new CharByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Short.class) {
                    arrayImplementation = (ArrayImplementation<T>) new ShortByteBufferArray(size, mode, init);
                } else if (type.getClassObject() == Tuple2.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple2Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple3.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple3Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple4.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple4Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple5.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple5Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple6.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple6Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple7.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple7Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple8.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple8Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple9.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple9Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple10.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple10Array<>(size, type.getNestedTypes(), mode, init);
                } else if (type.getClassObject() == Tuple11.class) {
                    arrayImplementation = (ArrayImplementation<T>) new Tuple11Array<>(size, type.getNestedTypes(), mode, init);
                } else {
                    System.err.println("[Not Implemented yet] " + type.getClassObject().getName() + " : " + printLineInfo());
                    throw new RuntimeException("[Not Implemented yet!!] " + type.getClassObject().getName() + " : " + printLineInfo());
                }
                break;
            case JAVA_OBJECT:
                arrayImplementation = new JavaObjectStorageArray<>(size);
        }
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public PArray(int size, RuntimeObjectTypeInfo type, StorageMode mode, InteropTable op) {

        assert (type != null);

        this.mode = mode;
        this.type = type;

        boolean init = true;

        if (mode == StorageMode.JAVA_OBJECT) {
            arrayImplementation = new JavaObjectStorageArray<>(size);
        } else if (op == InteropTable.T2) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple2Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T3) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple3Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T4) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple4Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T5) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple5Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T6) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple6Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T7) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple7Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T8) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple8Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T9) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple9Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T10) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple10Array<>(size, type.getNestedTypes(), mode, init);
        } else if (op == InteropTable.T11) {
            arrayImplementation = (ArrayImplementation<T>) new Tuple11Array<>(size, type.getNestedTypes(), mode, init);
        } else {
            System.err.println("[Not Implemented yet] " + type.getClassObject().getName());
            throw new RuntimeException("[Not Implemented yet!!] " + type.getClassObject().getName() + printLineInfo());
        }
    }

    @TruffleBoundary
    public void put(int index, T e) {
        arrayImplementation.put(index, e);
    }

    @TruffleBoundary
    public T get(int index) {
        return arrayImplementation.get(index);
    }

    @TruffleBoundary
    public void setBuffer(int idx, Buffer buffer) {
        arrayImplementation.setArrayReference(idx, buffer);
    }

    @TruffleBoundary
    public void setBuffer(int idx, Buffer buffer, boolean sequence) {
        setSequence(idx, sequence);
        arrayImplementation.setArrayReference(idx, buffer);
    }

    @TruffleBoundary
    public void setBufferFlagSequence(int idx, Buffer buffer, boolean sequence) {
        setSequence(idx, sequence);
        setFlag(idx, true);
        arrayImplementation.setArrayReference(idx, buffer);
    }

    @TruffleBoundary
    public void setBufferCompassSequence(int idx, Buffer buffer, boolean sequence) {
        setSequence(idx, sequence);
        setCompass(idx, true);
        arrayImplementation.setArrayReference(idx, buffer);
    }

    @TruffleBoundary
    private void setBuffer(Buffer buffer) {
        arrayImplementation.setArrayReference(0, buffer);
    }

    @TruffleBoundary
    public Buffer getArrayReference() {
        return arrayImplementation.getArrayReference(0);
    }

    @TruffleBoundary
    public ArrayImplementation<T> getArrayImpl() {
        return this.arrayImplementation;
    }

    @TruffleBoundary
    public Buffer getArrayReference(int idxArray) {
        return arrayImplementation.getArrayReference(idxArray);
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public double[] asDoubleArray() {
        return ((DoubleByteBufferArray) arrayImplementation).array();
    }

    public double[] asDoubleArray(int idx) {
        return arrayImplementation.doubleArray(idx);
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public int[] asIntegerArray() {
        return ((IntegerByteBufferArray) arrayImplementation).array();
    }

    @TruffleBoundary
    public int[] asIntegerArray(int idx) {
        return arrayImplementation.intArray(idx);
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public float[] asFloatArray() {
        return ((FloatByteBufferArray) arrayImplementation).array();
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public short[] asShortArray() {
        return ((ShortByteBufferArray) arrayImplementation).array();
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public long[] asLongArray() {
        return ((LongByteBufferArray) arrayImplementation).array();
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public char[] asCharArray() {
        return ((CharByteBufferArray) arrayImplementation).array();
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public byte[] asByteArray() {
        return ((ByteBufferArray) arrayImplementation).array();
    }

    @TruffleBoundary
    public boolean isSequence() {
        return isSequence(0);
    }

    @TruffleBoundary
    public boolean isSequence(int idx) {
        return arrayImplementation.isSequence(idx);
    }

    @TruffleBoundary
    public void setSequence(boolean sequence) {
        setSequence(0, sequence);
    }

    @TruffleBoundary
    public void setSequence(int idx, boolean sequence) {
        arrayImplementation.setSequence(idx, sequence);
    }

    @TruffleBoundary
    public void setCompass(int idx, boolean compass) {
        arrayImplementation.setCompass(idx, compass);
    }

    @TruffleBoundary
    public void setCompass(boolean compass) {
        setCompass(0, compass);
    }

    @TruffleBoundary
    public boolean isCompass(int idx) {
        return arrayImplementation.isCompass(idx);
    }

    @TruffleBoundary
    public boolean isCompass() {
        return isCompass(0);
    }

    @TruffleBoundary
    public void setFlag(int idx, boolean flag) {
        arrayImplementation.setFlag(idx, flag);
    }

    @TruffleBoundary
    public void setFlag(boolean flag) {
        setFlag(0, flag);
    }

    @TruffleBoundary
    public boolean isFlag(int idx) {
        return arrayImplementation.isFlag(idx);
    }

    @TruffleBoundary
    public boolean isFlag() {
        return isFlag(0);
    }

    @TruffleBoundary
    public void setIntArray(int[] inputArray) {
        setIntArray(0, inputArray);
    }

    @TruffleBoundary
    public void setIntArray(int idx, int[] inputArray) {
        arrayImplementation.setIntArray(idx, inputArray);
    }

    @TruffleBoundary
    public void setDoubleArray(double[] inputArray) {
        setDoubleArray(0, inputArray);
    }

    @TruffleBoundary
    public void setDoubleArray(int idx, double[] inputArray) {
        arrayImplementation.setDoubleArray(idx, inputArray);
    }

    @TruffleBoundary
    public boolean isPrimitiveArray() {
        return arrayImplementation.isPrimitiveArray();
    }

    @TruffleBoundary
    public boolean isPrimitiveArray(int idx) {
        return arrayImplementation.isPrimitiveArray(idx);
    }

    /**
     * If {@link #setSequence(boolean)} is enabled, then we need to allocate the output from the
     * real size. The real size is stored through this method.
     */
    @TruffleBoundary
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    @TruffleBoundary
    public int getTotalSizeWhenSequence() {
        return totalSize;
    }

    @TruffleBoundary
    public int offset() {
        return 0;
    }

    @TruffleBoundary
    public int size() {
        return arrayImplementation.size(0);
    }

    @TruffleBoundary
    public int size(int index) {
        return arrayImplementation.size(index);
    }

    @TruffleBoundary
    public int grade() {
        return arrayImplementation.grade();
    }

    @TruffleBoundary
    public Class<?> getClassObject() {
        return type.getClassObject();
    }

    @TruffleBoundary
    public Class<?> getType() {
        return getClassObject();
    }

    public RuntimeObjectTypeInfo getRuntimeObjectTypeInfo() {
        return this.type;
    }

    @TruffleBoundary
    public StorageMode getStorageMode() {
        return mode;
    }

    @TruffleBoundary
    public ArraySlice<T>[] splitInChunksOfSize(int chunkSize) {

        int size = size();

        int numberOfChunks = size / chunkSize;
        if (size % chunkSize != 0) {
            numberOfChunks++;
        }

        // compute start and end indices
        int[] startv = new int[numberOfChunks];
        int[] endv = new int[numberOfChunks];

        for (int i = 0; i < numberOfChunks; ++i) {
            if (i == 0) {
                startv[i] = 0;
            } else {
                startv[i] = endv[i - 1] + 1;
            }
            endv[i] = startv[i] + chunkSize - 1;
        }

        endv[numberOfChunks - 1] = size - 1;

        // create array slices
        @SuppressWarnings("unchecked")
        ArraySlice<T>[] slices = new ArraySlice[numberOfChunks];

        for (int i = 0; i < numberOfChunks; ++i) {
            slices[i] = new ArraySlice<>(this, startv[i], endv[i] - startv[i] + 1);
        }
        return slices;

    }

    @TruffleBoundary
    public ArraySlice<T>[] splitInFixedNumberOfChunks(int numberOfChunks) {

        int size = size();
        int sizeOfChunk = size / numberOfChunks;

        int[] startv = new int[numberOfChunks];
        int[] endv = new int[numberOfChunks];

        for (int i = 0; i < numberOfChunks; ++i) {
            if (i == 0) {
                startv[i] = 0;
            } else {
                startv[i] = endv[i - 1] + 1;
            }
            endv[i] = startv[i] + sizeOfChunk - 1;
        }

        endv[numberOfChunks - 1] = size - 1;

        @SuppressWarnings("unchecked")
        ArraySlice<T>[] slices = new ArraySlice[numberOfChunks];

        for (int i = 0; i < numberOfChunks; ++i) {
            slices[i] = new ArraySlice<>(this, startv[i], endv[i] - startv[i] + 1);
        }
        return slices;
    }

    public void clear() {
        // empty
    }

    private interface ArrayImplementation<E> {

        void put(int index, E e);

        default double[] doubleArray(@SuppressWarnings("unused") int idx) {
            return null;
        }

        default int[] intArray(@SuppressWarnings("unused") int idx) {
            return null;
        }

        default boolean isPrimitiveArray(@SuppressWarnings("unused") int idx) {
            return false;
        }

        default boolean isPrimitiveArray() {
            return false;
        }

        default int primitiveSize() {
            return 0;
        }

        boolean isSequence(int idx);

        void setSequence(int idx, boolean sequence);

        void setCompass(int idx, boolean compass);

        void setFlag(int idx, boolean flag);

        boolean isFlag(int idx);

        boolean isCompass(int idx);

        E get(int index);

        int size(int index);

        int grade();

        Buffer getArrayReference(int idxArrayRef);

        void setArrayReference(int idxArrayRef, Buffer data);

        @SuppressWarnings("unused")
        default void setDoubleArray(int idx, double[] inputArray) {
        }

        @SuppressWarnings("unused")
        default void setIntArray(int idx, int[] inputArray) {
        }
    }

    private abstract class AbstractBufferArray<E> implements ArrayImplementation<E> {
        protected boolean compass = false;
        protected boolean flag = false;
        protected boolean sequence = false;

        @Override
        public void setCompass(int idx, boolean compass) {
            this.compass = compass;
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            this.flag = flag;
        }

        @Override
        public boolean isFlag(int idx) {
            return this.flag;
        }

        @Override
        public boolean isCompass(int idx) {
            return this.compass;
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            this.sequence = sequence;
        }

        @Override
        public boolean isSequence(int idx) {
            return sequence;
        }

        public boolean isPrimitiveArray(int idx) {
            // TODO Auto-generated method stub
            return false;
        }

    }

    private class FloatBufferArray extends AbstractBufferArray<Float> {

        public FloatBuffer buffer;
        private final int GRADE = 1;

        public FloatBufferArray(int size) {
            buffer = FloatBuffer.allocate(size);
        }

        @Override
        public void put(int index, Float f) {
            buffer.put(index, f);
        }

        @Override
        public Float get(int index) {
            return buffer.get(index);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public int size(int index) {
            return buffer.capacity();
        }

        @Override
        public Buffer getArrayReference(int idxArrayRef) {
            return buffer;
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            this.buffer = (FloatBuffer) data;
        }

    }

    private class DoubleBufferArray extends AbstractBufferArray<Double> {

        public DoubleBuffer buffer;
        private final int GRADE = 1;

        public DoubleBufferArray(int size) {
            buffer = DoubleBuffer.allocate(size);
        }

        @Override
        public void put(int index, Double d) {
            buffer.put(index, d);
        }

        @Override
        public Double get(int index) {
            return buffer.get(index);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public int size(int index) {
            return buffer.capacity();
        }

        @Override
        public Buffer getArrayReference(int idxArrayRef) {
            return buffer;
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            this.buffer = (DoubleBuffer) data;
        }

    }

    private class IntegerBufferArray extends AbstractBufferArray<Integer> {

        public IntBuffer buffer;
        private final int GRADE = 1;

        public IntegerBufferArray(int size) {
            buffer = IntBuffer.allocate(size);
        }

        @Override
        public void put(int index, Integer i) {
            buffer.put(index, i);
        }

        @Override
        public Integer get(int index) {
            return buffer.get(index);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public int size(int index) {
            return buffer.capacity();
        }

        @Override
        public Buffer getArrayReference(int idxArrayRef) {
            return buffer;
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            this.buffer = (IntBuffer) data;
        }

    }

    private class LongBufferArray extends AbstractBufferArray<Long> {

        public LongBuffer buffer;
        private final int GRADE = 1;

        public LongBufferArray(int size) {
            buffer = LongBuffer.allocate(size);
        }

        @Override
        public void put(int index, Long i) {
            buffer.put(index, i);
        }

        @Override
        public Long get(int index) {
            return buffer.get(index);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public int size(int index) {
            return buffer.capacity();
        }

        @Override
        public Buffer getArrayReference(int idxArrayRef) {
            return buffer;
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            this.buffer = (LongBuffer) data;
        }
    }

    private class ShortBufferArray extends AbstractBufferArray<Short> {

        public ShortBuffer buffer;

        public ShortBufferArray(int size) {
            buffer = ShortBuffer.allocate(size);
        }

        @Override
        public void put(int index, Short s) {
            buffer.put(index, s);
        }

        @Override
        public Short get(int index) {
            return buffer.get(index);
        }

        @Override
        public int grade() {
            return 1;
        }

        @Override
        public int size(int index) {
            return buffer.capacity();
        }

        @Override
        public Buffer getArrayReference(int idxArrayRef) {
            return buffer;
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            this.buffer = (ShortBuffer) data;
        }

    }

    private class CharBufferArray extends AbstractBufferArray<Character> {

        public CharBuffer buffer;

        public CharBufferArray(int size) {
            buffer = CharBuffer.allocate(size);
        }

        @Override
        public void put(int index, Character s) {
            buffer.put(index, s);
        }

        @Override
        public Character get(int index) {
            return buffer.get(index);
        }

        @Override
        public int grade() {
            return 1;
        }

        @Override
        public int size(int index) {
            return buffer.capacity();
        }

        @Override
        public Buffer getArrayReference(int idxArrayRef) {
            return buffer;
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            this.buffer = (CharBuffer) data;
        }

    }

    // Base class for ByteBuffer arrays with Pinned Memory
    private abstract class PrimitiveArray<PE> implements ArrayImplementation<PE> {

        public ByteBuffer buffer;
        public int sizeofElement;
        public boolean sequence;
        private boolean compass = false;
        private boolean flag = false;

        public PrimitiveArray(int size, JavaDataTypeSizes javaType, StorageMode mode, boolean init) {
            this.sizeofElement = javaType.getSize();
            if (init) {
                if (mode == StorageMode.OPENCL_BYTE_BUFFER || mode == StorageMode.DEFAULT) {
                    this.buffer = HeterogenousBufferAllocator.allocateBuffer(size, sizeofElement, javaType);
                } else if (mode == StorageMode.JAVA_BYTE_BUFFER) {
                    this.buffer = ByteBuffer.allocateDirect(size * sizeofElement);
                }
            }
        }

        @Override
        public int size(int index) {
            if (isPrimitiveArray()) {
                return this.primitiveSize();
            } else {
                return buffer.capacity() / sizeofElement;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArrayRef) {
            return buffer;
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            this.buffer = (ByteBuffer) data;
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            this.sequence = sequence;
        }

        @Override
        public boolean isSequence(int idx) {
            return sequence;
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            this.compass = compass;
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            this.flag = flag;
        }

        @Override
        public boolean isFlag(int idx) {
            return flag;
        }

        @Override
        public boolean isCompass(int idx) {
            return compass;
        }

    }

    private class FloatByteBufferArray extends PrimitiveArray<Float> {

        private final int GRADE = 1;

        public FloatByteBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.FLOAT, mode, init);
        }

        /**
         * @see <a href=
         *      "http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double">
         *      http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double </a>
         *
         * @return float[]
         */
        public float[] array() {
            FloatBuffer b = buffer.asFloatBuffer();
            float output[] = new float[b.remaining()];
            b.get(output);
            return output;
        }

        @Override
        public void put(int index, Float data) {
            buffer.putFloat(index * sizeofElement, data);
        }

        @Override
        public Float get(int index) {
            return buffer.getFloat(index * sizeofElement);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        public boolean isPrimitiveArray(int idx) {
            // TODO Auto-generated method stub
            return false;
        }

    }

    private class DoubleByteBufferArray extends PrimitiveArray<Double> {

        private final int GRADE = 1;
        private double[] array;

        @TruffleBoundary
        public DoubleByteBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.DOUBLE, mode, init);
        }

        @Override
        @TruffleBoundary
        public void put(int index, Double d) {
            if (array != null) {
                array[index] = d;
            } else {
                buffer.putDouble(index * sizeofElement, d);
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            this.array = inputArray;
        }

        @Override
        public double[] doubleArray(int idx) {
            return array();
        }

        @Override
        @TruffleBoundary
        public Double get(int index) {
            if (array != null) {
                return array[index];
            } else {
                return buffer.getDouble(index * sizeofElement);
            }
        }

        /**
         * @see <a href=
         *      "http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double">
         *      http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double </a>
         *
         * @return double[]
         */
        @TruffleBoundary
        public double[] array() {
            if (array != null) {
                return array;
            } else {
                DoubleBuffer b = buffer.asDoubleBuffer();
                double output[] = new double[b.remaining()];
                b.get(output);
                return output;
            }
        }

        @Override
        @TruffleBoundary
        public int grade() {
            return GRADE;
        }

        @Override
        public boolean isPrimitiveArray() {
            if (array != null) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int primitiveSize() {
            return array.length;
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            return isPrimitiveArray();
        }
    }

    private class IntegerByteBufferArray extends PrimitiveArray<Integer> {

        private final int GRADE = 1;
        private int[] array;

        public IntegerByteBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.INT, mode, init);
        }

        /**
         * @see <a href=
         *      "http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double">
         *      http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double </a>
         *
         * @return int[]
         */
        public int[] array() {
            if (array != null) {
                return array;
            } else {
                IntBuffer b = buffer.asIntBuffer();
                int output[] = new int[b.remaining()];
                b.get(output);
                return output;
            }
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            this.array = inputArray;
        }

        @Override
        public int[] intArray(int idx) {
            return array();
        }

        @Override
        public void put(int index, Integer data) {
            if (array != null) {
                array[index] = data;
            } else {
                buffer.putInt(index * sizeofElement, data);
            }
        }

        @Override
        public Integer get(int index) {
            if (array != null) {
                return array[index];
            } else {
                return buffer.getInt(index * sizeofElement);
            }
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public boolean isPrimitiveArray() {
            if (array != null) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int primitiveSize() {
            return array.length;
        }

        public boolean isPrimitiveArray(int idx) {
            return isPrimitiveArray();
        }
    }

    private class LongByteBufferArray extends PrimitiveArray<Long> {

        private final int GRADE = 1;

        public LongByteBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.LONG, mode, init);
        }

        /**
         * @see <a href=
         *      "http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double">
         *      http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double </a>
         *
         * @return long[]
         */
        public long[] array() {
            LongBuffer b = buffer.asLongBuffer();
            long output[] = new long[b.remaining()];
            b.get(output);
            return output;
        }

        @Override
        public void put(int index, Long data) {
            buffer.putLong(index * sizeofElement, data);
        }

        @Override
        public Long get(int index) {
            return buffer.getLong(index * sizeofElement);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        public boolean isPrimitiveArray(int idx) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private class ByteBufferArray extends PrimitiveArray<Byte> {

        private final int GRADE = 1;

        public ByteBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.BYTE, mode, init);
        }

        private byte[] array() {
            return buffer.array();
        }

        @Override
        public void put(int index, Byte i) {
            buffer.put(index * sizeofElement, i);
        }

        @Override
        public Byte get(int index) {
            return buffer.get(index * sizeofElement);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        public boolean isPrimitiveArray(int idx) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private class CharByteBufferArray extends PrimitiveArray<Character> {

        private final int GRADE = 1;

        public CharByteBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.BYTE, mode, init);
        }

        /**
         * @see <a href=
         *      "http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double">
         *      http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double </a>
         *
         * @return char[]
         */
        public char[] array() {
            CharBuffer b = buffer.asCharBuffer();
            char output[] = new char[b.remaining()];
            b.get(output);
            return output;
        }

        @Override
        public void put(int index, Character data) {
            buffer.putChar(index * sizeofElement, data);
        }

        @Override
        public Character get(int index) {
            return buffer.getChar(index * sizeofElement);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        public boolean isPrimitiveArray(int idx) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private class BooleanBufferArray extends PrimitiveArray<Character> {

        private final int GRADE = 1;

        public BooleanBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.BOOLEAN, mode, init);
        }

        @Override
        public void put(int index, Character i) {
            buffer.putChar(index * sizeofElement, i);
        }

        @Override
        public Character get(int index) {
            return buffer.getChar(index * sizeofElement);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        public boolean isPrimitiveArray(int idx) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private class ShortByteBufferArray extends PrimitiveArray<Short> {

        private final int GRADE = 1;

        public ShortByteBufferArray(int size, StorageMode mode, boolean init) {
            super(size, JavaDataTypeSizes.SHORT, mode, init);
        }

        /**
         * @see <a href=
         *      "http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double">
         *      http://stackoverflow.com/questions/12228152/java-convert-direct-bytebuffer-to-double </a>
         *
         * @return short[]
         */
        public short[] array() {
            ShortBuffer b = buffer.asShortBuffer();
            short output[] = new short[b.remaining()];
            b.get(output);
            return output;
        }

        @Override
        public void put(int index, Short s) {
            buffer.putShort(index * sizeofElement, s);
        }

        @Override
        public Short get(int index) {
            return buffer.getShort(index * sizeofElement);
        }

        @Override
        public int grade() {
            return GRADE;
        }

        public boolean isPrimitiveArray(int idx) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    /**
     * Tuple Arrays. Base class for {@link Tuple}. Each subclass will contain the {@link PArray}.
     *
     * @param <TE>
     */
    private abstract class TupleArray<TE> implements ArrayImplementation<TE> {
        @SuppressWarnings("unused")
        public RuntimeObjectTypeInfo[] getNestedTypes() {
            return type.getNestedTypes();
        }
    }

    private class Tuple2Array<T0, T1> extends TupleArray<Tuple2<T0, T1>> {

        private final int GRADE = 2;

        private PArray<T0> array0;
        private PArray<T1> array1;

        @TruffleBoundary
        public Tuple2Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 2);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
        }

        @Override
        @TruffleBoundary
        public void put(int index, Tuple2<T0, T1> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
        }

        @Override
        @TruffleBoundary
        public Tuple2<T0, T1> get(int index) {
            return new Tuple2<>(array0.get(index), array1.get(index));
        }

        @Override
        @TruffleBoundary
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
            }
            return array0.size();
        }

        @Override
        @TruffleBoundary
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public double[] doubleArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.asDoubleArray(0);
                case 1:
                    return array1.asDoubleArray(1);
                default:
                    return null;
            }
        }

        @Override
        @TruffleBoundary
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        @TruffleBoundary
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
            }
        }

        @Override
        @TruffleBoundary
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
            }
        }

        @Override
        @TruffleBoundary
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                default:
                    return false;
            }
        }

        @Override
        @TruffleBoundary
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
            }
        }

        @Override
        @TruffleBoundary
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
            }
        }

        @Override
        @TruffleBoundary
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                default:
                    return false;
            }
        }

        @Override
        @TruffleBoundary
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public int[] intArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.asIntegerArray();
                case 1:
                    return array1.asIntegerArray();
                default:
                    return null;
            }
        }

    }

    private class Tuple3Array<T0, T1, T2> extends TupleArray<Tuple3<T0, T1, T2>> {

        private final int GRADE = 3;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;

        public Tuple3Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 3);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
        }

        @Override
        public void put(int index, Tuple3<T0, T1, T2> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
        }

        @Override
        public Tuple3<T0, T1, T2> get(int index) {
            return new Tuple3<>(array0.get(index), array1.get(index), array2.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();

            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                default:
                    return false;
            }
        }

        @Override
        public int[] intArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.asIntegerArray();
                case 1:
                    return array1.asIntegerArray();
                case 2:
                    return array2.asIntegerArray();
                default:
                    return null;
            }
        }
    }

    private class Tuple4Array<T0, T1, T2, T3> extends TupleArray<Tuple4<T0, T1, T2, T3>> {

        private final int GRADE = 4;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;

        public Tuple4Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 4);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
        }

        @Override
        public void put(int index, Tuple4<T0, T1, T2, T3> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);

        }

        @Override
        public Tuple4<T0, T1, T2, T3> get(int index) {
            return new Tuple4<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();

            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                default:
                    return false;
            }
        }

    }

    private class Tuple5Array<T0, T1, T2, T3, T4> extends TupleArray<Tuple5<T0, T1, T2, T3, T4>> {

        private final int GRADE = 5;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;
        private PArray<T4> array4;

        public Tuple5Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 5);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
            array4 = new PArray<>(size, types[4], mode, init);
        }

        @Override
        public void put(int index, Tuple5<T0, T1, T2, T3, T4> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);
            array4.put(index, t._5);
        }

        @Override
        public Tuple5<T0, T1, T2, T3, T4> get(int index) {
            return new Tuple5<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index), array4.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();
                case 4:
                    return array4.size();

            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
                case 4:
                    array4.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
                case 4:
                    array4.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                case 4:
                    return array4.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
                case 4:
                    array4.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
                case 4:
                    array4.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                case 4:
                    return array4.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                case 4:
                    return array4.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
                case 4:
                    array4.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
                case 4:
                    array4.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                case 4:
                    return array4.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                case 4:
                    return array4.isCompass();
                default:
                    return false;
            }
        }
    }

    private class Tuple6Array<T0, T1, T2, T3, T4, T5> extends TupleArray<Tuple6<T0, T1, T2, T3, T4, T5>> {

        private final int GRADE = 6;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;
        private PArray<T4> array4;
        private PArray<T5> array5;

        public Tuple6Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 6);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
            array4 = new PArray<>(size, types[4], mode, init);
            array5 = new PArray<>(size, types[5], mode, init);
        }

        @Override
        public void put(int index, Tuple6<T0, T1, T2, T3, T4, T5> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);
            array4.put(index, t._5);
            array5.put(index, t._6);
        }

        @Override
        public Tuple6<T0, T1, T2, T3, T4, T5> get(int index) {
            return new Tuple6<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index), array4.get(index), array5.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();
                case 4:
                    return array4.size();
                case 5:
                    return array5.size();
            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
                case 4:
                    array4.setIntArray(inputArray);
                    break;
                case 5:
                    array5.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
                case 4:
                    array4.setDoubleArray(inputArray);
                    break;
                case 5:
                    array5.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                case 4:
                    return array4.getArrayReference();
                case 5:
                    return array5.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
                case 4:
                    array4.setBuffer(data);
                    break;
                case 5:
                    array5.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
                case 4:
                    array4.setSequence(sequence);
                    break;
                case 5:
                    array5.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                case 4:
                    return array4.isSequence();
                case 5:
                    return array5.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                case 4:
                    return array4.isPrimitiveArray();
                case 5:
                    return array5.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public double[] doubleArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.asDoubleArray(0);
                case 1:
                    return array1.asDoubleArray(1);
                case 2:
                    return array2.asDoubleArray(2);
                case 3:
                    return array3.asDoubleArray(3);
                case 4:
                    return array4.asDoubleArray(4);
                case 5:
                    return array5.asDoubleArray(5);
                default:
                    return null;
            }
        }

        @Override
        @TruffleBoundary
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
                case 4:
                    array4.setCompass(compass);
                    break;
                case 5:
                    array5.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
                case 4:
                    array4.setFlag(flag);
                    break;
                case 5:
                    array5.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                case 4:
                    return array4.isFlag();
                case 5:
                    return array5.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                case 4:
                    return array4.isCompass();
                case 5:
                    return array5.isCompass();
                default:
                    return false;
            }
        }

    }

    private class Tuple7Array<T0, T1, T2, T3, T4, T5, T6> extends TupleArray<Tuple7<T0, T1, T2, T3, T4, T5, T6>> {

        private final int GRADE = 7;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;
        private PArray<T4> array4;
        private PArray<T5> array5;
        private PArray<T6> array6;

        public Tuple7Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 7);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
            array4 = new PArray<>(size, types[4], mode, init);
            array5 = new PArray<>(size, types[5], mode, init);
            array6 = new PArray<>(size, types[6], mode, init);
        }

        @Override
        public void put(int index, Tuple7<T0, T1, T2, T3, T4, T5, T6> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);
            array4.put(index, t._5);
            array5.put(index, t._6);
            array6.put(index, t._7);
        }

        @Override
        public Tuple7<T0, T1, T2, T3, T4, T5, T6> get(int index) {
            return new Tuple7<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index), array4.get(index), array5.get(index), array6.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();
                case 4:
                    return array4.size();
                case 5:
                    return array5.size();
                case 6:
                    return array6.size();
            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
                case 4:
                    array4.setIntArray(inputArray);
                    break;
                case 5:
                    array5.setIntArray(inputArray);
                    break;
                case 6:
                    array6.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
                case 4:
                    array4.setDoubleArray(inputArray);
                    break;
                case 5:
                    array5.setDoubleArray(inputArray);
                    break;
                case 6:
                    array6.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                case 4:
                    return array4.getArrayReference();
                case 5:
                    return array5.getArrayReference();
                case 6:
                    return array6.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
                case 4:
                    array4.setBuffer(data);
                    break;
                case 5:
                    array5.setBuffer(data);
                    break;
                case 6:
                    array6.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
                case 4:
                    array4.setSequence(sequence);
                    break;
                case 5:
                    array5.setSequence(sequence);
                    break;
                case 6:
                    array6.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                case 4:
                    return array4.isSequence();
                case 5:
                    return array5.isSequence();
                case 6:
                    return array6.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                case 4:
                    return array4.isPrimitiveArray();
                case 5:
                    return array5.isPrimitiveArray();
                case 6:
                    return array6.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
                case 4:
                    array4.setCompass(compass);
                    break;
                case 5:
                    array5.setCompass(compass);
                    break;
                case 6:
                    array6.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
                case 4:
                    array4.setFlag(flag);
                    break;
                case 5:
                    array5.setFlag(flag);
                    break;
                case 6:
                    array6.setFlag(flag);
                    break;
            }
        }

        @Override
        @TruffleBoundary
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                case 4:
                    return array4.isFlag();
                case 5:
                    return array5.isFlag();
                case 6:
                    return array6.isFlag();
                default:
                    return false;
            }
        }

        @Override
        @TruffleBoundary
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                case 4:
                    return array4.isCompass();
                case 5:
                    return array5.isCompass();
                case 6:
                    return array6.isCompass();
                default:
                    return false;
            }
        }

    }

    private class Tuple8Array<T0, T1, T2, T3, T4, T5, T6, T7> extends TupleArray<Tuple8<T0, T1, T2, T3, T4, T5, T6, T7>> {

        private final int GRADE = 8;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;
        private PArray<T4> array4;
        private PArray<T5> array5;
        private PArray<T6> array6;
        private PArray<T7> array7;

        public Tuple8Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 8);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
            array4 = new PArray<>(size, types[4], mode, init);
            array5 = new PArray<>(size, types[5], mode, init);
            array6 = new PArray<>(size, types[6], mode, init);
            array7 = new PArray<>(size, types[7], mode, init);
        }

        @Override
        public void put(int index, Tuple8<T0, T1, T2, T3, T4, T5, T6, T7> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);
            array4.put(index, t._5);
            array5.put(index, t._6);
            array6.put(index, t._7);
            array7.put(index, t._8);
        }

        @Override
        public Tuple8<T0, T1, T2, T3, T4, T5, T6, T7> get(int index) {
            return new Tuple8<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index), array4.get(index), array5.get(index), array6.get(index), array7.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();
                case 4:
                    return array4.size();
                case 5:
                    return array5.size();
                case 6:
                    return array6.size();
                case 7:
                    return array7.size();
            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
                case 4:
                    array4.setIntArray(inputArray);
                    break;
                case 5:
                    array5.setIntArray(inputArray);
                    break;
                case 6:
                    array6.setIntArray(inputArray);
                    break;
                case 7:
                    array7.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
                case 4:
                    array4.setDoubleArray(inputArray);
                    break;
                case 5:
                    array5.setDoubleArray(inputArray);
                    break;
                case 6:
                    array6.setDoubleArray(inputArray);
                    break;
                case 7:
                    array7.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                case 4:
                    return array4.getArrayReference();
                case 5:
                    return array5.getArrayReference();
                case 6:
                    return array6.getArrayReference();
                case 7:
                    return array7.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
                case 4:
                    array4.setBuffer(data);
                    break;
                case 5:
                    array5.setBuffer(data);
                    break;
                case 6:
                    array6.setBuffer(data);
                    break;
                case 7:
                    array7.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
                case 4:
                    array4.setSequence(sequence);
                    break;
                case 5:
                    array5.setSequence(sequence);
                    break;
                case 6:
                    array6.setSequence(sequence);
                    break;
                case 7:
                    array7.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                case 4:
                    return array4.isSequence();
                case 5:
                    return array5.isSequence();
                case 6:
                    return array6.isSequence();
                case 7:
                    return array7.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                case 4:
                    return array4.isPrimitiveArray();
                case 5:
                    return array5.isPrimitiveArray();
                case 7:
                    return array7.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
                case 4:
                    array4.setCompass(compass);
                    break;
                case 5:
                    array5.setCompass(compass);
                    break;
                case 6:
                    array6.setCompass(compass);
                    break;
                case 7:
                    array7.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
                case 4:
                    array4.setFlag(flag);
                    break;
                case 5:
                    array5.setFlag(flag);
                    break;
                case 6:
                    array6.setFlag(flag);
                    break;
                case 7:
                    array7.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                case 4:
                    return array4.isFlag();
                case 5:
                    return array5.isFlag();
                case 6:
                    return array6.isFlag();
                case 7:
                    return array7.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                case 4:
                    return array4.isCompass();
                case 5:
                    return array5.isCompass();
                case 6:
                    return array6.isCompass();
                case 7:
                    return array7.isCompass();
                default:
                    return false;
            }
        }
    }

    private class Tuple9Array<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends TupleArray<Tuple9<T0, T1, T2, T3, T4, T5, T6, T7, T8>> {

        private final int GRADE = 9;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;
        private PArray<T4> array4;
        private PArray<T5> array5;
        private PArray<T6> array6;
        private PArray<T7> array7;
        private PArray<T8> array8;

        public Tuple9Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 9);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
            array4 = new PArray<>(size, types[4], mode, init);
            array5 = new PArray<>(size, types[5], mode, init);
            array6 = new PArray<>(size, types[6], mode, init);
            array7 = new PArray<>(size, types[7], mode, init);
            array8 = new PArray<>(size, types[8], mode, init);
        }

        @Override
        public void put(int index, Tuple9<T0, T1, T2, T3, T4, T5, T6, T7, T8> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);
            array4.put(index, t._5);
            array5.put(index, t._6);
            array6.put(index, t._7);
            array7.put(index, t._8);
            array8.put(index, t._9);
        }

        @Override
        public Tuple9<T0, T1, T2, T3, T4, T5, T6, T7, T8> get(int index) {
            return new Tuple9<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index), array4.get(index), array5.get(index), array6.get(index), array7.get(index),
                            array8.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();
                case 4:
                    return array4.size();
                case 5:
                    return array5.size();
                case 6:
                    return array6.size();
                case 7:
                    return array7.size();
                case 8:
                    return array8.size();
            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
                case 4:
                    array4.setIntArray(inputArray);
                    break;
                case 5:
                    array5.setIntArray(inputArray);
                    break;
                case 6:
                    array6.setIntArray(inputArray);
                    break;
                case 7:
                    array7.setIntArray(inputArray);
                    break;
                case 8:
                    array8.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
                case 4:
                    array4.setDoubleArray(inputArray);
                    break;
                case 5:
                    array5.setDoubleArray(inputArray);
                    break;
                case 6:
                    array6.setDoubleArray(inputArray);
                    break;
                case 7:
                    array7.setDoubleArray(inputArray);
                    break;
                case 8:
                    array8.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                case 4:
                    return array4.getArrayReference();
                case 5:
                    return array5.getArrayReference();
                case 6:
                    return array6.getArrayReference();
                case 7:
                    return array7.getArrayReference();
                case 8:
                    return array8.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
                case 4:
                    array4.setBuffer(data);
                    break;
                case 5:
                    array5.setBuffer(data);
                    break;
                case 6:
                    array6.setBuffer(data);
                    break;
                case 7:
                    array7.setBuffer(data);
                    break;
                case 8:
                    array8.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
                case 4:
                    array4.setSequence(sequence);
                    break;
                case 5:
                    array5.setSequence(sequence);
                    break;
                case 6:
                    array6.setSequence(sequence);
                    break;
                case 7:
                    array7.setSequence(sequence);
                    break;
                case 8:
                    array8.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                case 4:
                    return array4.isSequence();
                case 5:
                    return array5.isSequence();
                case 6:
                    return array6.isSequence();
                case 7:
                    return array7.isSequence();
                case 8:
                    return array8.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                case 4:
                    return array4.isPrimitiveArray();
                case 5:
                    return array5.isPrimitiveArray();
                case 7:
                    return array7.isPrimitiveArray();
                case 8:
                    return array8.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
                case 4:
                    array4.setCompass(compass);
                    break;
                case 5:
                    array5.setCompass(compass);
                    break;
                case 6:
                    array6.setCompass(compass);
                    break;
                case 7:
                    array7.setCompass(compass);
                    break;
                case 8:
                    array8.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
                case 4:
                    array4.setFlag(flag);
                    break;
                case 5:
                    array5.setFlag(flag);
                    break;
                case 6:
                    array6.setFlag(flag);
                    break;
                case 7:
                    array7.setFlag(flag);
                    break;
                case 8:
                    array8.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                case 4:
                    return array4.isFlag();
                case 5:
                    return array5.isFlag();
                case 6:
                    return array6.isFlag();
                case 7:
                    return array7.isFlag();
                case 8:
                    return array8.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                case 4:
                    return array4.isCompass();
                case 5:
                    return array5.isCompass();
                case 6:
                    return array6.isCompass();
                case 7:
                    return array7.isCompass();
                case 8:
                    return array8.isCompass();
                default:
                    return false;
            }
        }
    }

    private class Tuple10Array<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> extends TupleArray<Tuple10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>> {

        private final int GRADE = 10;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;
        private PArray<T4> array4;
        private PArray<T5> array5;
        private PArray<T6> array6;
        private PArray<T7> array7;
        private PArray<T8> array8;
        private PArray<T9> array9;

        public Tuple10Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 10);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
            array4 = new PArray<>(size, types[4], mode, init);
            array5 = new PArray<>(size, types[5], mode, init);
            array6 = new PArray<>(size, types[6], mode, init);
            array7 = new PArray<>(size, types[7], mode, init);
            array8 = new PArray<>(size, types[8], mode, init);
            array9 = new PArray<>(size, types[9], mode, init);
        }

        @Override
        public void put(int index, Tuple10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);
            array4.put(index, t._5);
            array5.put(index, t._6);
            array6.put(index, t._7);
            array7.put(index, t._8);
            array8.put(index, t._9);
            array9.put(index, t._10);
        }

        @Override
        public Tuple10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> get(int index) {
            return new Tuple10<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index), array4.get(index), array5.get(index), array6.get(index), array7.get(index),
                            array8.get(index), array9.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();
                case 4:
                    return array4.size();
                case 5:
                    return array5.size();
                case 6:
                    return array6.size();
                case 7:
                    return array7.size();
                case 8:
                    return array8.size();
                case 9:
                    return array9.size();
            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
                case 4:
                    array4.setIntArray(inputArray);
                    break;
                case 5:
                    array5.setIntArray(inputArray);
                    break;
                case 6:
                    array6.setIntArray(inputArray);
                    break;
                case 7:
                    array7.setIntArray(inputArray);
                    break;
                case 8:
                    array8.setIntArray(inputArray);
                    break;
                case 9:
                    array9.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
                case 4:
                    array4.setDoubleArray(inputArray);
                    break;
                case 5:
                    array5.setDoubleArray(inputArray);
                    break;
                case 6:
                    array6.setDoubleArray(inputArray);
                    break;
                case 7:
                    array7.setDoubleArray(inputArray);
                    break;
                case 8:
                    array8.setDoubleArray(inputArray);
                    break;
                case 9:
                    array9.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                case 4:
                    return array4.getArrayReference();
                case 5:
                    return array5.getArrayReference();
                case 6:
                    return array6.getArrayReference();
                case 7:
                    return array7.getArrayReference();
                case 8:
                    return array8.getArrayReference();
                case 9:
                    return array9.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
                case 4:
                    array4.setBuffer(data);
                    break;
                case 5:
                    array5.setBuffer(data);
                    break;
                case 6:
                    array6.setBuffer(data);
                    break;
                case 7:
                    array7.setBuffer(data);
                    break;
                case 8:
                    array8.setBuffer(data);
                    break;
                case 9:
                    array9.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
                case 4:
                    array4.setSequence(sequence);
                    break;
                case 5:
                    array5.setSequence(sequence);
                    break;
                case 6:
                    array6.setSequence(sequence);
                    break;
                case 7:
                    array7.setSequence(sequence);
                    break;
                case 8:
                    array8.setSequence(sequence);
                    break;
                case 9:
                    array9.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                case 4:
                    return array4.isSequence();
                case 5:
                    return array5.isSequence();
                case 6:
                    return array6.isSequence();
                case 7:
                    return array7.isSequence();
                case 8:
                    return array8.isSequence();
                case 9:
                    return array9.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                case 4:
                    return array4.isPrimitiveArray();
                case 5:
                    return array5.isPrimitiveArray();
                case 7:
                    return array7.isPrimitiveArray();
                case 8:
                    return array8.isPrimitiveArray();
                case 9:
                    return array9.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
                case 4:
                    array4.setCompass(compass);
                    break;
                case 5:
                    array5.setCompass(compass);
                    break;
                case 6:
                    array6.setCompass(compass);
                    break;
                case 7:
                    array7.setCompass(compass);
                    break;
                case 8:
                    array8.setCompass(compass);
                    break;
                case 9:
                    array9.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
                case 4:
                    array4.setFlag(flag);
                    break;
                case 5:
                    array5.setFlag(flag);
                    break;
                case 6:
                    array6.setFlag(flag);
                    break;
                case 7:
                    array7.setFlag(flag);
                    break;
                case 8:
                    array8.setFlag(flag);
                    break;
                case 9:
                    array9.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                case 4:
                    return array4.isFlag();
                case 5:
                    return array5.isFlag();
                case 6:
                    return array6.isFlag();
                case 7:
                    return array7.isFlag();
                case 8:
                    return array8.isFlag();
                case 9:
                    return array9.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                case 4:
                    return array4.isCompass();
                case 5:
                    return array5.isCompass();
                case 6:
                    return array6.isCompass();
                case 7:
                    return array7.isCompass();
                case 8:
                    return array8.isCompass();
                case 9:
                    return array9.isCompass();
                default:
                    return false;
            }
        }
    }

    private class Tuple11Array<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> extends TupleArray<Tuple11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> {

        private final int GRADE = 11;

        private PArray<T0> array0;
        private PArray<T1> array1;
        private PArray<T2> array2;
        private PArray<T3> array3;
        private PArray<T4> array4;
        private PArray<T5> array5;
        private PArray<T6> array6;
        private PArray<T7> array7;
        private PArray<T8> array8;
        private PArray<T9> array9;
        private PArray<T10> array10;

        public Tuple11Array(int size, RuntimeObjectTypeInfo[] types, StorageMode mode, boolean init) {
            assert (types.length == 11);
            array0 = new PArray<>(size, types[0], mode, init);
            array1 = new PArray<>(size, types[1], mode, init);
            array2 = new PArray<>(size, types[2], mode, init);
            array3 = new PArray<>(size, types[3], mode, init);
            array4 = new PArray<>(size, types[4], mode, init);
            array5 = new PArray<>(size, types[5], mode, init);
            array6 = new PArray<>(size, types[6], mode, init);
            array7 = new PArray<>(size, types[7], mode, init);
            array8 = new PArray<>(size, types[8], mode, init);
            array9 = new PArray<>(size, types[9], mode, init);
            array10 = new PArray<>(size, types[10], mode, init);
        }

        @Override
        public void put(int index, Tuple11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> t) {
            array0.put(index, t._1);
            array1.put(index, t._2);
            array2.put(index, t._3);
            array3.put(index, t._4);
            array4.put(index, t._5);
            array5.put(index, t._6);
            array6.put(index, t._7);
            array7.put(index, t._8);
            array8.put(index, t._9);
            array9.put(index, t._10);
            array10.put(index, t._11);
        }

        @Override
        public Tuple11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> get(int index) {
            return new Tuple11<>(array0.get(index), array1.get(index), array2.get(index), array3.get(index), array4.get(index), array5.get(index), array6.get(index), array7.get(index),
                            array8.get(index), array9.get(index), array10.get(index));
        }

        @Override
        public int size(int index) {
            switch (index) {
                case 0:
                    return array0.size();
                case 1:
                    return array1.size();
                case 2:
                    return array2.size();
                case 3:
                    return array3.size();
                case 4:
                    return array4.size();
                case 5:
                    return array5.size();
                case 6:
                    return array6.size();
                case 7:
                    return array7.size();
                case 8:
                    return array8.size();
                case 9:
                    return array9.size();
                case 10:
                    return array10.size();
            }
            return array0.size();
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public void setIntArray(int idx, int[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setIntArray(inputArray);
                    break;
                case 1:
                    array1.setIntArray(inputArray);
                    break;
                case 2:
                    array2.setIntArray(inputArray);
                    break;
                case 3:
                    array3.setIntArray(inputArray);
                    break;
                case 4:
                    array4.setIntArray(inputArray);
                    break;
                case 5:
                    array5.setIntArray(inputArray);
                    break;
                case 6:
                    array6.setIntArray(inputArray);
                    break;
                case 7:
                    array7.setIntArray(inputArray);
                    break;
                case 8:
                    array8.setIntArray(inputArray);
                    break;
                case 9:
                    array9.setIntArray(inputArray);
                    break;
                case 10:
                    array10.setIntArray(inputArray);
                    break;
            }
        }

        @Override
        public void setDoubleArray(int idx, double[] inputArray) {
            switch (idx) {
                case 0:
                    array0.setDoubleArray(inputArray);
                    break;
                case 1:
                    array1.setDoubleArray(inputArray);
                    break;
                case 2:
                    array2.setDoubleArray(inputArray);
                    break;
                case 3:
                    array3.setDoubleArray(inputArray);
                    break;
                case 4:
                    array4.setDoubleArray(inputArray);
                    break;
                case 5:
                    array5.setDoubleArray(inputArray);
                    break;
                case 6:
                    array6.setDoubleArray(inputArray);
                    break;
                case 7:
                    array7.setDoubleArray(inputArray);
                    break;
                case 8:
                    array8.setDoubleArray(inputArray);
                    break;
                case 9:
                    array9.setDoubleArray(inputArray);
                    break;
                case 10:
                    array10.setDoubleArray(inputArray);
                    break;
            }
        }

        @Override
        public Buffer getArrayReference(int idxArray) {
            switch (idxArray) {
                case 0:
                    return array0.getArrayReference();
                case 1:
                    return array1.getArrayReference();
                case 2:
                    return array2.getArrayReference();
                case 3:
                    return array3.getArrayReference();
                case 4:
                    return array4.getArrayReference();
                case 5:
                    return array5.getArrayReference();
                case 6:
                    return array6.getArrayReference();
                case 7:
                    return array7.getArrayReference();
                case 8:
                    return array8.getArrayReference();
                case 9:
                    return array9.getArrayReference();
                case 10:
                    return array10.getArrayReference();
                default:
                    return null;
            }
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            switch (idxArrayRef) {
                case 0:
                    array0.setBuffer(data);
                    break;
                case 1:
                    array1.setBuffer(data);
                    break;
                case 2:
                    array2.setBuffer(data);
                    break;
                case 3:
                    array3.setBuffer(data);
                    break;
                case 4:
                    array4.setBuffer(data);
                    break;
                case 5:
                    array5.setBuffer(data);
                    break;
                case 6:
                    array6.setBuffer(data);
                    break;
                case 7:
                    array7.setBuffer(data);
                    break;
                case 8:
                    array8.setBuffer(data);
                    break;
                case 9:
                    array9.setBuffer(data);
                    break;
                case 10:
                    array10.setBuffer(data);
                    break;
            }
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            switch (idx) {
                case 0:
                    array0.setSequence(sequence);
                    break;
                case 1:
                    array1.setSequence(sequence);
                    break;
                case 2:
                    array2.setSequence(sequence);
                case 3:
                    array3.setSequence(sequence);
                    break;
                case 4:
                    array4.setSequence(sequence);
                    break;
                case 5:
                    array5.setSequence(sequence);
                    break;
                case 6:
                    array6.setSequence(sequence);
                    break;
                case 7:
                    array7.setSequence(sequence);
                    break;
                case 8:
                    array8.setSequence(sequence);
                    break;
                case 9:
                    array9.setSequence(sequence);
                    break;
                case 10:
                    array10.setSequence(sequence);
                    break;
            }
        }

        @Override
        public boolean isSequence(int idx) {
            switch (idx) {
                case 0:
                    return array0.isSequence();
                case 1:
                    return array1.isSequence();
                case 2:
                    return array2.isSequence();
                case 3:
                    return array3.isSequence();
                case 4:
                    return array4.isSequence();
                case 5:
                    return array5.isSequence();
                case 6:
                    return array6.isSequence();
                case 7:
                    return array7.isSequence();
                case 8:
                    return array8.isSequence();
                case 9:
                    return array9.isSequence();
                case 10:
                    return array10.isSequence();
                default:
                    return false;
            }
        }

        @Override
        public boolean isPrimitiveArray(int idx) {
            switch (idx) {
                case 0:
                    return array0.isPrimitiveArray();
                case 1:
                    return array1.isPrimitiveArray();
                case 2:
                    return array2.isPrimitiveArray();
                case 3:
                    return array3.isPrimitiveArray();
                case 4:
                    return array4.isPrimitiveArray();
                case 5:
                    return array5.isPrimitiveArray();
                case 7:
                    return array7.isPrimitiveArray();
                case 8:
                    return array8.isPrimitiveArray();
                case 9:
                    return array9.isPrimitiveArray();
                case 10:
                    return array10.isPrimitiveArray();
                default:
                    return false;
            }
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            switch (idx) {
                case 0:
                    array0.setCompass(compass);
                    break;
                case 1:
                    array1.setCompass(compass);
                    break;
                case 2:
                    array2.setCompass(compass);
                    break;
                case 3:
                    array3.setCompass(compass);
                    break;
                case 4:
                    array4.setCompass(compass);
                    break;
                case 5:
                    array5.setCompass(compass);
                    break;
                case 6:
                    array6.setCompass(compass);
                    break;
                case 7:
                    array7.setCompass(compass);
                    break;
                case 8:
                    array8.setCompass(compass);
                    break;
                case 9:
                    array9.setCompass(compass);
                    break;
                case 10:
                    array10.setCompass(compass);
                    break;
            }
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            switch (idx) {
                case 0:
                    array0.setFlag(flag);
                    break;
                case 1:
                    array1.setFlag(flag);
                    break;
                case 2:
                    array2.setFlag(flag);
                    break;
                case 3:
                    array3.setFlag(flag);
                    break;
                case 4:
                    array4.setFlag(flag);
                    break;
                case 5:
                    array5.setFlag(flag);
                    break;
                case 6:
                    array6.setFlag(flag);
                    break;
                case 7:
                    array7.setFlag(flag);
                    break;
                case 8:
                    array8.setFlag(flag);
                    break;
                case 9:
                    array9.setFlag(flag);
                    break;
                case 10:
                    array10.setFlag(flag);
                    break;
            }
        }

        @Override
        public boolean isFlag(int idx) {
            switch (idx) {
                case 0:
                    return array0.isFlag();
                case 1:
                    return array1.isFlag();
                case 2:
                    return array2.isFlag();
                case 3:
                    return array3.isFlag();
                case 4:
                    return array4.isFlag();
                case 5:
                    return array5.isFlag();
                case 6:
                    return array6.isFlag();
                case 7:
                    return array7.isFlag();
                case 8:
                    return array8.isFlag();
                case 9:
                    return array9.isFlag();
                case 10:
                    return array10.isFlag();
                default:
                    return false;
            }
        }

        @Override
        public boolean isCompass(int idx) {
            switch (idx) {
                case 0:
                    return array0.isCompass();
                case 1:
                    return array1.isCompass();
                case 2:
                    return array2.isCompass();
                case 3:
                    return array3.isCompass();
                case 4:
                    return array4.isCompass();
                case 5:
                    return array5.isCompass();
                case 6:
                    return array6.isCompass();
                case 7:
                    return array7.isCompass();
                case 8:
                    return array8.isCompass();
                case 9:
                    return array9.isCompass();
                case 10:
                    return array10.isCompass();
                default:
                    return false;
            }
        }
    }

    // Generic Java Object Storage
    private class JavaObjectStorageArray<EE> implements ArrayImplementation<EE> {

        private final int GRADE = 1;

        private Object[] storage;

        JavaObjectStorageArray(int size) {
            storage = new Object[size];
        }

        @Override
        public void put(int index, EE e) {
            storage[index] = e;
        }

        @SuppressWarnings("unchecked")
        @Override
        public EE get(int index) {
            return (EE) storage[index];
        }

        @Override
        public int size(int index) {
            return storage.length;
        }

        @Override
        public int grade() {
            return GRADE;
        }

        @Override
        public ByteBuffer getArrayReference(int idxArrayRef) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }

        @Override
        public void setArrayReference(int idxArrayRef, Buffer data) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }

        @Override
        public boolean isSequence(int idx) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }

        @Override
        public void setSequence(int idx, boolean sequence) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }

        @Override
        public void setCompass(int idx, boolean compass) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }

        @Override
        public void setFlag(int idx, boolean flag) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }

        @Override
        public boolean isPrimitiveArray() {
            return false;
        }

        public boolean isFlag(int idx) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }

        public boolean isCompass(int idx) {
            assert (false); // This function should never be called
            throw new RuntimeException("It should not reach this point");
        }
    }

    /**
     * Print a PArray. If tuple, it will print the tuples in format:
     *
     * <code>
     * TupleX[ <a1, .. x1> , <an, .., Xn> ]).
     * </code>
     *
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("[ ");
        int size = isSequence() ? getTotalSizeWhenSequence() : size();
        for (int i = 0; i < size; i++) {
            buffer.append(arrayImplementation.get(i) + ",");
        }
        buffer.replace(buffer.length() - 1, buffer.length(), " ]");
        return buffer.toString();
    }

    @SuppressWarnings("unused")
    public void setPrimitive(boolean primitiveArray) {

    }

    public void enableSequence(T start, T stride, T aux) {
        this.put(0, start);
        this.put(1, stride);
        this.put(2, aux);
        this.setSequence(true);
    }

}
