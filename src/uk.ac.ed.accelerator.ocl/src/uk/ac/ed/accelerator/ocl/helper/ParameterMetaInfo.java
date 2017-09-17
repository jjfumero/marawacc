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

package uk.ac.ed.accelerator.ocl.helper;

public class ParameterMetaInfo {

    private int typeSize;
    private Class<?> klass;
    private int len;
    private int dim;

    public ParameterMetaInfo(int typeSize, Class<?> klass, int len, int dim) {
        super();
        this.typeSize = typeSize;
        this.klass = klass;
        this.len = len;
        this.dim = dim;
    }

    public Class<?> getMetaClass() {
        return this.klass;
    }

    public int getLen() {
        return this.len;
    }

    public int getDim() {
        return this.dim;
    }

    public int getTypeSize() {
        return this.typeSize;
    }
}
