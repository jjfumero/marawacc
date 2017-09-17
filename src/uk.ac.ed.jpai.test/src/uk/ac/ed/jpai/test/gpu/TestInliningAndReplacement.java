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

package uk.ac.ed.jpai.test.gpu;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ed.accelerator.math.ocl.OCLMath;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

/**
 * Test to check the in-lining and the OCL replacements in the compiler.
 */
public class TestInliningAndReplacement extends MarawaccOpenCLTestBase {

    private static float compute(float x) {
        return OCLMath.sqrt(x);
    }

    private static double compute(double x) {
        return OCLMath.sqrt(x);
    }

    private static float computeExp(float x) {
        return OCLMath.exp(x);
    }

    private static float computeAbs(float x) {
        return OCLMath.fabs(x);
    }

    private static float computeLog(float x) {
        return OCLMath.log(x);
    }

    @Test
    public void testFloatSqrt() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * compute(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Float> output = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.sqrt(i), output.get(i), 0.001);
        }
    }

    @Test
    public void testDoubleSqrt() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * compute((double) x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Double> output = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.sqrt(i), output.get(i), 0.001);
        }
    }

    @Test
    @Ignore
    public void testFloatExp() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * computeExp(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Float> output = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.exp(i), output.get(i), 0.001);
        }
    }

    @Test
    public void testFloatExp2() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.exp(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Float> output = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.exp(i), output.get(i), 1.5);
        }
    }

    @Test
    public void testFloatAbs() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * computeAbs(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Float> output = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.fabs(i), output.get(i), 0.001);
        }
    }

    @Test
    @Ignore
    public void testFloatLog() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * computeLog(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Float> output = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.log(i), output.get(i), 0.001);
        }
    }
}
