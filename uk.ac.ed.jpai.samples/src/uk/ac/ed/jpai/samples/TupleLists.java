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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package uk.ac.ed.jpai.samples;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.MarawaccOptions;

public class TupleLists {

    public final static int SIZE = 10;

    public static void main(String[] args) throws Exception {
        MarawaccOptions.DEOPTIMIZE = false;
        ArrayFunction<Integer, Tuple2<Integer, Integer>> computation = new MapAccelerator<>(x -> new Tuple2<>(x, x));
        PArray<Integer> input = new PArray<>(SIZE, TypeFactory.Integer());
        for (int i = 0; i < SIZE; ++i) {
            input.put(i, i);
        }
        @SuppressWarnings("unused")
        PArray<Tuple2<Integer, Integer>> output = computation.apply(input);
    }
}
