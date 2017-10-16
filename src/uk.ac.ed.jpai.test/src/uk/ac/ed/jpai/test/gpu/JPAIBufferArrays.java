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

public class JPAIBufferArrays extends MarawaccOpenCLTestBase {

    @Test
    public void testFloat() {

        PArray<Integer> input = new PArray<>(100, TypeFactory.Integer());
        for (int i = 0; i < input.size(); i++) {
            input.put(i, i);
        }

        ArrayFunction<Integer, Float> function = new MapAccelerator<>(i -> {
            return i * 2.0f;
        });

        PArray<Float> output = function.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(i * 2.0f, output.get(i), 0.001);
        }
    }

    @Test
    public void testDouble() {

        PArray<Integer> input = new PArray<>(100, TypeFactory.Integer());
        for (int i = 0; i < input.size(); i++) {
            input.put(i, i);
        }

        ArrayFunction<Integer, Double> function = new MapAccelerator<>(i -> {
            return i * 2.0;
        });

        PArray<Double> output = function.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(i * 2.0, output.get(i), 0.001);
        }
    }

}
