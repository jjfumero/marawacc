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
package uk.ac.ed.jpai.test.directparray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

/**
 * Test to check the primitive arrays within the {@link PArray}. As optional, a primitive array can
 * be set to the {@link PArray} with no allocation with direct buffer. The main advantage is that,
 * is a good way to create PArrys for Truffle languages after a specialisation.
 *
 */
public class DirectPArrayTest extends MarawaccOpenCLTestBase {

    @Test
    public void basicDirectArrayPass() {
        // The input comes from primitive type arrays
        int[] inputArray = new int[10];
        Arrays.fill(inputArray, 10);

        // PArray with no buffer allocation
        boolean bufferAllocation = false;
        PArray<Integer> inputPArray = new PArray<>(10, TypeFactory.Integer(), bufferAllocation);
        // We pass the real input to the PArray
        inputPArray.setIntArray(inputArray);

        ArrayFunction<Integer, Double> gpuFunction = new MapAccelerator<>(x -> x * 2.0);

        PArray<Double> output = gpuFunction.apply(inputPArray);

        for (int i = 0; i < output.size(); ++i) {
            assertEquals(inputPArray.get(i) * 2.0, output.get(i), 0.001);
        }
    }

    @Test
    public void tupleDirectArrayPass() {

        // It creates a PArray of Tuple2<I, I> from two different PArrays with primitives arrays.
        // This unittest checks the composition is correct

        int size = 10;
        int[] inputArrayA = new int[size];
        IntStream.range(0, size).parallel().forEach(i -> inputArrayA[i] = i);

        final PArray<Integer> inputA = new PArray<>(size, TypeFactory.Integer(), false);
        inputA.setIntArray(inputArrayA);

        final PArray<Integer> inputB = new PArray<>(size, TypeFactory.Integer(), false);
        int[] inputArrayB = new int[size];
        IntStream.range(0, size).parallel().forEach(i -> inputArrayB[i] = i);
        inputB.setIntArray(inputArrayB);

        final PArray<Tuple2<Integer, Integer>> inputReal = new PArray<>(size, TypeFactory.Tuple("Tuple2<Integer, Integer>"), false);
        inputReal.setIntArray(0, inputA.asIntegerArray());
        inputReal.setIntArray(1, inputB.asIntegerArray());

        assertNotNull(inputReal);

        for (int i = 0; i < size; i++) {
            assertEquals(inputA.get(i), inputReal.get(i)._1);
            assertEquals(inputB.get(i), inputReal.get(i)._2);
        }
    }

    /**
     * Same test than {@link DirectPArrayTest#tupleDirectArrayPass}, but with computation.
     */
    @Test
    public void tupleDirectArrayPassWithComputation() {

        // This test shows an example from T2 -> Double

        int size = 10;
        int[] inputArrayA = new int[size];
        IntStream.range(0, size).parallel().forEach(i -> inputArrayA[i] = i);

        final PArray<Integer> inputA = new PArray<>(size, TypeFactory.Integer(), false);
        inputA.setIntArray(inputArrayA);

        PArray<Integer> inputB = new PArray<>(size, TypeFactory.Integer(), false);
        int[] inputArrayB = new int[size];
        IntStream.range(0, size).parallel().forEach(i -> inputArrayB[i] = i);
        inputB.setIntArray(inputArrayB);

        final PArray<Tuple2<Integer, Integer>> inputReal = new PArray<>(size, TypeFactory.Tuple("Tuple2<Integer, Integer>"), false);
        inputReal.setIntArray(0, inputA.asIntegerArray());
        inputReal.setIntArray(1, inputB.asIntegerArray());

        assertNotNull(inputReal);

        ArrayFunction<Tuple2<Integer, Integer>, Double> function = new MapAccelerator<>(t -> t._1() * t._2() * 0.1);
        assertNotNull(function);

        PArray<Double> result = function.apply(inputReal);

        for (int i = 0; i < size; i++) {
            assertEquals(result.get(i), inputReal.get(i)._1() * inputReal.get(i)._2() * 0.1, 0.01);
        }
    }

    /**
     * Same test than {@link DirectPArrayTest#tupleDirectArrayPass}, but with computation from Tuple
     * to Tuple.
     */
    @Test
    public void tupleDirectArrayPassWithTuplesComputation() {

        // This test shows an example from T2 -> T2

        int size = 10;
        int[] inputArrayA = new int[size];
        IntStream.range(0, size).parallel().forEach(i -> inputArrayA[i] = i);

        final PArray<Integer> inputA = new PArray<>(size, TypeFactory.Integer(), false);
        inputA.setIntArray(inputArrayA);

        PArray<Integer> inputB = new PArray<>(size, TypeFactory.Integer(), false);
        int[] inputArrayB = new int[size];
        IntStream.range(0, size).parallel().forEach(i -> inputArrayB[i] = i);
        inputB.setIntArray(inputArrayB);

        final PArray<Tuple2<Integer, Integer>> inputReal = new PArray<>(size, TypeFactory.Tuple("Tuple2<Integer, Integer>"), false);
        inputReal.setIntArray(0, inputA.asIntegerArray());
        inputReal.setIntArray(1, inputB.asIntegerArray());

        assertNotNull(inputReal);

        ArrayFunction<Tuple2<Integer, Integer>, Tuple2<Double, Double>> function = new MapAccelerator<>(t -> {
            double x = t._1() * 0.1;
            double y = t._2() * 0.2;
            Tuple2<Double, Double> out = new Tuple2<>(x, y);
            return out;
        });
        assertNotNull(function);

        PArray<Tuple2<Double, Double>> result = function.apply(inputReal);

        for (int i = 0; i < size; i++) {
            assertEquals(result.get(i)._1, inputReal.get(i)._1() * 0.1, 0.01);
            assertEquals(result.get(i)._2, inputReal.get(i)._2() * 0.2, 0.01);
        }

        // Since we use the array primitive construction, we can reconstruct them by calling its
        // internal array
        double[] asDoubleArray1 = result.asDoubleArray(0);
        double[] asDoubleArray2 = result.asDoubleArray(1);
        assertNotNull(asDoubleArray1);
        assertNotNull(asDoubleArray2);

        // The precision has to be exactly the same
        for (int i = 0; i < size; i++) {
            assertEquals(result.get(i)._1, asDoubleArray1[i], 0.000000001);
            assertEquals(result.get(i)._2, asDoubleArray2[i], 0.000000001);
        }
    }
}
