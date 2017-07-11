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

import uk.ac.ed.datastructures.common.ArraySlice;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;

public class LastPipelineStage<inT, outT> extends AbstractPipelineStage<inT, outT> {

    private ArraySlice<outT>[] outputChunks;

    public LastPipelineStage(ArrayFunction<inT, outT> f, BlockingQueue<PArray<inT>> inputQueue) {
        super(f, inputQueue);
    }

    public void setOutputChunks(ArraySlice<outT>[] outputChunks) {
        this.outputChunks = outputChunks;
    }

    @Override
    public void run() {
        try {
            for (ArraySlice<outT> outputChunk : outputChunks) {
                // 1. get chunk from prev stage
                PArray<inT> input = inputQueue.take();

                arrayFunction.setOutput(outputChunk);

                // 2. call f on chunk
                arrayFunction.apply(input);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void allocate(int chunkSize, StorageMode mode) {
        // empty
    }
}
