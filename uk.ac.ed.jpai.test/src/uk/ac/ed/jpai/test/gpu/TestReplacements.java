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

import org.junit.Test;

import uk.ac.ed.accelerator.math.ocl.OCLMath;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestReplacements extends MarawaccOpenCLTestBase {

    @Test
    public void testReplament1() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.sqrt((double) x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, new RuntimeObjectTypeInfo(Integer.class));
        PArray<Double> output = new PArray<>(size, new RuntimeObjectTypeInfo(Double.class));
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
    public void testReplament2() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.exp((double) x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, new RuntimeObjectTypeInfo(Integer.class));
        PArray<Double> output = new PArray<>(size, new RuntimeObjectTypeInfo(Double.class));
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.exp((double) i), output.get(i), 0.01);
        }
    }

    @Test
    public void testReplament3() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.fabs((double) x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, new RuntimeObjectTypeInfo(Integer.class));
        PArray<Double> output = new PArray<>(size, new RuntimeObjectTypeInfo(Double.class));
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.fabs(i), output.get(i), 0.01);
        }
    }

    @Test
    public void testReplament4() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.log((double) x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, new RuntimeObjectTypeInfo(Integer.class));
        PArray<Double> output = new PArray<>(size, new RuntimeObjectTypeInfo(Double.class));
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.log(i), output.get(i), 0.1);
        }
    }

    @Test
    public void testReplament5() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.sqrt(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, new RuntimeObjectTypeInfo(Integer.class));
        PArray<Float> output = new PArray<>(size, new RuntimeObjectTypeInfo(Float.class));
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.sqrt(i), output.get(i), 0.01);
        }
    }

    @Test
    public void testReplament6() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.exp(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, new RuntimeObjectTypeInfo(Integer.class));
        PArray<Float> output = new PArray<>(size, new RuntimeObjectTypeInfo(Float.class));
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.exp(i), output.get(i), 0.01);
        }
    }

    @Test
    public void testReplament7() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.fabs(x));

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
    public void testReplament8() {

        ArrayFunction<Integer, Float> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.log(x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Float> output = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.log(i), output.get(i), 0.1);
        }
    }

    @Test
    public void testReplament9() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.pow2((double) x));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Double> output = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.pow2(i), output.get(i), 0.001);
        }
    }

    @Test
    public void testReplament10() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.pow((double) x, 3));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Double> output = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.pow(i, 3), output.get(i), 0.001);
        }
    }

    @Test
    public void testReplament11() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * OCLMath.hypot((double) x, 3));

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Double> output = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * OCLMath.hypot(i, 3), output.get(i), 0.001);
        }

    }

    @Test
    public void testReplament12() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * Math.cos(x));
        int size = 10;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Double> output = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(i * Math.cos(i), output.get(i), 0.001);
        }

    }

    @Test
    public void testReplament13() {
        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * Math.sin(x));
        int size = 10;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Double> output = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.setOutput(output);

        output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(i * Math.sin(i), output.get(i), 0.001);
        }

    }
}
