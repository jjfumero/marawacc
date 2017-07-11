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

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;

public final class Pipeline<inT, outT> extends ArrayFunction<inT, outT> {

    private int chunkSize;

    private ArrayBlockingQueue<PArray<inT>> inputQueue;

    private ArrayList<AbstractPipelineStage<?, ?>> stages;

    private PArray<outT> output;

    private Pipeline(int chunkSize) {
        this.chunkSize = chunkSize;
        this.inputQueue = new ArrayBlockingQueue<>(1);
        this.stages = new ArrayList<>();
        this.output = null;
    }

    public static <T0, T1, T2> Pipeline<T0, T2> create(int chunkSize, ArrayFunction<T0, T1> stage0, ArrayFunction<T1, T2> stage1) {
        Pipeline<T0, T2> p = new Pipeline<>(chunkSize);
        ArrayBlockingQueue<PArray<T1>> queue1 = new ArrayBlockingQueue<>(1);

        p.stages.add(new IntermediatePipelineStage<>(stage0, p.inputQueue, queue1));
        p.stages.add(new LastPipelineStage<>(stage1, queue1));
        return p;
    }

    public static <T0, T1, T2, T3> Pipeline<T0, T3> create(int chunkSize, ArrayFunction<T0, T1> stage0, ArrayFunction<T1, T2> stage1, ArrayFunction<T2, T3> stage2) {
        Pipeline<T0, T3> pipeline = new Pipeline<>(chunkSize);
        ArrayBlockingQueue<PArray<T1>> queue1 = new ArrayBlockingQueue<>(1);
        ArrayBlockingQueue<PArray<T2>> queue2 = new ArrayBlockingQueue<>(1);

        pipeline.stages.add(new IntermediatePipelineStage<>(stage0, pipeline.inputQueue, queue1));
        pipeline.stages.add(new IntermediatePipelineStage<>(stage1, queue1, queue2));
        pipeline.stages.add(new LastPipelineStage<>(stage2, queue2));
        return pipeline;
    }

    public static <T0, T1, T2, T3, T4> Pipeline<T0, T4> create(int chunkSize, ArrayFunction<T0, T1> stage0, ArrayFunction<T1, T2> stage1, ArrayFunction<T2, T3> stage2, ArrayFunction<T3, T4> stage3) {
        Pipeline<T0, T4> pipeline = new Pipeline<>(chunkSize);
        ArrayBlockingQueue<PArray<T1>> queue1 = new ArrayBlockingQueue<>(1);
        ArrayBlockingQueue<PArray<T2>> queue2 = new ArrayBlockingQueue<>(1);
        ArrayBlockingQueue<PArray<T3>> queue3 = new ArrayBlockingQueue<>(1);

        pipeline.stages.add(new IntermediatePipelineStage<>(stage0, pipeline.inputQueue, queue1));
        pipeline.stages.add(new IntermediatePipelineStage<>(stage1, queue1, queue2));
        pipeline.stages.add(new IntermediatePipelineStage<>(stage2, queue2, queue3));
        pipeline.stages.add(new LastPipelineStage<>(stage3, queue3));
        return pipeline;
    }

    @Override
    public PArray<outT> apply(PArray<inT> input) {
        if (!preparedExecutionFinish) {
            prepareExecution(input);
        }

        try {
            // this catches the case, where the input size is smaller than the chunkSize
            int chunkSizeLocal = Math.min(this.chunkSize, input.size());

            ArrayList<Thread> threads = new ArrayList<>(stages.size() + 1);

            // producer pushing results into the first stage in its own thread ...
            threads.add(new Thread(() -> {
                try {
                    PArray<inT>[] inputSlices = input.splitInChunksOfSize(chunkSizeLocal);
                    for (PArray<inT> inputSlice : inputSlices) {
                        inputQueue.put(inputSlice);
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }));
            // ... and one thread per stage
            for (Runnable s : stages) {
                threads.add(new Thread(s));
            }

            // launch all threads
            for (Thread t : threads) {
                t.start();
            }

            // wait for the last thread to finish
            threads.get(threads.size() - 1).join();

            // interrupt and join all (other) threads
            for (Thread t : threads) {
                t.interrupt();
                t.join();
            }

            return output;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if all stages already know their types.
     *
     * @return boolean
     */
    private boolean stagesKnowTheirTypes() {
        for (AbstractPipelineStage<?, ?> stage : stages) {
            if (!stage.doesKnowTypes()) {
                return false;
            }
        }
        return true;
    }

    /**
     * It allocates intermediate buffers of all stages.
     *
     * @param chunkSizeLocal
     * @param mode
     */
    private void allocateIntermediateBuffers(int chunkSizeLocal, StorageMode mode) {
        for (AbstractPipelineStage<?, ?> stage : stages) {
            stage.allocate(chunkSizeLocal, mode);
        }
    }

    @SuppressWarnings("unchecked")
    private LastPipelineStage<?, outT> getLastStage() {
        return (LastPipelineStage<?, outT>) stages.get(stages.size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PArray<outT> prepareExecution(PArray<inT> input) {
        PArray<?> tmp = input;
        for (AbstractPipelineStage<?, ?> stage : stages) {
            tmp = stage.prepareExecution(tmp);
        }

        // infer the types of the stages, if the stages do not already knows their type
        if (!stagesKnowTheirTypes()) {
            inferTypes(input);
        }

        // TODO: this assumes that input has already the proper size!
        if (output == null) {
            // TODO: this assumes the input and output sizes are the same ...
            output = new PArray<>(input.size(), getLastStage().getOutputType());
        }

        // this catches the case, where the input size is smaller than the chunkSize
        int chunkSizeLocal = Math.min(this.chunkSize, input.size());

        // allocate all intermediate buffers
        allocateIntermediateBuffers(chunkSizeLocal, input.getStorageMode());

        // get last stage and set output slices to write to
        getLastStage().setOutputChunks(output.splitInChunksOfSize(chunkSizeLocal));

        preparedExecutionFinish = true;
        return (PArray<outT>) tmp;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PArray<outT> inferTypes(PArray<inT> input) {
        PArray<?> tmp = input;
        for (AbstractPipelineStage<?, ?> stage : stages) {
            tmp = stage.inferTypes(tmp);
        }
        return (PArray<outT>) tmp;
    }

    @Override
    public void setOutput(PArray<outT> output) {
        this.output = output;
    }

    @Override
    public boolean isInCache() {
        return false;
    }
}
