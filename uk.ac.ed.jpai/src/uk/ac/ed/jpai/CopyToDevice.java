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

import org.jocl.CL;

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.datastructures.common.AcceleratorPArray;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;

public class CopyToDevice<T> extends Identity<T> {

    private AcceleratorPArray<T> acceleratorArray;
    private static int idxCounter = 0;

    @Override
    public PArray<T> apply(PArray<T> array) {

        long begin = System.nanoTime();
        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COPY_TO_DEVICE, "begin", begin);
        }

        if (!preparedExecutionFinish) {
            prepareExecution(array);
        }

        if (acceleratorArray == null) {
            acceleratorArray = allocateOutputArray(array.size(), array.getStorageMode());
        }
        // link up the ByteBuffer of the array with the cl_mem of acceleratorArray
        acceleratorArray.setArray(array);

        acceleratorArray.setSequence(array.isSequence());
        acceleratorArray.setTotalSize(array.getTotalSizeWhenSequence());

        acceleratorArray.allocateOpenCLBuffer(CL.CL_MEM_READ_ONLY);
        acceleratorArray.copyToDevice(idxCounter++);

        long end = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COPY_TO_DEVICE, "end", end);
            Profiler.getInstance().put(ProfilerType.COPY_TO_DEVICE, end - begin);
        }
        return acceleratorArray;
    }

    @Override
    public void setOutput(PArray<T> output) {
        this.acceleratorArray = (AcceleratorPArray<T>) output;
    }

    @Override
    public AcceleratorPArray<T> allocateOutputArray(int size, StorageMode mode) {
        // this output array has no Java array to store the data ...
        // ... in the apply method the input array will be combined with this array
        return new AcceleratorPArray<>(size, outputType, mode, true);
    }
}
