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

import java.util.function.Function;

import jdk.vm.ci.code.InvalidInstalledCodeException;
import uk.ac.ed.datastructures.common.ArraySlice;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.jpai.jit.JPAICompileFunctionThread;

public class MapJavaThreads<inT, outT> extends MapArrayFunction<inT, outT> {

    private int numberOfThreads;

    public MapJavaThreads(Function<inT, outT> f) {
        // as default: get number of available cores
        this(Runtime.getRuntime().availableProcessors(), f);
    }

    public MapJavaThreads(int numberOfThreads, Function<inT, outT> f) {
        super(f);
        this.numberOfThreads = numberOfThreads;
    }

    // Example of custom compilation
    private void customUDFCompilation(PArray<inT> input) {
        JPAICompileFunctionThread<inT, outT> compilation = new JPAICompileFunctionThread<>(function);
        compilation.start();

        try {
            compilation.join();
        } catch (InterruptedException e) {

        }

        try {
            outT executeCompiledCode = compilation.executeCompiledCode(input.get(0));
            System.out.println("Result: " + executeCompiledCode);
        } catch (InvalidInstalledCodeException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public PArray<outT> apply(PArray<inT> input) {

        if (!preparedExecutionFinish) {
            prepareExecution(input);
        }

        if (numberOfThreads == 0) {
            // provoked from a deoptimization
            numberOfThreads = Runtime.getRuntime().availableProcessors();
        }

        if (output == null) {
            output = allocateOutputArray(input.size(), input.getStorageMode());
        }

        ArraySlice<inT>[] inputSlices = input.splitInFixedNumberOfChunks(numberOfThreads);
        ArraySlice<outT>[] outputSlices = output.splitInFixedNumberOfChunks(numberOfThreads);

        // each thread executes a sequential map
        Thread[] threads = new Thread[numberOfThreads];
        for (int t = 0; t < numberOfThreads; t++) {
            int j = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < inputSlices[j].size(); ++i) {
                    outputSlices[j].put(i, function.apply(inputSlices[j].get(i)));
                }
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

        return output;
    }
}
