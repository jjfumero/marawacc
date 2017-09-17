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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public final class StorageParallelStage {

    private static StorageParallelStage instance = null;
    private HashMap<Object, ArrayList<Object>> resultList;
    private HashMap<Object, TreeMap<Integer, Integer>> threadIDs;

    public static synchronized <R> StorageParallelStage getInstance() {
        if (instance == null) {
            instance = new StorageParallelStage();
        }
        return instance;
    }

    private StorageParallelStage() {
        resultList = new HashMap<>();
        threadIDs = new HashMap<>();
    }

    public synchronized void add(Object reference, Object partial, int id) {

        TreeMap<Integer, Integer> treeMap = threadIDs.get(reference);
        int i = 0;
        if (treeMap == null) {
            treeMap = new TreeMap<>();
        } else {
            i = resultList.get(reference).size();
        }
        treeMap.put(id, i);
        threadIDs.put(reference, treeMap);

        ArrayList<Object> list = resultList.get(reference);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(partial);
        resultList.put(reference, list);
    }

    public TreeMap<Integer, Integer> getIndexDictionary(Object reference) {
        return threadIDs.get(reference);
    }

    public ArrayList<Object> getResultList(Object reference) {
        return this.resultList.get(reference);
    }

    public void clear() {
        if (resultList != null) {
            resultList.clear();
            resultList = null;
        }
        if (threadIDs != null) {
            threadIDs.clear();
            threadIDs = null;
        }
        instance = null;
    }
}
