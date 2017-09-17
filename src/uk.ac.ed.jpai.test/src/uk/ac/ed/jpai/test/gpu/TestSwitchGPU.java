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

public class TestSwitchGPU extends MarawaccOpenCLTestBase {

    @Test
    public void test01() {
        ArrayFunction<Integer, Integer> mapTimesTwo = new MapAccelerator<>(x -> {
            switch (x) {
                case 0:
                    return -1;
                case 1:
                    return 0;
                case 2:
                    return 1;
                default:
                    return 100;
            }
        });

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Integer> output = new PArray<>(size, TypeFactory.Integer());

        for (int i = 0; i < 10; i++) {
            input.put(i, i);
        }

        output = mapTimesTwo.apply(input);

        assertNotNull(output);
    }

    @Test
    public void test02() {

        ArrayFunction<Integer, Integer> mapTimesTwo = new MapAccelerator<>(x -> {
            int a = 0;
            switch (x) {
                case 0:
                    a = 10;
                    break;
                case 1:
                    a = 20;
                    break;
                case 2:
                    a = 30;
                    break;
                default:
                    a = 100;
            }

            return a;
        });

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Integer> output = new PArray<>(size, TypeFactory.Integer());

        for (int i = 0; i < 10; i++) {
            input.put(i, i);
        }

        output = mapTimesTwo.apply(input);

        assertNotNull(output);
    }

    @Test
    public void test03() {

        ArrayFunction<Integer, Integer> mapTimesTwo = new MapAccelerator<>(x -> {
            int a = 0;
            switch (x) {
                case 0:
                    a = 10;
                    break;
                case 1:
                    a = 20;
                    break;
                case 2:
                    a = 30;
                    break;
                default:
                    a = 100;
            }

            for (int i = 0; i < 100; i++) {
                a += i;
            }
            return a;
        });

        int size = 10;

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        PArray<Integer> output = new PArray<>(size, TypeFactory.Integer());

        for (int i = 0; i < 10; i++) {
            input.put(i, i);
        }

        output = mapTimesTwo.apply(input);

        assertNotNull(output);
    }

}