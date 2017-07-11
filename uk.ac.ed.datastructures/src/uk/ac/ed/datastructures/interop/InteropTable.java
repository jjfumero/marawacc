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

package uk.ac.ed.datastructures.interop;

/**
 * Table for interoperability between JVM guest languages and Marawacc.
 *
 */
public enum InteropTable {

    /**
     * Object equivalence for Truffle languages into Marawacc.
     */
    T1(uk.ac.ed.datastructures.tuples.Tuple1.class),
    T2(uk.ac.ed.datastructures.tuples.Tuple2.class),
    T3(uk.ac.ed.datastructures.tuples.Tuple3.class),
    T4(uk.ac.ed.datastructures.tuples.Tuple4.class),
    T5(uk.ac.ed.datastructures.tuples.Tuple5.class),
    T6(uk.ac.ed.datastructures.tuples.Tuple6.class),
    T7(uk.ac.ed.datastructures.tuples.Tuple7.class),
    T8(uk.ac.ed.datastructures.tuples.Tuple8.class),
    T9(uk.ac.ed.datastructures.tuples.Tuple9.class),
    T10(uk.ac.ed.datastructures.tuples.Tuple10.class),
    T11(uk.ac.ed.datastructures.tuples.Tuple11.class);

    InteropTable(Class<?> klass) {
        this.klass = klass;
    }

    private Class<?> klass;

    public Class<?> getTupleClass() {
        return klass;
    }

    @Override
    public String toString() {
        return klass.toString();
    }
}
