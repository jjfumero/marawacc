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

package uk.ac.ed.datastructures.common;

import org.jocl.Sizeof;

public enum JavaDataTypeSizes {

    // TYPE (<Size In Bytes>, <OpenCL SizeOF>)

    INT(4, Sizeof.cl_int),
    LONG(8, Sizeof.cl_long),
    FLOAT(4, Sizeof.cl_float),
    BYTE(1, Sizeof.cl_char),
    CHAR(1, Sizeof.cl_char),
    BOOLEAN(1, Sizeof.cl_char),
    SHORT(2, Sizeof.cl_short),
    DOUBLE(8, Sizeof.cl_double);

    // XXX: Vector data types for OpenCL

    private int size;
    private int sizeofOCL;

    JavaDataTypeSizes(int size, int sizeOfOCL) {
        this.size = size;
        this.sizeofOCL = sizeOfOCL;
    }

    public int getSize() {
        return this.size;
    }

    public int getOCLSize() {
        return this.sizeofOCL;
    }

}
