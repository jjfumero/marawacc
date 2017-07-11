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

import static org.junit.Assert.assertNotNull;

import java.util.Random;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestGPUIfNode extends MarawaccOpenCLTestBase {

    @Test
    public void testIf01() {
        // Basic algorithm (one core on the GPU)
        // Test with 1 GPU Thread: not efficient, just a test

        ArrayFunction<Integer, Integer> mapTimesTwo = new MapAccelerator<>(x -> {
            int n = x;
            if (n % 2 == 0) {
                n += 2;
            } else {
                n *= 10;
            }
            return n;
        });

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Integer> output = new PArray<>(size, TypeFactory.Integer());

        for (int i = 0; i < 10; i++) {
            input.put(i, i);
        }

        output = mapTimesTwo.apply(input);

        assertNotNull(output);
    }

    @Test
    public void testIf02() {
        ArrayFunction<Integer, Integer> ifFunction = new MapAccelerator<>(x -> {
            if (x >= 0) {
                return 1;
            } else {
                return -1;
            }
        });

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Integer> output = new PArray<>(size, TypeFactory.Integer());

        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            input.put(i, r.nextInt(1000));
        }

        output = ifFunction.apply(input);

        assertNotNull(output);
    }

}