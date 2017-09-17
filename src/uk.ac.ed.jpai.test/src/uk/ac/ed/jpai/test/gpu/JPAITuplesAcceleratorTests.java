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

import java.util.Arrays;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;
import uk.ac.ed.datastructures.tuples.Tuple4;
import uk.ac.ed.datastructures.tuples.Tuple5;
import uk.ac.ed.datastructures.tuples.Tuple6;
import uk.ac.ed.datastructures.tuples.Tuple7;
import uk.ac.ed.datastructures.tuples.Tuple8;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class JPAITuplesAcceleratorTests extends MarawaccOpenCLTestBase {

    private static int size = 10;

    @Test
    public void testMapWithTuplesT2() {

        ArrayFunction<Tuple2<Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1 * t._2);
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Float,Float>"));

        Tuple2<Float, Float> tuple2 = new Tuple2<>();
        for (int i = 0; i < size; ++i) {
            tuple2._1((float) i);
            tuple2._2((float) i + 2);
            input.put(i, tuple2);
        }
        tuple2 = null;

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2()), output.get(i), 0.000001);
        }
    }

    @Test
    public void testMapWithTuplesT2T2() {

        ArrayFunction<Tuple2<Float, Float>, Tuple2<Float, Float>> mult = new MapAccelerator<>(t -> {
            Tuple2<Float, Float> tn = new Tuple2<>();
            tn._1 = t._1() * t._2();
            tn._2 = t._1() + t._2();
            return tn;
        });
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Float,Float>"));

        Tuple2<Float, Float> tuple2 = new Tuple2<>();
        for (int i = 0; i < size; ++i) {
            tuple2._1((float) i);
            tuple2._2((float) i + 2);
            input.put(i, tuple2);
        }
        tuple2 = null;

        PArray<Tuple2<Float, Float>> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2()), output.get(i)._1, 0.000001);
            assertEquals((input.get(i)._1() + input.get(i)._2()), output.get(i)._2, 0.000001);
        }
    }

    @Test
    public void testMapWithTuplesT3() {

        ArrayFunction<Tuple3<Float, Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2() + t._3());
        PArray<Tuple3<Float, Float, Float>> input = new PArray<>(size, new Tuple3<>(0.0f, 0.0f, 0.0f).getType());

        Tuple3<Float, Float, Float> tuple3 = new Tuple3<>();
        for (int i = 0; i < size; ++i) {
            tuple3._1((float) i);
            tuple3._2((float) i + 2);
            tuple3._3((float) i + 3);
            input.put(i, tuple3);
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() + input.get(i)._3), output.get(i), 0.000001);
        }
    }

    @Test
    public void testMapWithTuplesT4() {

        ArrayFunction<Tuple4<Float, Float, Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2() + t._3() + t._4());
        PArray<Tuple4<Float, Float, Float, Float>> input = new PArray<>(size, new Tuple4<>(0.0f, 0.0f, 0.0f, 0.0f).getType());

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple4<>((float) i, (float) i + 2, (float) i + 3, (float) i + 4));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() + input.get(i)._3 + input.get(i)._4), output.get(i), 0.000001);
        }
    }

    @Test
    public void testMapWithTuplesT5() {

        ArrayFunction<Tuple5<Float, Float, Float, Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2() + t._3() + t._4() + t._5());
        PArray<Tuple5<Float, Float, Float, Float, Float>> input = new PArray<>(size, new Tuple5<>(0.0f, 0.0f, 0.0f, 0.0f, 0.0f).getType());

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple5<>((float) i, (float) i + 2, (float) i + 3, (float) i + 4, (float) i + 5));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() + input.get(i)._3 + input.get(i)._4 + input.get(i)._5), output.get(i), 0.000001);
        }
    }

    @Test
    public void testMapWithTuplesT6() {

        ArrayFunction<Tuple6<Float, Float, Float, Float, Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2() * t._3() * t._4() * t._5() * t._6());
        PArray<Tuple6<Float, Float, Float, Float, Float, Float>> input = new PArray<>(size, new Tuple6<>(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f).getType());

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple6<>((float) i, (float) i + 2, (float) i + 3, (float) i + 4, (float) i + 5, (float) i + 6));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() * input.get(i)._3() * input.get(i)._4() * input.get(i)._5() * input.get(i)._6()), output.get(i), 0.000001);
        }
    }

    @Test
    public void testMapWithTuplesT7() {

        ArrayFunction<Tuple7<Float, Float, Float, Float, Float, Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2() * t._3() * t._4() * t._5() * t._6() * t._7());
        PArray<Tuple7<Float, Float, Float, Float, Float, Float, Float>> input = new PArray<>(size, new Tuple7<>(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f).getType());

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple7<>((float) i, (float) i + 2, (float) i + 3, (float) i + 4, (float) i + 5, (float) i + 6, (float) i + 7));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() * input.get(i)._3() * input.get(i)._4() * input.get(i)._5() * input.get(i)._6() * input.get(i)._7()), output.get(i), 0.000001);
        }
    }

    @Test
    public void testMapWithTuplesT8() {

        ArrayFunction<Tuple8<Float, Float, Float, Float, Float, Float, Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2() * t._3() * t._4() * t._5() * t._6() * t._7() * t._8());
        PArray<Tuple8<Float, Float, Float, Float, Float, Float, Float, Float>> input = new PArray<>(size, new Tuple8<>(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f).getType());

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple8<>((float) i, (float) i + 2, (float) i + 3, (float) i + 4, (float) i + 5, (float) i + 6, (float) i + 7, (float) i + 8));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() * input.get(i)._3() * input.get(i)._4() * input.get(i)._5() * input.get(i)._6() * input.get(i)._7() * input.get(i)._8()),
                            output.get(i), 0.000001);
        }
    }

    @Test
    public void testSaxpyOCL() {

        ArrayFunction<Tuple2<Float, Float>, Float> mult = new MapAccelerator<>(t -> 2.5f * t._1() + t._2());
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, new Tuple2<>(0.0f, 0.0f).getType());

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        mult.prepareExecution(input);

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((2.5f * input.get(i)._1() + input.get(i)._2()), output.get(i), 0.000001);
        }
    }

    @Test
    public void testDotProductOCL() {

        ArrayFunction<Tuple2<Float, Float>, Float> product = new MapAccelerator<>(t -> t._1() * t._2());
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, new Tuple2<>(0.0f, 0.0f).getType());

        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        PArray<Float> output = product.reduce((x, y) -> x + y, 0.0f).apply(input);

        // Reduction on CPU
        float acc = 0.0f;
        for (int i = 0; i < input.size(); ++i) {
            acc += input.get(i)._1 * input.get(i)._2;
        }

        assertEquals(acc, output.get(0), 0.0001);

    }

    @Test
    public void testMapWithTuplesAndLoops() {

        int[] a = new int[1000];
        Arrays.fill(a, 1);
        int[] c = new int[10];
        ArrayFunction<Integer, Integer> someComputation = new MapAccelerator<>(i -> {
            int val = c[i];
            for (int j = 0; j < 100; j++) {
                val = val + (a[j + i * j] * 2);
            }
            return val;
        });

        PArray<Integer> input = new PArray<>(size, new RuntimeObjectTypeInfo(Integer.class));

        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        PArray<Integer> output = someComputation.apply(input);

        int[] result = new int[10];
        result = Arrays.copyOfRange(c, 0, c.length);

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 100; j++) {
                result[i] = result[i] + (a[j + i * j] * 2);
            }
        }

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(result[i], output.get(i), 0.000001);
        }
    }
}
