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

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.jpai.cache.UserFunctionCache;

public class ArrayFunctionComposition<T0, T1, T2> extends ArrayFunction<T0, T2> {

    private ArrayFunction<T0, T1> arrayFunction0;
    private ArrayFunction<T1, T2> arrayFunction1;

    public ArrayFunctionComposition(ArrayFunction<T0, T1> f0, ArrayFunction<T1, T2> f1) {
        this.arrayFunction0 = f0;
        this.arrayFunction1 = f1;
    }

    @Override
    public PArray<T2> apply(PArray<T0> input) {
        if (!preparedExecutionFinish) {
            prepareExecution(input);
        }

        return arrayFunction1.apply(arrayFunction0.apply(input));
    }

    @Override
    public PArray<T2> prepareExecution(PArray<T0> input) {
        PArray<T2> output = arrayFunction1.prepareExecution(arrayFunction0.prepareExecution(input));

        inputType = arrayFunction0.getInputType();
        outputType = arrayFunction1.getOutputType();
        if (inputType == null || outputType == null) {
            inferTypes(input);
        }
        // assert (inputType != null);
        // assert (outputType != null);

        preparedExecutionFinish = true;

        return output;
    }

    @Override
    public PArray<T2> inferTypes(PArray<T0> input) {
        PArray<T2> output = arrayFunction1.inferTypes(arrayFunction0.inferTypes(input));

        inputType = arrayFunction0.getInputType();
        outputType = arrayFunction1.getOutputType();
        // assert (inputType != null);
        // assert (outputType != null);

        return output;
    }

    @Override
    public void setOutput(PArray<T2> output) {
        arrayFunction1.setOutput(output);
    }

    @Override
    public boolean isInCache() {
        if (UserFunctionCache.INSTANCE.isFunction(arrayFunction0) && (UserFunctionCache.INSTANCE.isFunction(arrayFunction1))) {
            return true;
        }
        return false;
    }
}
