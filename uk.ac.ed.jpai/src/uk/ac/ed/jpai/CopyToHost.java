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
import uk.ac.ed.datastructures.common.AcceleratorPArray;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;

public class CopyToHost<T> extends Identity<T> {

    private PArray<T> output = null;
    private static int idx;

    private void setPrimitiveArray(PArray<T> input, RuntimeObjectTypeInfo runtimeTypeInfo, int idx) {
        if (runtimeTypeInfo.getClassObject() == Integer.class) {
            output.setIntArray(idx, input.asIntegerArray(idx));
        } else if (runtimeTypeInfo.getClassObject() == Double.class) {
            output.setDoubleArray(idx, input.asDoubleArray(idx));
        } else if (PArray.TUPLESET.contains(runtimeTypeInfo.getClassObject())) {
            // Inspect nested types
            RuntimeObjectTypeInfo[] nestedTypes = runtimeTypeInfo.getNestedTypes();
            int i = idx;
            for (RuntimeObjectTypeInfo r : nestedTypes) {
                setPrimitiveArray(input, r, i);
                i++;
            }
        } else {
            throw new RuntimeException("Data type not supported yet: " + outputType.getClassObject());
        }
    }

    @Override
    public PArray<T> apply(PArray<T> input) {

        long begin = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COPY_TO_HOST, "begin", begin);
        }

        if (!preparedExecutionFinish) {
            prepareExecution(input);
        }

        if (output == null) {
            if (input.isPrimitiveArray()) {
                output = allocateOutputArray(input.size(), input.getStorageMode(), false);
                setPrimitiveArray(input, outputType, 0);
                output.setPrimitive(true);
            } else {
                output = allocateOutputArray(input.size(), input.getStorageMode());
            }
        }

        // link up the cl_mem from input with the ByteBuffer from output
        ((AcceleratorPArray<T>) input).setArray(output);
        ((AcceleratorPArray<T>) input).copyToHost(idx++);

        long end = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COPY_TO_HOST, "end", end);
            Profiler.getInstance().put(ProfilerType.COPY_TO_HOST, end - begin);
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

    public PArray<T> allocateOutputArray(int size, StorageMode mode, boolean doAllocation) {
        return new PArray<>(size, outputType, mode, doAllocation);
    }
}
