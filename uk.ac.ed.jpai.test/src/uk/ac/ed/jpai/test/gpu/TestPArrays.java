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

import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestPArrays extends MarawaccOpenCLTestBase {

    @Test
    public void testScopeIntArray() {

        int size = 10;
        final PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        int[] a = new int[]{1, 2, 3};
        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * 2.0 * a[1]);

        PArray<Double> output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2.0 * a[1], output.get(i), 0.001);
        }
    }

    @Test
    public void testScopePArray01() {

        int size = 10;
        final PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * 2.0 * input.get(9));

        PArray<Double> output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2.0 * input.get(9), output.get(i), 0.001);
        }
    }

    private static Double[] compute(Integer[] input, Function<Integer, Double> f) {
        Double[] result = new Double[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = f.apply(input[i]);
        }
        return result;
    }

    @Test
    public void testScopePArray02() {

        int size = 10;
        final PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> {
            double sum = 0.0;
            for (int i = 0; i < 10; i++) {
                sum += x + input.get(i);
            }
            return sum;
        });

        PArray<Double> output = mapTimesTwo.apply(input);

        Integer[] check = new Integer[10];
        IntStream.range(0, check.length).forEach(x -> check[x] = x);

        Function<Integer, Double> f = (x) -> {
            double sum = 0.0;
            for (int i = 0; i < 10; i++) {
                sum += x + input.get(i);
            }
            return sum;
        };

        Double[] res = compute(check, f);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(res[i], output.get(i), 0.001);
        }
    }

    @Test
    public void testScopePArray03() {

        int size = 10;
        final PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> x * 2.0 * input.size());

        PArray<Double> output = mapTimesTwo.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2.0 * input.size(), output.get(i), 0.001);
        }
    }

    @Test
    public void testScopePArray04() {

        int size = 10;
        final PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        ArrayFunction<Integer, Double> mapTimesTwo = new MapAccelerator<>(x -> {
            double sum = 0.0;
            for (int i = 0; i < input.size(); i++) {
                sum += x + input.get(i);
            }
            return sum;
        });

        PArray<Double> output = mapTimesTwo.apply(input);

        Integer[] check = new Integer[10];
        IntStream.range(0, check.length).forEach(x -> check[x] = x);

        Function<Integer, Double> f = (x) -> {
            double sum = 0.0;
            for (int i = 0; i < 10; i++) {
                sum += x + input.get(i);
            }
            return sum;
        };

        Double[] res = compute(check, f);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(res[i], output.get(i), 0.001);
        }
    }

    @Test
    public void testScopePArray05() {

        int size = 10;
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, new Tuple2<>(0.0f, 0.0f).getType());
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        ArrayFunction<Tuple2<Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2());

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2()), output.get(i), 0.000001);
        }
    }

    @Test
    @Ignore
    public void testScopePArray06() {

        int size = 10;
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, new Tuple2<>(0.0f, 0.0f).getType());
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        ArrayFunction<Tuple2<Float, Float>, Float> mult = new MapAccelerator<>(t -> t._1() * t._2() + input.get(0)._1());

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2() + input.get(0)._1()), output.get(i), 0.000001);
        }
    }
}
