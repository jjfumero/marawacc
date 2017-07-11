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
import uk.ac.ed.datastructures.common.PArray.StorageMode;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.CopyToDevice;
import uk.ac.ed.jpai.CopyToHost;
import uk.ac.ed.jpai.Marawacc;
import uk.ac.ed.jpai.OpenCLMap;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestGPUComposition extends MarawaccOpenCLTestBase {

    @Test
    public void testComposition01() {

        CopyToDevice<Float> copyIn = new CopyToDevice<>();
        ArrayFunction<Float, Double> function = new OpenCLMap<>(x -> x * 2.0);
        CopyToHost<Double> copyToHost = new CopyToHost<>();

        ArrayFunction<Float, Double> composition = copyIn.andThen(function).andThen(copyToHost);

        int size = 10;

        PArray<Float> input = new PArray<>(size, TypeFactory.Float(), StorageMode.OPENCL_BYTE_BUFFER);
        PArray<Double> output;
        for (int i = 0; i < size; ++i) {
            input.put(i, (float) i + 1);
        }

        output = composition.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2.0, output.get(i), 0.001);
        }
    }

    @Test
    public void testComposition02() {

        ArrayFunction<Float, Double> composition = Marawacc.map(x -> x * 2.0);

        int size = 10;

        PArray<Float> input = new PArray<>(size, TypeFactory.Float(), StorageMode.OPENCL_BYTE_BUFFER);
        PArray<Double> output;
        for (int i = 0; i < size; ++i) {
            input.put(i, (float) i + 1);
        }

        output = composition.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2.0, output.get(i), 0.001);
        }
    }

}
