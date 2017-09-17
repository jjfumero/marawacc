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

import java.util.function.Function;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.MapArrayFunction;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestKernelWithInterfaces extends MarawaccOpenCLTestBase {

    public static class MyFunction implements Function<Integer, Float> {
        @Override
        public Float apply(Integer input) {
            return (float) input;
        }
    }

    private static final RuntimeObjectTypeInfo INTEGER = TypeFactory.Integer();

    public static class MyComplexFunction implements Function<Integer, Float> {
        @Override
        public Float apply(Integer input) {
            float sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += input * 2.0f;
            }
            return sum;
        }
    }

    @Test
    public void testMapInterfaceThreads() {

        ArrayFunction<Integer, Float> mapToFloat = new MapArrayFunction<>(new MyFunction());

        int size = 10;

        PArray<Integer> input = new PArray<>(size, INTEGER);

        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        PArray<Float> output = mapToFloat.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i), output.get(i), 0.0001);
        }
    }

    @Test
    public void testMapInterfaceGPU() {

        ArrayFunction<Integer, Float> mapToFloat = new MapAccelerator<>(new MyFunction());

        int size = 10;

        PArray<Integer> input = new PArray<>(size, INTEGER);

        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        PArray<Float> output = mapToFloat.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i), output.get(i), 0.0001);
        }
    }

    @Test
    public void testMapInterfaceGPU2() {

        ArrayFunction<Integer, Float> mapToFloat = new MapAccelerator<>(new MyComplexFunction());

        int size = 10;

        PArray<Integer> input = new PArray<>(size, INTEGER);

        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        PArray<Float> output = mapToFloat.apply(input);

        float[] expected = new float[size];

        for (int k = 0; k < size; k++) {
            float sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += input.get(k) * 2.0f;
            }
            expected[k] = sum;
        }

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(expected[i], output.get(i), 0.0001);
        }
    }
}
