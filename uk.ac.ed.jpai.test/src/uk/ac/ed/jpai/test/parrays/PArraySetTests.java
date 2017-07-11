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
package uk.ac.ed.jpai.test.parrays;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class PArraySetTests extends MarawaccOpenCLTestBase {

    @Test
    public void test01() {

        int size = 10;
        final PArray<Integer> inputA = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; ++i) {
            inputA.put(i, i);
        }

        final PArray<Double> inputB = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            inputB.put(i, (double) i + 100);
        }

        final PArray<Tuple2<Integer, Double>> inputReal = new PArray<>(size, TypeFactory.Tuple("Tuple2<Integer, Double>"), false);

        inputReal.setBuffer(0, inputA.getArrayReference());
        inputReal.setBuffer(1, inputB.getArrayReference());

        assertNotNull(inputReal);

        for (int i = 0; i < size; i++) {
            assertEquals(inputA.get(i), inputReal.get(i)._1);
            assertEquals(inputB.get(i), inputReal.get(i)._2);
            Double a = new Double(i + 100);
            assertEquals(a, inputReal.get(i)._2);
        }
    }

    @Test
    public void test02() {
        int size = 10;
        final PArray<Double> inputB = new PArray<>(size, TypeFactory.Double());
        for (int i = 0; i < size; ++i) {
            inputB.put(i, (double) i + 100);
        }

        double[] asDoubleArray = inputB.asDoubleArray();

        assertNotNull(asDoubleArray);

        for (int i = 0; i < size; i++) {
            assertEquals(asDoubleArray[i], inputB.get(i), 0.01);
        }
    }
}
