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

package uk.ac.ed.jpai.test.api.annotation;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.annotations.Cached;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestAnnotation01 extends MarawaccOpenCLTestBase {

    public @Cached(name = "f") ArrayFunction<Integer, Integer> f;

    @Test
    public void annotation01() {

        PArray<Integer> input = new PArray<>(10, TypeFactory.Integer());
        for (int i = 0; i < input.size(); i++) {
            input.put(i, i);
        }

        for (int i = 0; i < 10; i++) {
            f = new MapAccelerator<>(x -> x + 100);
            f.apply(input);
        }
    }
}
