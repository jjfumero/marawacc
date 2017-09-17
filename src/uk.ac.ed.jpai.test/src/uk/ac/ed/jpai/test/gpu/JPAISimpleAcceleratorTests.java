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
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.OpenCLMap;
import uk.ac.ed.jpai.CopyToDevice;
import uk.ac.ed.jpai.CopyToHost;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.Marawacc;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class JPAISimpleAcceleratorTests extends MarawaccOpenCLTestBase {

    private static int SIZE = 10;

    @Test
    public void testMapOCL() {

        ArrayFunction<Integer, Double> gpuFunction = new MapAccelerator<>(x -> x * 2.0);

        PArray<Integer> input = new PArray<>(SIZE, TypeFactory.Integer());
        PArray<Double> output = new PArray<>(SIZE, TypeFactory.Double());
        for (int i = 0; i < SIZE; ++i) {
            input.put(i, i);
        }

        output = gpuFunction.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2.0, output.get(i), 0.001);
        }
    }

    @Test
    public void testPrepareKernelMapOCL() {

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x + 2.0);

        PArray<Integer> input = new PArray<>(SIZE, TypeFactory.Integer());
        for (int i = 0; i < SIZE; ++i) {
            input.put(i, i);
        }

        mapTimesTwo.prepareExecution(input);

        PArray<Double> output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) + 2, output.get(i), 0.000001);
        }
    }

    @Test
    public void testDecomposeMapInTree() {

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        CopyToDevice<Integer> copyIn = new CopyToDevice<>();
        OpenCLMap<Integer, Double> compute = new OpenCLMap<>(x -> x * 2.0);
        CopyToHost<Double> copyOut = new CopyToHost<>();

        ArrayFunction<Integer, Double> func = copyIn.andThen(compute).andThen(copyOut);

        func.prepareExecution(input);

        PArray<Double> output = func.apply(input);

        // copyOut.apply(compute.apply(copyIn.apply(input)));

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2, output.get(i), 0.000001);
        }
    }

    @Test
    public void testMapScopeScalar() {

        PArray<Float> input = new PArray<>(SIZE, new RuntimeObjectTypeInfo(Float.class));

        for (int i = 0; i < SIZE; ++i) {
            input.put(i, (float) i);
        }

        final float ten = 10.0f;

        PArray<Float> output = Marawacc.<Float, Float> mapAccelerator(x -> x * ten).apply(input);

        assertNotNull(output);

        for (int i = 0; i < output.size(); i++) {
            assertEquals((input.get(i) * ten), output.get(i), 0.0001);
        }
    }

    @Test
    public void testMapScopeArray() throws Exception {

        PArray<Float> input = new PArray<>(SIZE, TypeFactory.Float());

        for (int i = 0; i < SIZE; ++i) {
            input.put(i, (float) i);
        }

        final float[] ten = new float[]{10.0f};

        PArray<Float> output = Marawacc.<Float, Float> mapAccelerator(x -> x * ten[0]).apply(input);

        assertNotNull(output);

        for (int i = 0; i < output.size(); i++) {
            assertEquals((input.get(i) * ten[0]), output.get(i), 0.0001);
        }
    }

    @Test
    @Ignore
    public void testOffloadPipeline() {
        CopyToDevice<Integer> copyIn = new CopyToDevice<>();
        OpenCLMap<Integer, Double> compute = new OpenCLMap<>(x -> x * 2.0);
        CopyToHost<Double> copyOut = new CopyToHost<>();

        ArrayFunction<Integer, Double> pipeline = Marawacc.<Integer, Integer, Double, Double> pipeline(2, copyIn, compute, copyOut);

        PArray<Integer> input = new PArray<>(SIZE, TypeFactory.Integer());

        PArray<Double> output = new PArray<>(SIZE, TypeFactory.Double());
        pipeline.setOutput(output);
        pipeline.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i) * 2.0), output.get(i), 0.0001);
        }
    }
}
