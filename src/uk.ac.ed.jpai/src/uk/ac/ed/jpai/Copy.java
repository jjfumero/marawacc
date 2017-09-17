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

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;

/**
 * This is a dummy implementation that simulates Java marshalling. We allocate an output array which
 * uses our (good) default storage design and copy all elements over
 *
 */
public class Copy<T> extends Identity<T> {

    protected PArray<T> output;
    protected StorageMode targetStorageMode;
    protected Operation operation;

    public enum Operation {
        MARSHAL,
        UNMARSHAL,
        PARRAY
    }

    public Copy() {
        this(StorageMode.DEFAULT);
    }

    public Copy(StorageMode targetStorageMode) {
        this.targetStorageMode = targetStorageMode;
        // By default is in the PArray format, unless the operation comes from Marshal
        // operations.
        this.operation = Operation.PARRAY;
    }

    protected Copy(StorageMode targetStorageMode, Operation operation) {
        this.targetStorageMode = targetStorageMode;
        this.operation = operation;
    }

    @Override
    public PArray<T> apply(PArray<T> input) {

        // skip if the input uses already the optimal storage design
        if (operation == Operation.PARRAY) {
            return input;
        }

        long begin = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            if (targetStorageMode == StorageMode.DEFAULT) {
                if (operation == Operation.MARSHAL) {
                    Profiler.getInstance().writeInBuffer(ProfilerType.MARSHAL, "begin", begin);
                } else {
                    Profiler.getInstance().writeInBuffer(ProfilerType.UNMARSHAL, "begin", begin);
                }
            }
        }

        if (!preparedExecutionFinish) {
            prepareExecution(input);
        }

        if (output == null) {
            output = allocateOutputArray(input.size(), input.getStorageMode());
        }

        // Marshal - data type transformation
        for (int i = 0; i < input.size(); ++i) {
            output.put(i, input.get(i));
        }

        long end = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            if (targetStorageMode == StorageMode.DEFAULT) {
                if (operation == Operation.MARSHAL) {
                    Profiler.getInstance().writeInBuffer(ProfilerType.MARSHAL, "end", end);
                    Profiler.getInstance().put(ProfilerType.MARSHAL, end - begin);

                    Profiler.getInstance().writeInBuffer(ProfilerType.MARSHAL, "total", (end - begin));

                } else {
                    Profiler.getInstance().writeInBuffer(ProfilerType.UNMARSHAL, "end", end);
                    Profiler.getInstance().put(ProfilerType.UNMARSHAL, end - begin);

                    Profiler.getInstance().writeInBuffer(ProfilerType.UNMARSHAL, "total", (end - begin));
                }
            }
        }
        return output;
    }

    @Override
    public void setOutput(PArray<T> output) {
        this.output = output;
    }

    @Override
    public PArray<T> allocateOutputArray(int size, StorageMode mode) {
        return new PArray<>(size, outputType, mode, true);
    }
}
