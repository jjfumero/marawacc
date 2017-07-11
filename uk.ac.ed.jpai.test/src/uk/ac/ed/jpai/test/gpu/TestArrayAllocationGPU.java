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

public class TestArrayAllocationGPU extends MarawaccOpenCLTestBase {

    private static float sum(float[] array) {
        // do some computation
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    @Test
    public void test01() {
        ArrayFunction<Float, Float> mult = new MapAccelerator<>(f -> {
            float[] array = new float[10];
            for (int i = 0; i < array.length; i++) {
                array[i] = f + (i * 10);
            }
            return sum(array);
        });

        int size = 10;

        PArray<Float> input = new PArray<>(size, TypeFactory.Float());
        for (int i = 0; i < size; ++i) {
            input.put(i, (float) i);
        }
        PArray<Float> output = mult.apply(input);

        // Check with sequential
        float[] seq = new float[10];
        for (int i = 0; i < seq.length; i++) {
            float[] array = new float[10];
            for (int j = 0; j < array.length; j++) {
                array[j] = i + (j * 10);
            }
            seq[i] = sum(array);
        }

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(seq[i], output.get(i), 0.01);
        }
    }
}
