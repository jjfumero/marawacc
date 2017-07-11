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

/**
 * Small test to check vector types on GPU.
 *
 */
public class HelloVectorTypes {

    public static void main(String[] args) {

        ArrayFunction<Tuple2<Short, Short>, Short> computation = new MapAccelerator<>(vectors -> (short) (2 * vectors._1() + vectors._2()));

        // Prepare the input data
        int size = 262144;
        PArray<Tuple2<Short, Short>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Short, Short>"));
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((short) i, (short) (i + 2)));
        }

        PArray<Short> apply = computation.apply(input);

    }
}
