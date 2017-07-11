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

package uk.ac.ed.replacements.ocl;

import uk.ac.ed.replacements.ocl.OCLMathIntrinsicNode.Operation;

import com.oracle.graal.api.replacements.ClassSubstitution;
import com.oracle.graal.api.replacements.MethodSubstitution;

/**
 * Substitutions for {@link uk.ac.ed.accelerator.math.ocl.OCLMath} methods.
 */
@ClassSubstitution(uk.ac.ed.accelerator.math.ocl.OCLMath.class)
public class OCLMathSubstitutions {

    @MethodSubstitution
    public static float fabs(float x) {
        return OCLMathIntrinsicNode.compute(x, Operation.FABS);
    }

    @MethodSubstitution
    public static double fabs(double x) {
        return OCLMathIntrinsicNode.compute(x, Operation.FABS);
    }

    @MethodSubstitution
    public static float sqrt(float x) {
        return OCLMathIntrinsicNode.compute(x, Operation.SQRT);
    }

    @MethodSubstitution
    public static double sqrt(double x) {
        return OCLMathIntrinsicNode.compute(x, Operation.SQRT);
    }

    @MethodSubstitution
    public static float log(float x) {
        return OCLMathIntrinsicNode.compute(x, Operation.LOG);
    }

    @MethodSubstitution
    public static double log(double x) {
        return OCLMathIntrinsicNode.compute(x, Operation.LOG);
    }

    @MethodSubstitution
    public static float exp(float x) {
        return OCLMathIntrinsicNode.compute(x, Operation.EXP);
    }

    @MethodSubstitution
    public static double exp(double x) {
        return OCLMathIntrinsicNode.compute(x, Operation.EXP);
    }

    @MethodSubstitution
    public static float pow2(float x) {
        return OCLMathIntrinsicNode.compute(x, Operation.POW2);
    }

    @MethodSubstitution
    public static double pow2(double x) {
        return OCLMathIntrinsicNode.compute(x, Operation.POW2);
    }

    @MethodSubstitution
    public static float pow(float x, float y) {
        return OCLMathIntrinsicNode.compute(x, y, Operation.POW);
    }

    @MethodSubstitution
    public static double pow(double x, double y) {
        return OCLMathIntrinsicNode.compute(x, y, Operation.POW);
    }

    @MethodSubstitution
    public static float hypot(float x, float y) {
        return OCLMathIntrinsicNode.compute(x, y, Operation.HYPOT);
    }

    @MethodSubstitution
    public static double hypot(double x, double y) {
        return OCLMathIntrinsicNode.compute(x, y, Operation.HYPOT);
    }

}
