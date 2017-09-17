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

package uk.ac.ed.jpai;

import uk.ac.ed.datastructures.interop.InteropTable;
import uk.ac.ed.datastructures.tuples.Tuple10;
import uk.ac.ed.datastructures.tuples.Tuple11;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;
import uk.ac.ed.datastructures.tuples.Tuple4;
import uk.ac.ed.datastructures.tuples.Tuple5;
import uk.ac.ed.datastructures.tuples.Tuple6;
import uk.ac.ed.datastructures.tuples.Tuple7;
import uk.ac.ed.datastructures.tuples.Tuple8;
import uk.ac.ed.datastructures.tuples.Tuple9;

public class DataTypeAPIHelper {

    public static Object[] createOneOutputElement(Class<?> outputComponentType) throws Exception {
        if (outputComponentType == Float.class) {               // Basic data type support
            return new Float[1];
        } else if (outputComponentType == Double.class) {
            return new Double[1];
        } else if (outputComponentType == Integer.class) {
            return new Integer[1];
        } else if (outputComponentType == Long.class) {
            return new Long[1];
        } else if (outputComponentType == Short.class) {
            return new Short[1];
        } else if (outputComponentType == Byte.class) {
            return new Byte[1];
        } else if (outputComponentType == Character.class) {
            return new Character[1];
        } else if (outputComponentType == Tuple2.class) {       // Tuples support
            return new Tuple2<?, ?>[1];
        } else if (outputComponentType == Tuple3.class) {
            return new Tuple3<?, ?, ?>[1];
        } else if (outputComponentType == Tuple4.class) {
            return new Tuple4<?, ?, ?, ?>[1];
        } else if (outputComponentType == Tuple5.class) {
            return new Tuple5<?, ?, ?, ?, ?>[1];
        } else if (outputComponentType == Tuple6.class) {
            return new Tuple6<?, ?, ?, ?, ?, ?>[1];
        } else if (outputComponentType == Tuple7.class) {
            return new Tuple7<?, ?, ?, ?, ?, ?, ?>[1];
        } else if (outputComponentType == Tuple8.class) {
            return new Tuple8<?, ?, ?, ?, ?, ?, ?, ?>[1];
        } else if (outputComponentType == Tuple9.class) {
            return new Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>[1];
        } else if (outputComponentType == Tuple10.class) {
            return new Tuple10<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>[1];
        } else if (outputComponentType == Tuple11.class) {
            return new Tuple11<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>[1];
        } else {
            throw new Exception("Data type not supported - One output representation");
        }
    }

    public static Object[] createOneOutputElement(InteropTable interop) throws Exception {
        if (interop == InteropTable.T2) {       // Tuples support
            return new Tuple2<?, ?>[1];
        } else if (interop == InteropTable.T3) {
            return new Tuple3<?, ?, ?>[1];
        } else if (interop == InteropTable.T4) {
            return new Tuple4<?, ?, ?, ?>[1];
        } else if (interop == InteropTable.T5) {
            return new Tuple5<?, ?, ?, ?, ?>[1];
        } else if (interop == InteropTable.T6) {
            return new Tuple6<?, ?, ?, ?, ?, ?>[1];
        } else {
            throw new Exception("Data type not supported - One output representation");
        }
    }
}
