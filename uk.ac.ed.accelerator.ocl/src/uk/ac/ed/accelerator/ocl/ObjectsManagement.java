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

import java.util.ArrayList;

/**
 * Additional data structure in order to know how many parameters we need to get the proper struct
 * in the code generator. This class is only used by the code generator (lambda)
 *
 */
public class ObjectsManagement {

    private int numFields;
    private ArrayList<String> names;
    private ArrayList<String> inputVars;
    private ArrayList<String> inputAuxVars;

    public ObjectsManagement(int numFields) {
        this.numFields = numFields;
        names = new ArrayList<>();
        inputVars = new ArrayList<>();
        inputAuxVars = new ArrayList<>();
    }

    public void insertVariableName(String name, boolean isInput) {
        names.add(name);
        if (isInput) {
            inputVars.add(name);
        }
    }

    public void insertAuxData(String name) {
        inputAuxVars.add(name);
    }

    public ArrayList<String> getInputVars() {
        return inputVars;
    }

    public ArrayList<String> getInputAuxVars() {
        return inputAuxVars;
    }

    public ArrayList<String> getNameList() {
        return names;
    }

    public String getNameAt(int i) {
        return names.get(i);
    }

    public int getNumElements() {
        return numFields;
    }
}
