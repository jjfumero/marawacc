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

package uk.ac.ed.datastructures.tuples;

import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;

public class Tuple3<T, R, U> extends Tuple {

    public T _1;
    public R _2;
    public U _3;

    public Tuple3() {
    }

    public Tuple3(T _1, R _2, U _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    /**
     * Get the first element in the Tuple3.
     *
     * @return T
     */
    public T _1() {
        return this._1;
    }

    /**
     * Get the second element in the Tuple3.
     *
     * @return R
     */
    public R _2() {
        return this._2;
    }

    /**
     * Get the third element in the Tuple3.
     *
     * @return U
     */
    public U _3() {
        return this._3;
    }

    /**
     * Set the first element in the Tuple3.
     */
    @SuppressWarnings({"unchecked", "hiding"})
    public void _1(Object _1) {
        this._1 = (T) _1;
    }

    /**
     * Set the second element in the Tuple3.
     */
    @SuppressWarnings({"unchecked", "hiding"})
    public void _2(Object _2) {
        this._2 = (R) _2;
    }

    /**
     * Set the second element in the Tuple3.
     */
    @SuppressWarnings({"unchecked", "hiding"})
    public void _3(Object _3) {
        this._3 = (U) _3;
    }

    @Override
    public RuntimeObjectTypeInfo getType() {
        return new RuntimeObjectTypeInfo(Tuple3.class, new RuntimeObjectTypeInfo(this._1.getClass()), new RuntimeObjectTypeInfo(this._2.getClass()), new RuntimeObjectTypeInfo(this._3.getClass()));
    }

    @Override
    public String toString() {
        return "<" + _1 + "," + _2 + "," + _3 + ">";
    }
}
