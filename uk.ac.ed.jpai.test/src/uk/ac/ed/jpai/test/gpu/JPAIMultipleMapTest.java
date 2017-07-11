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

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.Marawacc;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class JPAIMultipleMapTest extends MarawaccOpenCLTestBase {

    private static int SIZE = 10;

    @Test
    public void testMaps01() {

        // No kernel fussion
        ArrayFunction<Tuple2<Float, Float>, Float> mult = Marawacc.<Float, Float> zip2().mapAccelerator(t -> t._1() * t._2()).mapAccelerator(i -> i + 100);
        PArray<Tuple2<Float, Float>> input = new PArray<>(SIZE, new Tuple2<>(0.0f, 0.0f).getType());

        for (int i = 0; i < SIZE; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(((input.get(i)._1() * input.get(i)._2()) + 100), output.get(i), 0.000001);
        }
    }

    @Test
    public void testMaps02() {

        ArrayFunction<Tuple2<Float, Float>, Float> mult = Marawacc.<Float, Float> zip2().mapAccelerator(t -> t._1() * t._2());
        ArrayFunction<Float, Float> sum = new MapAccelerator<>(i -> i + 100);

        PArray<Tuple2<Float, Float>> input = new PArray<>(SIZE, new Tuple2<>(0.0f, 0.0f).getType());

        for (int i = 0; i < SIZE; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        PArray<Float> out = mult.apply(input);
        PArray<Float> output = sum.apply(out);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(((input.get(i)._1() * input.get(i)._2()) + 100), output.get(i), 0.000001);
        }
    }

    @Test
    @Ignore
    public void testMaps03() {

        // Kernel Fusion: this is just an idea - not implemented yet.
        ArrayFunction<Tuple2<Float, Float>, Float> mult = Marawacc.<Float, Float> zip2().mapAccelerator(t -> t._1() * t._2()).mapAccelerator(i -> i + 100);

        PArray<Tuple2<Float, Float>> input = new PArray<>(SIZE, new Tuple2<>(0.0f, 0.0f).getType());

        for (int i = 0; i < SIZE; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(((input.get(i)._1() * input.get(i)._2()) + 100), output.get(i), 0.000001);
        }
    }

}
