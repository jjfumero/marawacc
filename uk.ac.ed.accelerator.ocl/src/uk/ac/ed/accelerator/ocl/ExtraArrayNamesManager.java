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

/**
 * Class to manage extra arrays in Tuples. If there is any arrays in tuples, it is managed as a
 * pointer to global memory and we save memory in the tuple.
 *
 * This class allows to know the data type, the name and the tuple field reference in order to know
 * the attributes in the code generator.
 *
 */
public final class ExtraArrayNamesManager {

    private static ExtraArrayNamesManager instance = null;

    // @formatter:off
    /*
     * List with the array properties.
     * Each item contain a list with:
     *  [0] : name
     *  [1] : index in the tuple
     *  [2] : real data type
     */
    private ArrayList<ArrayList<String>> arraysInputNames;

    private ArrayList<ArrayList<String>> arraysOutputNames;
    // @formatter:on

    /*
     * Look up table for ArrayList
     */
    private HashMap<Integer, Integer> indexInput;
    private HashMap<Integer, Integer> indexOutput;

    /**
     * Enum to facilitate the access of subarrays.
     */
    public enum ArrayInfo {
        NAME(0),
        INDEX(1),
        TYPE(2);

        private int idx;

        ArrayInfo(int i) {
            this.idx = i;
        }

        public int getIdx() {
            return idx;
        }
    }

    public static ExtraArrayNamesManager getInstance() {
        if (instance == null) {
            instance = new ExtraArrayNamesManager();
        }
        return instance;
    }

    private ExtraArrayNamesManager() {
        arraysInputNames = new ArrayList<>();
        arraysOutputNames = new ArrayList<>();
        indexInput = new HashMap<>();
        indexOutput = new HashMap<>();
    }

    public ArrayList<ArrayList<String>> getArrayInputList() {
        return arraysInputNames;
    }

    public ArrayList<ArrayList<String>> getArrayOutputList() {
        return arraysOutputNames;
    }

    public void addInput(ArrayList<String> elem) {
        int key = Integer.parseInt(elem.get(1));
        int idx = arraysInputNames.size();
        arraysInputNames.add(elem);
        indexInput.put(key, idx);
    }

    public void addOutput(ArrayList<String> elem) {
        int key = Integer.parseInt(elem.get(1));
        int idx = arraysOutputNames.size();
        arraysOutputNames.add(elem);
        indexOutput.put(key, idx);
    }

    public HashMap<Integer, Integer> getInIndex() {
        return indexInput;
    }

    public HashMap<Integer, Integer> getOutIndex() {
        return indexOutput;
    }

    public void clean() {

        if (arraysInputNames != null) {
            for (ArrayList<String> elem : arraysInputNames) {
                elem.clear();
            }
            arraysInputNames.clear();
        }

        if (arraysOutputNames != null) {
            for (ArrayList<String> elem : arraysOutputNames) {
                elem.clear();
            }
            arraysOutputNames.clear();

        }
    }
}
