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
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.Marawacc;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class JPAI2DTests extends MarawaccOpenCLTestBase {

    @Test
    public void testFlat2D() {

        final int[] a = new int[1000];

        IntStream.range(0, a.length).sequential().forEach(i -> a[i] = i + 1);

        int[] expected = new int[10];

        // Java Sequential
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 100; j++) {
                expected[i] += (a[i + i * j] * 2);
            }
        }

        PArray<Integer> input = new PArray<>(10, TypeFactory.Integer());
        for (int i = 0; i < input.size(); i++) {
            input.put(i, i);
        }

        ArrayFunction<Integer, Integer> function = Marawacc.<Integer, Integer> mapAccelerator(i -> {
            int sum = 0;
            for (int j = 0; j < 100; j++) {
                sum += (a[i + i * j] * 2);
            }
            return sum;
        });

        PArray<Integer> output = function.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(expected[i], output.get(i), 0.1);
        }
    }

    @Test
    public void test() {
        int size = 10;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        int[] a = new int[size];

        ArrayFunction<Integer, Tuple2<Integer, Integer>> compute = new MapAccelerator<>(i -> {
            int ci = a[i];
            Tuple2<Integer, Integer> r = new Tuple2<>(i, ci);
            return r;
        });
        PArray<Tuple2<Integer, Integer>> output = compute.apply(input);

        assertNotNull(output);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(i, output.get(i)._1, 0.1);
            assertEquals(a[i], output.get(i)._2, 0.1);
        }

    }
}
