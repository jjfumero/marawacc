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

public class Tuple8<T, R, U, V, W, X, Y, Z> extends Tuple {

    public T _1;
    public R _2;
    public U _3;
    public V _4;
    public W _5;
    public X _6;
    public Y _7;
    public Z _8;

    public Tuple8(T _1, R _2, U _3, V _4, W _5, X _6, Y _7, Z _8) {
        super();
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
        this._5 = _5;
        this._6 = _6;
        this._7 = _7;
        this._8 = _8;
    }

    public T _1() {
        return this._1;
    }

    public R _2() {
        return this._2;
    }

    public U _3() {
        return this._3;
    }

    public V _4() {
        return _4;
    }

    public W _5() {
        return _5;
    }

    public X _6() {
        return _6;
    }

    public Y _7() {
        return _7;
    }

    public Z _8() {
        return _8;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _1(Object _1) {
        this._1 = (T) _1;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _2(Object _2) {
        this._2 = (R) _2;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _3(Object _3) {
        this._3 = (U) _3;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _4(Object _4) {
        this._4 = (V) _4;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _5(Object _5) {
        this._5 = (W) _5;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _6(Object _6) {
        this._6 = (X) _6;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _7(Object _7) {
        this._7 = (Y) _7;
    }

    @SuppressWarnings({"hiding", "unchecked"})
    public void _8(Object _8) {
        this._8 = (Z) _8;
    }

    @Override
    public RuntimeObjectTypeInfo getType() {
        return new RuntimeObjectTypeInfo(Tuple8.class, new RuntimeObjectTypeInfo(this._1.getClass()), new RuntimeObjectTypeInfo(this._2.getClass()), new RuntimeObjectTypeInfo(this._3.getClass()),
                        new RuntimeObjectTypeInfo(this._4.getClass()), new RuntimeObjectTypeInfo(this._5.getClass()), new RuntimeObjectTypeInfo(this._6.getClass()), new RuntimeObjectTypeInfo(
                                        this._7.getClass()), new RuntimeObjectTypeInfo(this._8.getClass()));
    }

    @Override
    public String toString() {
        return "<" + _1 + "," + _2 + "," + _3 + "," + _4 + "," + _5 + "," + _6 + "," + _7 + "," + _8 + ">";
    }
}
