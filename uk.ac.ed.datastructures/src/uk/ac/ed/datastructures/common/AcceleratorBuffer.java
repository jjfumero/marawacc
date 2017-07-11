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

import java.util.ArrayList;

import org.jocl.cl_mem;

public class AcceleratorBuffer {

    private int id;

    private ArrayList<cl_mem> clBuffers;
    private ArrayList<cl_mem> metadataBuffers;
    private ArrayList<Class<?>> listTypes;

    public AcceleratorBuffer(int id) {
        clBuffers = new ArrayList<>();
        metadataBuffers = new ArrayList<>();
        listTypes = new ArrayList<>();
        this.id = id;
    }

    public void addCLMem(cl_mem mem) {
        clBuffers.add(mem);
    }

    public void addMetadata(cl_mem mem) {
        metadataBuffers.add(mem);
    }

    public void addType(Class<?> t) {
        listTypes.add(t);
    }

    public int getID() {
        return id;
    }

    public ArrayList<cl_mem> getOpenCLBuffersWithMetadata() {
        ArrayList<cl_mem> mems = new ArrayList<>();
        for (int i = 0; i < clBuffers.size(); ++i) {
            mems.add(clBuffers.get(i));
        }
        for (int i = 0; i < metadataBuffers.size(); ++i) {
            mems.add(metadataBuffers.get(i));
        }
        return mems;
    }

    public ArrayList<cl_mem> getBuffer() {
        return clBuffers;
    }

    public ArrayList<cl_mem> getMetadata() {
        return metadataBuffers;
    }

    public ArrayList<Class<?>> getListOfTypes() {
        return listTypes;
    }

    public void clean() {
        clBuffers.clear();
        metadataBuffers.clear();
        listTypes.clear();
    }

}
