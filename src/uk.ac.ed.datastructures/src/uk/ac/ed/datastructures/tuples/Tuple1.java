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

public class Tuple1<T> extends Tuple {

    public T _1;

    public Tuple1() {
        super();
    }

    public Tuple1(T _1) {
        this._1 = _1;
    }

    /**
     * Get the first element in the Tuple2.
     *
     * @return T
     */
    public T _1() {
        return this._1;
    }

    /**
     * Set the first element in the Tuple2.
     *
     * @param t
     */
    @SuppressWarnings("unchecked")
    public void _1(Object t) {
        this._1 = (T) t;
    }

    @Override
    public RuntimeObjectTypeInfo getType() {
        return new RuntimeObjectTypeInfo(Tuple1.class, new RuntimeObjectTypeInfo(this._1.getClass()));
    }

    @Override
    public String toString() {
        return "<" + _1 + ">";
    }
}
