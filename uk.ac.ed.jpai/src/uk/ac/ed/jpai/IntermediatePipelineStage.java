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

import java.util.concurrent.BlockingQueue;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;

public class IntermediatePipelineStage<inT, outT> extends AbstractPipelineStage<inT, outT> {

    protected BlockingQueue<PArray<outT>> outputQueue;

    // this assumes, that the blocking queue has a capacity of 1:
    // one buffer can be used by the producer, one by the consumer, and one can be in the queue
    private static final int NUMBER_OF_BUFFERS = 3;
    private PArray<outT>[] buffers;

    public IntermediatePipelineStage(ArrayFunction<inT, outT> f, BlockingQueue<PArray<inT>> inputQueue, BlockingQueue<PArray<outT>> outputQueue) {
        super(f, inputQueue);
        this.outputQueue = outputQueue;
    }

    /**
     * This method allocates the intermediate buffers for this stage. It uses its input to infer the
     * type information necessary for the allocation
     */
    @SuppressWarnings("unchecked")
    @Override
    public void allocate(int chunkSize, StorageMode mode) {
        assert (getOutputType() != null);
        buffers = new PArray[NUMBER_OF_BUFFERS];
        for (int i = 0; i < NUMBER_OF_BUFFERS; ++i) {
            buffers[i] = arrayFunction.allocateOutputArray(chunkSize, mode);
        }
    }

    @Override
    public void run() {
        int whichBuffer = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 1. get chunk from prev stage
                PArray<inT> input = inputQueue.take();

                buffers[whichBuffer].clear();

                // 2. set output
                arrayFunction.setOutput(buffers[whichBuffer]);

                // 3. call apply
                buffers[whichBuffer] = arrayFunction.apply(input);

                // 4. pass chunks to next stage
                outputQueue.put(buffers[whichBuffer]);

                // 5. swap output buffers
                whichBuffer++;
                if (whichBuffer > NUMBER_OF_BUFFERS - 1) {
                    whichBuffer = 0;
                }
            } catch (InterruptedException ex) {
                return; // return from function == end thread execution
            }
        }
    }
}
