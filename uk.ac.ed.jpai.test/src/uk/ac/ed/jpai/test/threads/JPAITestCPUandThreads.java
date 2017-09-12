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

package uk.ac.ed.jpai.test.threads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ed.accelerator.math.ocl.OCLMath;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple4;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapArrayFunction;
import uk.ac.ed.jpai.MapJavaThreads;
import uk.ac.ed.jpai.Marawacc;
import uk.ac.ed.jpai.Reduce;
import uk.ac.ed.jpai.ReduceJavaThreads;

public class JPAITestCPUandThreads {

    @Test
    public void testMapAndPipeline() {

        ArrayFunction<Integer, Float> mapToFloat = new MapArrayFunction<>(i -> (float) i);
        ArrayFunction<Float, Double> mapTimesTwo = new MapArrayFunction<>(f -> f * 2.0);
        ArrayFunction<Double, Double> mapTimesThree = Marawacc.map(f -> f * 3.0);

        ArrayFunction<Integer, Double> pipe4 = Marawacc.pipeline(2, mapToFloat, mapTimesTwo, mapTimesThree);
        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());

        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        PArray<Double> output = pipe4.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 2 * 3, output.get(i), 0.0001);
        }
    }

    @Test
    public void testZipMap() {

        ArrayFunction<Tuple2<Float, Float>, Float> mult = Marawacc.<Float, Float> zip2().map(t -> t._1() * t._2());

        int size = 10;

        PArray<Tuple2<Float, Float>> input = new PArray<>(size, new Tuple2<>(0.0f, 0.0f).getType());
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((float) i, (float) i + 2));
        }

        PArray<Float> output = mult.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals((input.get(i)._1() * input.get(i)._2()), output.get(i), 0.0001);
        }
    }

    @Test
    public void testDotProduct() {
        // zip - map - reduce
        ArrayFunction<Tuple2<Float, Float>, Float> mult = Marawacc.<Float, Float> zip2().map(t -> t._1() * t._2());
        int size = 12;

        PArray<Tuple2<Float, Float>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Float, Float>"));
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>(i * 1.0f, i * 2.0f));
        }
        PArray<Float> output = mult.apply(input);

        PArray<Float> output2 = Marawacc.<Float, Float> zip2().map(t -> t._1() * t._2()).reduce((x, y) -> x + y, 0.0f).apply(input);

        Float reduction = 0.0f;
        for (int i = 0; i < output.size(); ++i) {
            reduction += output.get(i);
        }
        assertEquals(reduction, output2.get(0), 0.0001);
    }

    @Test
    public void testPipeline() {

        // zip and pipeline
        ArrayFunction<Float, Double> mapTimesTwo = new MapArrayFunction<>(f -> f * 2.0);

        ArrayFunction<Tuple2<Float, Float>, Double> p = Marawacc.<Float, Float> zip2().pipeline(2, Marawacc.map(t -> t._1() * t._2()), mapTimesTwo);

        int size = 10;
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, new Tuple2<>(0.0f, 0.0f).getType());

        PArray<Double> output3 = new PArray<>(size, TypeFactory.Double());
        p.setOutput(output3);
        p.apply(input);
        for (int i = 0; i < output3.size(); ++i) {
            assertEquals(((input.get(i)._1() * input.get(i)._2()) * 2.0), output3.get(i), 0.0001);
        }
    }

    @Ignore
    @Test
    public void testNestedPipeline() {
        // zip and nested pipeline
        ArrayFunction<Float, Double> mapTimesTwo = new MapArrayFunction<>(f -> f * 2.0);
        ArrayFunction<Double, Double> mapTimesThree = Marawacc.map(f -> f * 3.0);

        int size = 10;
        PArray<Tuple2<Float, Float>> input = new PArray<>(size, new Tuple2<>(0.0f, 0.0f).getType());

        ArrayFunction<Tuple2<Float, Float>, Double> p2 = Marawacc.<Float, Float> zip2().pipeline(4, Marawacc.map(t -> t._1() * t._2()), Marawacc.pipeline(2, mapTimesTwo, mapTimesThree));

        PArray<Double> output5 = new PArray<>(size, TypeFactory.Double());
        p2.setOutput(output5);
        p2.apply(input);

        for (int i = 0; i < output5.size(); ++i) {
            assertEquals(((input.get(i)._1() * input.get(i)._2())) * 2.0 * 3.0, output5.get(i), 0.0001);
        }
    }

    @Test
    public void testArraySlice() {
        int size = 10;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        PArray<Integer>[] slices = input.splitInFixedNumberOfChunks(3);
        assertEquals(slices.length, 3);
        assertEquals(3, slices[0].size());
        assertEquals(3, slices[1].size());
        assertEquals(4, slices[2].size());

        for (int i = 0; i < slices[0].size(); ++i) {
            assertEquals(i, (int) slices[0].get(i));
        }
        for (int i = 0; i < slices[1].size(); ++i) {
            assertEquals(i + 3, (int) slices[1].get(i));
        }
        for (int i = 0; i < slices[2].size(); ++i) {
            assertEquals(i + 6, (int) slices[2].get(i));
        }
    }

    @Test
    public void testArraySliceFixedChunkSize() {
        int size = 10;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        PArray<Integer>[] slices = input.splitInChunksOfSize(3);
        assertEquals(slices.length, 4);
        assertEquals(3, slices[0].size());
        assertEquals(3, slices[1].size());
        assertEquals(3, slices[2].size());
        assertEquals(1, slices[3].size());

        for (int i = 0; i < slices[0].size(); ++i) {
            assertEquals(i, (int) slices[0].get(i));
        }
        for (int i = 0; i < slices[1].size(); ++i) {
            assertEquals(i + 3, (int) slices[1].get(i));
        }
        for (int i = 0; i < slices[2].size(); ++i) {
            assertEquals(i + 6, (int) slices[2].get(i));
        }
        for (int i = 0; i < slices[3].size(); ++i) {
            assertEquals(i + 9, (int) slices[3].get(i));
        }
    }

    @Test
    public void testJavaThreadMap() {
        ArrayFunction<Double, Double> mapTimesThree = new MapJavaThreads<>(f -> f * 3.0);

        int size = 73;
        PArray<Double> input = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i * 2.5);
        }

        PArray<Double> output = mapTimesThree.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 3.0, output.get(i), 0.001);
        }
    }

    @Test
    public void testJavaThreadMap2() {
        ArrayFunction<Integer, Integer> mapTimesThree = new MapJavaThreads<>(i -> i + 1);

        int size = 73;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i + 10);
        }

        PArray<Integer> output = mapTimesThree.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) + 1, output.get(i), 0.001);
        }
    }

    @Test
    public void testCollections_ArrayList() {

        ArrayList<Integer> l = new ArrayList<>();
        int size = 73;
        for (int i = 0; i < size; i++) {
            l.add(i);
        }

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        // Only access using get allowed.
        ArrayFunction<Integer, Integer> mapTimesThree = new MapJavaThreads<>(i -> {
            return i * 3 + l.get(i);
        });

        PArray<Integer> output = mapTimesThree.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 3 + l.get(i), output.get(i), 0.1);
        }
    }

    @Test
    public void testCollections_ArrayList02() {

        ArrayList<Integer> l = new ArrayList<>();
        int size = 73;
        for (int i = 0; i < size; i++) {
            l.add(i);
        }

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        // Only access using get allowed.
        ArrayFunction<Integer, Integer> mapTimesThree = new MapJavaThreads<>(i -> {
            ArrayList<Integer> a = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                a.add(j);
            }
            int sum = 0;
            for (int j = 0; j < 100; j++) {
                sum += a.get(j);
            }
            return sum;
        });

        PArray<Integer> output = mapTimesThree.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(4950, output.get(i), 0.1);
        }
    }

    @Test
    public void testCollections_HashMap() {

        HashMap<Integer, Integer> l = new HashMap<>();
        int size = 73;
        for (int i = 0; i < size; i++) {
            l.put(i, i + 100);
        }

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, i);
        }

        // Only access using get allowed.
        ArrayFunction<Integer, Integer> mapTimesThree = new MapJavaThreads<>(i -> {
            return i * 3 + l.get(i);
        });

        PArray<Integer> output = mapTimesThree.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 3 + l.get(i), output.get(i), 0.1);
        }
    }

    @Test
    public void testJavaFixedThreadMap() {
        ArrayFunction<Double, Double> mapTimesThree = new MapJavaThreads<>(10, f -> f * 3.0);

        int size = 73;
        PArray<Double> input = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            input.put(i, i * 2.5);
        }

        PArray<Double> output = mapTimesThree.apply(input);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(input.get(i) * 3.0, output.get(i), 0.001);
        }
    }

    @Test
    public void geoMeanJPAI() {

        int size = 100;

        PArray<Float> input = new PArray<>(size, TypeFactory.Float());

        Float[] inputSeq = new Float[size];

        // Initialisation
        for (int i = 0; i < input.size(); i++) {
            input.put(i, 2.1f);
            inputSeq[i] = 2.1f;
        }

        PArray<Float> output = Marawacc.reduce((x, y) -> x * y, 1.0f).apply(input);

        output.put(0, (float) Math.pow(output.get(0), (1.0 / input.size())));

        float partialResult = 1;
        for (int i = 0; i < inputSeq.length; i++) {
            partialResult *= inputSeq[i];
        }
        float expected = (float) Math.pow(partialResult, (1.0 / inputSeq.length));

        float delta = 0.001f;
        assertNotNull(output);
        assertEquals(expected, output.get(0), delta);
    }

    @Test
    public void pi() {
        int size = 1000000;
        PArray<Long> input = new PArray<>(size, TypeFactory.Long());
        Random rand = new Random(12345);

        for (int i = 0; i < input.size(); i++) {
            input.put(i, rand.nextLong());
        }

        ArrayFunction<Long, Float> function = Marawacc.mapJavaThreads(4, z -> {
            // generate a pseudo random number (you do need it twice)
            long seed = z;
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);

            // this generates a number between -1 and 1 (with an awful entropy)
            float x = -1 + 2 * (seed & 0x0FFFFFFF) / 268435455f;

            // repeat for y
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            float y = -1 + 2 * (seed & 0x0FFFFFFF) / 268435455f;
            float compute = x * x + y * y;

            float result = 0.0f;
            if (compute < 1) {
                result = 1.0f;
            }
            return result;
        });
        PArray<Float> output = function.reduce((x, y) -> x + y, 0.0f).apply(input);
        assertNotNull(output);
        assertEquals(3.1416, output.get(0) * 4.0 / size, 0.1);
    }

    @Test
    @Ignore
    public void testNestedPArrays() {

        // Mandelbrot

        final int len = 10;
        int size = len;
        final int iterations = 100;

        // Initialization
        PArray<Tuple2<Float, Integer>> input = new PArray<>(size, new Tuple2<>(0.0f, 0).getType());
        for (int i = 0; i < input.size(); i++) {
            input.put(i, new Tuple2<>(2.0f / size, i));
        }

        @SuppressWarnings("unused")
        PArray<PArray<Short>> output = Marawacc.<Float, Integer> zip2().mapAccelerator(x -> {
            float space = x._1();
            int index = x._2();
            PArray<Short> rowResult2 = new PArray<>(len, TypeFactory.Short());

            for (int j = 0; j < len; j++) {
                float Zr = 0.0f;
                float Zi = 0.0f;
                float Cr = (1 * j * space - 1.5f);
                float Ci = (1 * index * space - 1.0f);

                float ZrN = 0;
                float ZiN = 0;
                int y = 0;

                for (y = 0; y < iterations && ZiN + ZrN <= 4.0f; y++) {
                    Zi = 2.0f * Zr * Zi + Ci;
                    Zr = 1 * ZrN - ZiN + Cr;
                    ZiN = Zi * Zi;
                    ZrN = Zr * Zr;
                }

                short value = (short) ((y * 255) / iterations);
                rowResult2.put(j, value);
            }
            return rowResult2;
        }).apply(input);
    }

    public static float f(float x) {
        return (float) ((float) Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI));
    }

    public static float integrateSeq(float a, float b, int N) {
        float h = (b - a) / N;
        float sum = 0.5f * (f(a) + f(b));

        for (int i = 1; i < N; i++) {
            float x = a + h * i;
            sum += f(x);
        }
        return sum * h;
    }

    @Test
    public void testIntegration() {

        final int N = 100;

        ArrayFunction<Tuple4<Float, Float, Integer, Integer>, Float> partialIntegration = new MapJavaThreads<>(x -> {
            float a = x._1();
            float b = x._2();
            int inf = x._3();
            int sup = x._4();
            float h = (b - a) / N;
            float sum = 0.5f * (f(a) + f(b));

            for (int i = inf; i < sup; i++) {
                float xx = a + h * i;
                sum += OCLMath.exp(-xx * xx / 2) / OCLMath.sqrt(2 * Math.PI);
            }
            return sum * h;
        });

        int partitions = 5;

        RuntimeObjectTypeInfo t = TypeFactory.Tuple("Tuple4<Float, Float, Integer, Integer>");
        PArray<Tuple4<Float, Float, Integer, Integer>> input = new PArray<>(partitions, t);
        input.put(0, new Tuple4<>(0.0f, 10.0f, 1, 20));
        input.put(1, new Tuple4<>(0.0f, 10.0f, 21, 40));
        input.put(2, new Tuple4<>(0.0f, 10.0f, 41, 60));
        input.put(3, new Tuple4<>(0.0f, 10.0f, 61, 80));
        input.put(3, new Tuple4<>(0.0f, 10.0f, 81, 100));

        PArray<Float> result = partialIntegration.apply(input);

        assertNotNull(result);

        float resultSeq = integrateSeq(0f, 10.0f, N);

        float sum = 0.0f;
        for (int i = 0; i < partitions; i++) {
            sum += result.get(i);
        }

        assertEquals(resultSeq, sum, 0.1);

    }

    @Test
    @Ignore
    public void eulerConjeture() {
        final int n = 500;
        int size = n;

        PArray<Integer> input = new PArray<>(size + 1, TypeFactory.Integer());

        // precompute i^4 for i = 0..n
        IntStream.range(0, n).parallel().forEach(i -> {
            int comp = (i + 1) * (i + 1) * (i + 1) * (i + 1);
            input.put(i, comp);
        });

        MapJavaThreads<Integer, Tuple4<Long, Long, Long, Long>> function = new MapJavaThreads<>(4, x -> {
            long d4 = x;
            Tuple4<Long, Long, Long, Long> t = new Tuple4<>();
            t._1(0.0);
            t._2(0.0);
            t._3(0.0);
            t._4(0.0);
            for (int a = 1; a < n; a++) {
                long a4 = input.get(a);
                for (int b = a; b < n; b++) {
                    long b4 = input.get(b);
                    for (int c = b; c < n; c++) {
                        long c4 = input.get(c);
                        long comp = a4 + b4 + c4;
                        if (comp == d4) {
                            t._1(a4);
                            t._2(b4);
                            t._3(c4);
                            t._4(d4);
                        }
                    }
                }
            }
            return t;
        });

        PArray<Tuple4<Long, Long, Long, Long>> result = function.apply(input);
        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void testParallelReduce() {
        int size = 1000;
        Random f = new Random();

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            input.put(i, f.nextInt());
        }

        ArrayFunction<Integer, Integer> reductionSequential = new Reduce<>((x, y) -> x + y, 0);
        PArray<Integer> output = reductionSequential.apply(input);

        ArrayFunction<Integer, Integer> reductionParallel = new ReduceJavaThreads<>((x, y) -> x + y, 0);
        PArray<Integer> outputParallel = reductionParallel.apply(input);

        assertNotNull(output);
        assertNotNull(outputParallel);

        System.out.println(output);
        System.out.println(outputParallel);

        if (Math.abs(Math.abs(output.get(0)) - Math.abs(outputParallel.get(0))) > 0.5) {
            assertEquals(0, 1);
        }
    }
}
