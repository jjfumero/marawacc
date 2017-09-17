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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
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

    // Class for Graal compilation when running a user function (UDF) with Java threads in JPAI
    private static class MethodCompilation {
        private static MethodCompilation instance = null;
        private boolean compiling = false;
        private JPAICompileFunctionThread<?, ?> compilation;

        public static MethodCompilation getInstance() {
            if (instance == null) {
                instance = new MethodCompilation();
            }
            return instance;
        }

        private MethodCompilation() {
        }

        public synchronized <inT, outT> void compileMethod(Function<inT, outT> function) {
            if (!compiling) {
                compiling = true;
                compilation = new JPAICompileFunctionThread<>(function);
                compilation.start();
            }
        }

        public boolean isCompilationFinished() {
            if (compilation == null) {
                return false;
            }
            return compilation.isCompilationFinished();
        }

        public InstalledCode getCompileFunction() {
            if (compilation == null) {
                return null;
            }
            return compilation.getInstalledCode();
        }

        public void clean() {
            instance = null;
        }
    }

    @SuppressWarnings("unchecked")
    public PArray<outT> applyAndCompilation(PArray<inT> input) {

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

        AtomicInteger atomicCounter = new AtomicInteger(0);
        AtomicBoolean compiling = new AtomicBoolean(false);

        // each thread executes a sequential map
        Thread[] threads = new Thread[numberOfThreads];
        for (int t = 0; t < numberOfThreads; t++) {
            int j = t;
            threads[t] = new Thread(() -> {

                // Logic per thread
                for (int i = 0; i < inputSlices[j].size(); ++i) {

                    synchronized (compiling) {
                        int incrementAndGet = 0;
                        if (!compiling.get()) {
                            incrementAndGet = atomicCounter.incrementAndGet();
                        }
                        if (!compiling.get() && incrementAndGet >= GraalAcceleratorOptions.threadsGraalCompilationThreshold) {
                            compiling.set(true);
                            MethodCompilation.getInstance().compileMethod(function);
                        }
                    }

                    outT result = null;
                    if (MethodCompilation.getInstance().isCompilationFinished()) {
                        try {
                            result = (outT) MethodCompilation.getInstance().getCompileFunction().executeVarargs(inputSlices[j].get(i));
                        } catch (InvalidInstalledCodeException e) {
                            System.out.println("Error when compiling with Graal");
                            result = function.apply(inputSlices[j].get(i));
                        }

                    } else {
                        result = function.apply(inputSlices[j].get(i));
                    }
                    outputSlices[j].put(i, result);
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
        MethodCompilation.getInstance().clean();
        return output;
    }

    public PArray<outT> applyUsingC2(PArray<inT> input) {

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
                // Logic per thread
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

    @Override
    public PArray<outT> apply(PArray<inT> input) {
        if (GraalAcceleratorOptions.threadsGraalCompilation) {
            return applyAndCompilation(input);
        } else {
            return applyUsingC2(input);
        }
    }
}
