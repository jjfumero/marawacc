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

package uk.ac.ed.jpai;

import java.util.function.BiFunction;
import java.util.function.Function;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.interop.InteropTable;

/**
 * ArrayFunction base class. It defines the common operations (parallel skeletons): map, reduce,
 * pipeline and apply.
 *
 * @param <inT>
 * @param <outT>
 */
public abstract class ArrayFunction<inT, outT> implements Function<PArray<inT>, PArray<outT>> {

    /**
     * Runtime data type information class. It is used for input data type inference.
     */
    protected RuntimeObjectTypeInfo inputType = null;

    /**
     * Runtime data type information class. It is used for output data type inference.
     */
    protected RuntimeObjectTypeInfo outputType = null;

    protected boolean preparedExecutionFinish = false;

    /**
     * Method to implement in each skeleton. The programmer will call apply method in the user
     * program. This is the heart of ArrayFunction.
     */
    public abstract PArray<outT> apply(PArray<inT> input);

    /**
     * Method for compiling and obtaining types before executing the user code.
     */
    public abstract PArray<outT> prepareExecution(PArray<inT> input);

    /**
     * Infer the output type.
     */
    public abstract PArray<outT> inferTypes(PArray<inT> input);

    /**
     * It allocates an instance of PArray.
     */
    public PArray<outT> allocateOutputArray(int size, StorageMode mode) {
        return new PArray<>(size, outputType, mode, true);
    }

    /**
     * It allocates an instance of the PArray given the size, mode and the interop. This method is
     * prepared for Truffle framework conection.
     */
    public PArray<outT> allocateOutputArray(int size, StorageMode mode, InteropTable interop) {
        return new PArray<>(size, outputType, mode, interop);
    }

    public abstract void setOutput(PArray<outT> output);

    public abstract boolean isInCache();

    // This will return null if inferTypes() has not been called yet
    public RuntimeObjectTypeInfo getInputType() {
        return inputType;
    }

    // This will return null if inferTypes() has not been called yet
    public RuntimeObjectTypeInfo getOutputType() {
        return outputType;
    }

    /**
     * Execute one function after another. It keeps the operation in the pipeline.
     *
     * @param function
     * @return {@link ArrayFunction}
     */
    public <T> ArrayFunction<inT, T> andThen(ArrayFunction<outT, T> function) {
        return new ArrayFunctionComposition<>(this, function);
    }

    /**
     *
     * @param function
     * @return {@link ArrayFunction}
     */
    public <T> ArrayFunction<inT, T> map(Function<outT, T> function) {
        return new ArrayFunctionComposition<>(this, new MapArrayFunction<>(function));
    }

    /**
     * Map Java Threads parallel skeleton. The number of threads is the number of cores available at
     * runtime.
     *
     * @param function
     * @return {@link ArrayFunction}
     */
    public <T> ArrayFunction<inT, T> mapJavaThreads(Function<outT, T> function) {
        return new ArrayFunctionComposition<>(this, new MapJavaThreads<>(function));
    }

    /**
     * Map Java Threads parallel skeleton. It receives the number of threads and the function to be
     * executed.
     *
     * @param nThreads
     * @param function
     * @return {@link ArrayFunction}
     */
    public <T> ArrayFunction<inT, T> mapJavaThreads(int nThreads, Function<outT, T> function) {
        return new ArrayFunctionComposition<>(this, new MapJavaThreads<>(nThreads, function));
    }

    /**
     * Map accelerator skeleton. It will execute the function on the GPU.
     *
     * @param function : {@link java.util.function.Function}
     * @return {@link ArrayFunction}
     */
    public <T> ArrayFunction<inT, T> mapAccelerator(Function<outT, T> function) {
        return new ArrayFunctionComposition<>(this, new MapAccelerator<>(function));
    }

    /**
     * Reduce skeleton.
     *
     * @param function : {@link java.util.function.BiFunction}
     * @param: init : neutral element
     * @return {@link ArrayFunction} input.getStorageMode()
     */
    public ArrayFunction<inT, outT> reduce(BiFunction<outT, outT, outT> function, outT neutral) {
        return new ArrayFunctionComposition<>(this, new Reduce<>(function, neutral));
    }

    /**
     * It builds a pipeline with two stages.
     *
     * @param chunkSize
     * @param stage0
     * @param stage1
     * @return {@link ArrayFunction}
     */
    public <T0, T1> ArrayFunction<inT, T1> pipeline(int chunkSize, ArrayFunction<outT, T0> stage0, ArrayFunction<T0, T1> stage1) {
        return new ArrayFunctionComposition<>(this, Pipeline.create(chunkSize, stage0, stage1));
    }

    /**
     * Pipeline factory for three operations.
     *
     * @param chunkSize
     * @param stage0
     * @param stage1
     * @param stage2
     * @return {@link ArrayFunction}
     */
    public <T0, T1, T2> ArrayFunction<inT, T2> pipeline(int chunkSize, ArrayFunction<outT, T0> stage0, ArrayFunction<T0, T1> stage1, ArrayFunction<T1, T2> stage2) {
        return new ArrayFunctionComposition<>(this, Pipeline.create(chunkSize, stage0, stage1, stage2));
    }

    /**
     * Pipeline factory for four stages.
     *
     * @param chunkSize
     * @param stage0
     * @param stage1
     * @param stage2
     * @param stage3
     * @return {@link ArrayFunction}
     */
    public <T0, T1, T2, T3> ArrayFunction<inT, T3> pipeline(int chunkSize, ArrayFunction<outT, T0> stage0, ArrayFunction<T0, T1> stage1, ArrayFunction<T1, T2> stage2, ArrayFunction<T2, T3> stage3) {
        return new ArrayFunctionComposition<>(this, Pipeline.create(chunkSize, stage0, stage1, stage2, stage3));
    }

}
