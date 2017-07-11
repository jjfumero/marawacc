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

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestGPUAllocation extends MarawaccOpenCLTestBase {

    private static double[] sequentialTestAllocation() {

        int[] a = new int[]{1, 2, 3};
        int size = 10;
        double[] result = new double[size];

        for (int i = 0; i < size; i++) {
            double r = i * 2.0 * a[1];
            int[] n = new int[size];
            for (int j = 0; j < size; j++) {
                n[i] = a[1];
            }

            for (int j = 0; j < size; j++) {
                r += n[i];
            }
            result[i] = r;
        }
        return result;
    }

    @Test
    public void testAllocation01() {

        final int size = 10;
        final PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        int[] a = new int[]{1, 2, 3};
        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> {
            double r = x * 2.0 * a[1];
            int[] n = new int[size];
            for (int i = 0; i < size; i++) {
                n[i] = a[1];
            }

            for (int i = 0; i < size; i++) {
                r += n[i];
            }
            return r;
        });

        PArray<Double> output = mapTimesTwo.apply(input);

        double[] expected = sequentialTestAllocation();

        for (int i = 0; i < size; i++) {
            assertEquals(expected[i], output.get(i), 0.01);
        }
    }
}