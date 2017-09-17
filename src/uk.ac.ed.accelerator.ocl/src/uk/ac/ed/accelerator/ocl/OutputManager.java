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

package uk.ac.ed.accelerator.ocl;

import uk.ac.ed.accelerator.utils.ArrayPackage;

public class OutputManager {

    private Object[] data;
    private boolean inGlobalStorage;
    private int hashCodeOutput;
    private ArrayPackage arrayPackage;

    public OutputManager() {
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getData() {
        return (T[]) data;
    }

    public <T> void setData(T[] data) {
        this.data = data;
    }

    public void setInGlobalStorage(int hashCodeOutput) {
        inGlobalStorage = true;
        this.hashCodeOutput = hashCodeOutput;
    }

    public void setArrayInGlobalStorage(ArrayPackage arrayPackage) {
        inGlobalStorage = true;
        this.arrayPackage = arrayPackage;
    }

    public ArrayPackage getArrayInGlobalStorage() {
        return this.arrayPackage;
    }

    public boolean isInGlobalStorage() {
        return inGlobalStorage;
    }

    public int getHashCode() {
        return hashCodeOutput;
    }
}
