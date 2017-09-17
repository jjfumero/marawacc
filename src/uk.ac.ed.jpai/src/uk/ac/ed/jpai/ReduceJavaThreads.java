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

import uk.ac.ed.datastructures.common.ArraySlice;
import uk.ac.ed.datastructures.common.PArray;

public class ReduceJavaThreads<T> extends Reduce<T> {

    private int numberOfThreads;

    public ReduceJavaThreads(BiFunction<T, T, T> f, T init) {
        super(f, init);
        numberOfThreads = Runtime.getRuntime().availableProcessors();
    }

    @Override
    public PArray<T> apply(PArray<T> input) {

        PArray<T> output = new PArray<>(numberOfThreads, outputType);

        ArraySlice<T>[] inputSlices = input.splitInFixedNumberOfChunks(numberOfThreads);

        Thread[] threads = new Thread[numberOfThreads];

        for (int t = 0; t < numberOfThreads; t++) {
            int j = t;
            threads[t] = new Thread(() -> {
                T acc = accumulator;
                for (int i = 0; i < inputSlices[j].size(); ++i) {
                    acc = biFunction.apply(acc, inputSlices[j].get(i));
                }
                output.put(j, acc);
            });

            threads[t].start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }

        } catch (InterruptedException e) {
            System.err.println("Error in MapJavaThreads map execution (join) operation");
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println(output);

        // Final reduction (sequentially)
        accumulator = output.get(0);
        for (int i = 1; i < numberOfThreads; ++i) {
            T acc = accumulator;
            accumulator = biFunction.apply(acc, output.get(i));
        }
        PArray<T> outputReduce = new PArray<>(1, outputType);
        outputReduce.put(0, accumulator);

        return outputReduce;
    }
}
