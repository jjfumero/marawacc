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

import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;

/**
 * Public static class for calling JPAI for array programming with Graal and OpenCL. It defines the
 * common operations.
 *
 */
public class Marawacc {

    /**
     * Zip functions. It combines tuples in a unique Array Function.
     *
     * @return {@link ArrayFunction}
     */
    public static <T0, T1> ArrayFunction<Tuple2<T0, T1>, Tuple2<T0, T1>> zip2() {
        return new Identity<>();
    }

    public static <T0, T1, T2> ArrayFunction<Tuple3<T0, T1, T2>, Tuple3<T0, T1, T2>> zip3() {
        return new Identity<>();
    }

    /**
     * Basic map skeleton
     *
     * @param f
     * @return {@link ArrayFunction}
     */
    public static <T0, T1> ArrayFunction<T0, T1> map(Function<T0, T1> f) {
        return new MapArrayFunction<>(f);
    }

    /**
     * Java thread version for the map skeleton
     *
     * @param numberOfThreads
     * @param f
     * @return {@link ArrayFunction}
     */
    public static <T0, T1> ArrayFunction<T0, T1> mapJavaThreads(int numberOfThreads, Function<T0, T1> f) {
        return new MapJavaThreads<>(numberOfThreads, f);
    }

    /**
     * Accelerator (GPU) version for the map parallel skeleton.
     *
     * @param f
     * @return {@link ArrayFunction}
     */
    public static <T0, T1> ArrayFunction<T0, T1> mapAccelerator(Function<T0, T1> f) {
        return new MapAccelerator<>(f);
    }

    /**
     * General parallel skeleton for reduction.
     *
     * @param f
     * @param init
     * @return {@link ArrayFunction}
     */
    public static <T> ArrayFunction<T, T> reduce(BiFunction<T, T, T> f, T init) {
        return new Reduce<>(f, init);
    }

    /**
     * Pipeline for two {@link ArrayFunction}.
     *
     * @param chunkSize
     * @param stage0
     * @param stage1
     * @return {@link ArrayFunction}
     */
    public static <T0, T1, T2> ArrayFunction<T0, T2> pipeline(int chunkSize, ArrayFunction<T0, T1> stage0, ArrayFunction<T1, T2> stage1) {
        return Pipeline.create(chunkSize, stage0, stage1);
    }

    /**
     * Pipeline for 3 {@link ArrayFunction}.
     *
     * @param chunkSize
     * @param stage0
     * @param stage1
     * @param stage2
     * @return {@link ArrayFunction}
     */
    public static <T0, T1, T2, T3> ArrayFunction<T0, T3> pipeline(int chunkSize, ArrayFunction<T0, T1> stage0, ArrayFunction<T1, T2> stage1, ArrayFunction<T2, T3> stage2) {
        return Pipeline.create(chunkSize, stage0, stage1, stage2);
    }

    /**
     * Pipeline for 4 {@link ArrayFunction}.
     *
     * @param chunkSize
     * @param stage0
     * @param stage1
     * @param stage2
     * @param stage3
     * @return {@link ArrayFunction}
     */
    public static <T0, T1, T2, T3, T4> ArrayFunction<T0, T4> pipeline(int chunkSize, ArrayFunction<T0, T1> stage0, ArrayFunction<T1, T2> stage1, ArrayFunction<T2, T3> stage2,
                    ArrayFunction<T3, T4> stage3) {
        return Pipeline.create(chunkSize, stage0, stage1, stage2, stage3);
    }
}
