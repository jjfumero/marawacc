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
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;
import uk.ac.ed.datastructures.tuples.Tuple6;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestTypeTuplesFactory extends MarawaccOpenCLTestBase {

    @Test
    public void test01() {
        ArrayFunction<Tuple2<Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2());

        int size = 10;

        PArray<Tuple2<Float, Float>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Float, Float>"));

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2()), output.get(i), 0.000001);
        }
    }

    @Test
    public void test02() {
        ArrayFunction<Tuple2<Integer, Long>, Long> mult = new MapAccelerator<>(t -> t._1() * t._2());

        int size = 10;

        PArray<Tuple2<Integer, Long>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Integer, Long>"));

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>(i, (long) i + 2));
        }

        PArray<Long> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2()), output.get(i), 0.000001);
        }

    }

    @Test
    public void test03() {
        ArrayFunction<Tuple3<Integer, Long, Long>, Long> mult = new MapAccelerator<>(t -> t._1() * t._2() * t._3());

        int size = 10;

        PArray<Tuple3<Integer, Long, Long>> input = new PArray<>(size, TypeFactory.Tuple("Tuple3<Integer, Long, Long>"));

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple3<>(i, (long) i + 2, (long) i + 2));
        }

        PArray<Long> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() * input.get(i)._3()), output.get(i), 0.000001);
        }

    }

    @Test
    public void test04() {
        ArrayFunction<Tuple6<Double, Double, Double, Double, Double, Double>, Double> function = new MapAccelerator<>(t -> t._1() + t._2() + t._3() + t._4() + t._5() + t._6());

        int size = 10;

        PArray<Tuple6<Double, Double, Double, Double, Double, Double>> input = new PArray<>(size, TypeFactory.Tuple("Tuple6<Double, Double, Double, Double, Double, Double>"));

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple6<>(i + 1.1, i + 2.0, i + 4.0, i + 5.0, i + 6.0, i + 100.0));
        }

        PArray<Double> output = function.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() + input.get(i)._2() + input.get(i)._3() + input.get(i)._4() + input.get(i)._5() + input.get(i)._6()), output.get(i), 0.000001);
        }

    }
}
