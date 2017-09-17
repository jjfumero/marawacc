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

public class Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> extends Tuple {

    public T1 _1;
    public T2 _2;
    public T3 _3;
    public T4 _4;
    public T5 _5;
    public T6 _6;
    public T7 _7;
    public T8 _8;
    public T9 _9;
    public T10 _10;
    public T11 _11;

    public Tuple11() {
        super();
    }

    public Tuple11(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11) {
        super();
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
        this._5 = _5;
        this._6 = _6;
        this._7 = _7;
        this._8 = _8;
        this._9 = _9;
        this._10 = _10;
        this._11 = _11;
    }

    public T1 get_1() {
        return _1;
    }

    public T2 get_2() {
        return _2;
    }

    public T3 get_3() {
        return _3;
    }

    public T4 get_4() {
        return _4;
    }

    public T5 get_5() {
        return _5;
    }

    public T6 get_6() {
        return _6;
    }

    public T7 get_7() {
        return _7;
    }

    public T8 get_8() {
        return _8;
    }

    public T9 get_9() {
        return _9;
    }

    public T10 get_10() {
        return _10;
    }

    public T11 get_11() {
        return _11;
    }

    public T1 _1() {
        return this._1;
    }

    public T2 _2() {
        return this._2;
    }

    public T3 _3() {
        return this._3;
    }

    public T4 _4() {
        return _4;
    }

    public T5 _5() {
        return _5;
    }

    public T6 _6() {
        return _6;
    }

    public T7 _7() {
        return _7;
    }

    public T8 _8() {
        return _8;
    }

    public T9 _9() {
        return _9;
    }

    public T10 _10() {
        return _10;
    }

    public T11 _11() {
        return _11;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _1(Object _1) {
        this._1 = (T1) _1;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _2(Object _2) {
        this._2 = (T2) _2;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _3(Object _3) {
        this._3 = (T3) _3;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _4(Object _4) {
        this._4 = (T4) _4;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _5(Object _5) {
        this._5 = (T5) _5;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _6(Object _6) {
        this._6 = (T6) _6;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _7(Object _7) {
        this._7 = (T7) _7;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _8(Object _8) {
        this._8 = (T8) _8;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _9(Object _9) {
        this._9 = (T9) _9;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _10(Object _10) {
        this._10 = (T10) _10;
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void _11(Object _11) {
        this._11 = (T11) _11;
    }

    @Override
    public RuntimeObjectTypeInfo getType() {
        return new RuntimeObjectTypeInfo(Tuple11.class, new RuntimeObjectTypeInfo(this._1.getClass()), new RuntimeObjectTypeInfo(this._2.getClass()), new RuntimeObjectTypeInfo(this._3.getClass()),
                        new RuntimeObjectTypeInfo(this._4.getClass()), new RuntimeObjectTypeInfo(this._5.getClass()), new RuntimeObjectTypeInfo(this._6.getClass()), new RuntimeObjectTypeInfo(
                                        this._7.getClass()), new RuntimeObjectTypeInfo(this._8.getClass()), new RuntimeObjectTypeInfo(this._9.getClass(),
                                        new RuntimeObjectTypeInfo(this._10.getClass()), new RuntimeObjectTypeInfo(this._11.getClass())));
    }

    @Override
    public String toString() {
        return "<" + _1 + "," + _2 + "," + _3 + "," + _4 + "," + _5 + "," + _6 + "," + _7 + "," + _8 + "," + _9 + "," + _10 + ">";
    }
}
