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

import java.util.stream.IntStream;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestScopeArrays extends MarawaccOpenCLTestBase {

    @Test
    public void scopeTest() {

        final int size = 100;

        int[] in = new int[size];

        IntStream.range(0, size).parallel().forEach(i -> {
            in[i] = 10;
        });

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        MapAccelerator<Integer, Integer> function = new MapAccelerator<>(idx -> {
            int r = in[idx] * idx;
            return r;
        });

        PArray<Integer> result = function.apply(input);

        assertNotNull(result);

        for (int i = 0; i < size; i++) {
            assertEquals(in[i] * i, result.get(i), 0.1);
        }
    }

    @Test
    public void scopeTest2() {

        final int size = 100;

        float[] in = new float[size];

        IntStream.range(0, size).parallel().forEach(i -> {
            in[i] = 10.0f;
        });

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        MapAccelerator<Integer, Float> function = new MapAccelerator<>(idx -> {
            float r = in[idx] * idx;
            return r;
        });

        PArray<Float> result = function.apply(input);

        assertNotNull(result);

        for (int i = 0; i < size; i++) {
            assertEquals(in[i] * i, result.get(i), 0.1);
        }
    }

    @Test
    public void scopeTest2_a() {

        final int size = 100;

        float[] in = new float[size];
        short[] in2 = new short[size];

        IntStream.range(0, size).parallel().forEach(i -> {
            in[i] = 10.0f;
        });

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        MapAccelerator<Integer, Float> function = new MapAccelerator<>(idx -> {
            float r = in[idx] * idx + in2[idx];
            return r;
        });

        PArray<Float> result = function.apply(input);

        assertNotNull(result);

        for (int i = 0; i < size; i++) {
            assertEquals(in[i] * i, result.get(i), 0.1);
        }
    }

    @Test
    public void scopeTest3() {

        final int size = 100;

        float[] in = new float[size];

        IntStream.range(0, size).parallel().forEach(i -> {
            in[i] = 10.0f;
        });

        PArray<Float> input = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; i++) {
            input.put(i, (float) i);
        }

        MapAccelerator<Float, Float> function = new MapAccelerator<>(idx -> {
            float r = in[10] * idx;
            return r;
        });

        PArray<Float> result = function.apply(input);

        assertNotNull(result);

        for (int i = 0; i < size; i++) {
            assertEquals(in[10] * i, result.get(i), 0.1);
        }
    }

    @Test
    public void scopeTest4() {

        final int size = 100;

        final float[] inreal = new float[size];

        IntStream.range(0, size).parallel().forEach(i -> {
            inreal[i] = 0.01f;
        });

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        MapAccelerator<Integer, Float> function = new MapAccelerator<>(idx -> {
            return (float) idx;
        });

        PArray<Float> result = function.apply(input);

        assertNotNull(result);

        for (int i = 0; i < size; i++) {
            assertEquals(i, result.get(i), 0.1);
        }

    }

}
