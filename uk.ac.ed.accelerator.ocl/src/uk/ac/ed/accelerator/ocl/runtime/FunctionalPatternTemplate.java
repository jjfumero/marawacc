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

package uk.ac.ed.accelerator.ocl.runtime;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

import uk.ac.ed.accelerator.common.ParallelSkeleton;

public class FunctionalPatternTemplate {

    @SuppressWarnings("rawtypes") private Function function;
    @SuppressWarnings("rawtypes") private BiFunction bifunction;

    @SuppressWarnings("unchecked")
    public <T> void lambdaComputation(T[] input, T[] output) {
        for (int i = 0; i < input.length; i++) {
            T aux = input[i];
            output[i] = (T) function.apply(aux);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void lambdaReduceComputation(T[] input, T[] output) {
        for (int i = 1; i < input.length; i++) {
            T aux = input[0];
            output[0] = (T) bifunction.apply(aux, input[i]);
            input[0] = output[0];
        }
    }

    /**
     * @param skeleton which is used to build the CFG for OCL Graal backend.
     * @return Method string name in order to get the Method class.
     */
    private static String getMethodName(ParallelSkeleton skeleton) {
        String methodName = null;
        switch (skeleton) {
            case MAP:
                methodName = "lambdaComputation";
                break;
            case REDUCE:
                methodName = "lambdaReduceComputation";
                break;
            default:
                break;
        }
        return methodName;
    }

    /**
     *
     * @param skeleton which is used to build the CFG for OCL Graal backend.
     * @return Method class
     */
    public static Method getMethod(ParallelSkeleton skeleton) {
        String methodName = getMethodName(skeleton);
        if (methodName == null) {
            return null;
        }

        Method method = null;
        for (Method m : FunctionalPatternTemplate.class.getMethods()) {
            if (m.getName().equals(methodName)) {
                method = m;
            }
        }
        return method;
    }
}
