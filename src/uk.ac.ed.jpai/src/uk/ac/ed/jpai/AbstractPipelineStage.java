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
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;

public abstract class AbstractPipelineStage<inT, outT> implements Runnable {

    protected ArrayFunction<inT, outT> arrayFunction;
    protected BlockingQueue<PArray<inT>> inputQueue;

    protected AbstractPipelineStage(ArrayFunction<inT, outT> f, BlockingQueue<PArray<inT>> inputQueue) {
        this.arrayFunction = f;
        this.inputQueue = inputQueue;
    }

    @SuppressWarnings("unchecked")
    public PArray<?> prepareExecution(PArray<?> input) {
        return arrayFunction.prepareExecution((PArray<inT>) input);
    }

    @SuppressWarnings("unchecked")
    public PArray<?> inferTypes(PArray<?> input) {
        return arrayFunction.inferTypes((PArray<inT>) input);
    }

    public boolean doesKnowTypes() {
        return (getInputType() != null && getOutputType() != null);
    }

    public RuntimeObjectTypeInfo getInputType() {
        return arrayFunction.getInputType();
    }

    public RuntimeObjectTypeInfo getOutputType() {
        return arrayFunction.getOutputType();
    }

    public abstract void allocate(int chunkSize, StorageMode mode);
}
