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
import java.util.HashMap;

public class DimensionTable {

    private HashMap<Object, ArrayList<Integer>> extraDimensions;
    private HashMap<Object, Integer> pipelineSubdimensions;
    private HashMap<Object, Integer> extraTupleDimensions;

    public DimensionTable() {
        extraDimensions = new HashMap<>();
        extraTupleDimensions = new HashMap<>();
        pipelineSubdimensions = new HashMap<>();
    }

    public void addDimension(Object o, ArrayList<Integer> dimList) {
        extraDimensions.put(o, dimList);
    }

    public ArrayList<Integer> getDimList(Object o) {
        return extraDimensions.containsKey(o) ? extraDimensions.get(o) : null;
    }

    public void addTupleDimension(Object o, Integer size) {
        extraTupleDimensions.put(o, size);
    }

    public Integer getTupleDimension(Object o) {
        return extraTupleDimensions.containsKey(o) ? extraTupleDimensions.get(o) : null;
    }

    public void clean() {
        boolean isInfo = (extraDimensions != null || extraTupleDimensions != null) ? true : false;

        if (extraDimensions != null) {
            extraDimensions.clear();
            isInfo = true;
        }
        if (extraTupleDimensions != null) {
            extraTupleDimensions.clear();
        }

        if (isInfo) {
            System.gc();
        }
    }

    public void addSubSize(Object object, Integer fromToSize) {
        pipelineSubdimensions.put(object, fromToSize);
    }

    public Integer getSubSize(Object object) {
        return pipelineSubdimensions.get(object);
    }
}
