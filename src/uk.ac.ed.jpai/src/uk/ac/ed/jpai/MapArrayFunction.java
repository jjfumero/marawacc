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

import java.util.UUID;
import java.util.function.Function;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.cache.UserFunctionCache;

/**
 *
 * Base class for the parallel map skeleton.
 *
 * Java Array Programming Interface.
 *
 * @param <inT>
 * @param <outT>
 */
public class MapArrayFunction<inT, outT> extends ArrayFunction<inT, outT> {

    /**
     * Function to be executed. This is the lambda expression
     */
    protected Function<inT, outT> function;

    /**
     * PArray for the output data type inference.
     */
    protected PArray<outT> output;

    protected UUID uuidLambda;

    public MapArrayFunction(Function<inT, outT> function) {
        this.function = function;
        this.uuidLambda = UserFunctionCache.INSTANCE.insertFunction(function);
    }

    @Override
    public PArray<outT> apply(PArray<inT> input) {
        if (!preparedExecutionFinish) {
            prepareExecution(input);
        }

        if (output == null) {
            output = allocateOutputArray(input.size(), input.getStorageMode());
        }

        // Sequential apply
        for (int i = 0; i < input.size(); ++i) {
            output.put(i, function.apply(input.get(i)));
        }

        return output;
    }

    @Override
    public PArray<outT> prepareExecution(PArray<inT> input) {
        PArray<outT> out = inferTypes(input);
        preparedExecutionFinish = true;
        return out;
    }

    @Override
    public PArray<outT> inferTypes(PArray<inT> input) {
        inputType = TypeFactory.inferFromObject(input.get(0));
        outputType = TypeFactory.inferFromObject(function.apply(input.get(0)));
        return new PArray<>(1, outputType);
    }

    @Override
    public void setOutput(PArray<outT> output) {
        this.output = output;
    }

    @Override
    public boolean isInCache() {
        return UserFunctionCache.INSTANCE.isFunction(function);
    }
}
