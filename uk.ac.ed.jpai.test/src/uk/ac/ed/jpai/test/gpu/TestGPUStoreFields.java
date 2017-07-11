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

import java.util.Random;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestGPUStoreFields extends MarawaccOpenCLTestBase {

    @Test
    public void testStoreFields01() {

        ArrayFunction<Integer, Tuple2<Integer, Integer>> function = new MapAccelerator<>(x -> {
            Tuple2<Integer, Integer> t = new Tuple2<>();
            t._1 = x;
            t._2 = x;
            return t;
        });

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());

        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            input.put(i, r.nextInt());
        }

        PArray<Tuple2<Integer, Integer>> apply = function.apply(input);

        assertNotNull(apply);

    }

}
