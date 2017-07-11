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

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestReusability extends MarawaccOpenCLTestBase {

    private static final int SIZE = 100;

    @Test
    public void testMapOCL() {

        ArrayFunction<Double, Double> f1 = new MapAccelerator<>(x -> x + 100);
        ArrayFunction<Double, Double> f2 = new MapAccelerator<>(x -> x * 100);

        PArray<Double> input = new PArray<>(SIZE, TypeFactory.Double());
        PArray<Double> output = new PArray<>(SIZE, TypeFactory.Double());
        for (int i = 0; i < SIZE; ++i) {
            input.put(i, (double) i);
        }

        output = f1.apply(input);
        PArray<Double> o2 = f1.andThen(f2).apply(input);

        assertNotNull(output);
        assertNotNull(o2);
    }
}