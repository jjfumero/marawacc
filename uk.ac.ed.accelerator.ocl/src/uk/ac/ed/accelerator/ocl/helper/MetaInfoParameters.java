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

import uk.ac.ed.accelerator.utils.ArrayPackage;

public class MetaInfoParameters {

    private Object[] finalParameters;
    private boolean[] ioMask;
    private boolean[] isPrimitive;
    private int[] sizes;
    private boolean[] isFlatten;
    private ArrayPackage arrayPackage;
    private int[] finalDirection;
    private int outputIndex;
    private int indexUnrolledStructs;

    public MetaInfoParameters(Object[] finalParameters, boolean[] ioMask, boolean[] isPrimitive, int[] sizes, boolean[] isFlatten, ArrayPackage arrayPackage, int[] finalDirection, int outputIndex,
                    int indexUnrolledStructs) {
        this.finalParameters = finalParameters;
        this.ioMask = ioMask;
        this.isPrimitive = isPrimitive;
        this.sizes = sizes;
        this.isFlatten = isFlatten;
        this.arrayPackage = arrayPackage;
        this.finalDirection = finalDirection;
        this.outputIndex = outputIndex;
        this.indexUnrolledStructs = indexUnrolledStructs;
    }

    public Object[] getFinalParameters() {
        return finalParameters;
    }

    public boolean[] getIoMask() {
        return ioMask;
    }

    public boolean[] getIsPrimitive() {
        return isPrimitive;
    }

    public int[] getSizes() {
        return sizes;
    }

    public boolean[] isFlatten() {
        return isFlatten;
    }

    public ArrayPackage getArrayContainer() {
        return arrayPackage;
    }

    public int[] getFinalDirection() {
        return finalDirection;
    }

    public int getOutputIndex() {
        return this.outputIndex;
    }

    public int getIndexUnrolledStructs() {
        return this.indexUnrolledStructs;
    }
}
