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
package uk.ac.ed.accelerator.utils;

import java.util.ArrayList;
import java.util.HashMap;

public final class PipelineIndexInfo {

    /**
     * Static decision for pipeline data partition.
     */
    public static int CHUNKPIPELINE = 4;
    public HashMap<Long, Integer> indexReference;

    private static PipelineIndexInfo instance = null;
    private int[][] indexes;

    public static PipelineIndexInfo getInstance() {
        if (instance == null) {
            instance = new PipelineIndexInfo();
        }
        return instance;
    }

    private PipelineIndexInfo() {
        indexes = new int[CHUNKPIPELINE][2];
        indexReference = new HashMap<>();
    }

    public void insert(int virtualID, int from, int to) {
        indexes[virtualID][0] = from;
        indexes[virtualID][1] = to;
    }

    public int getFrom(int virtualID) {
        return indexes[virtualID][0];
    }

    public int getTo(int virtualID) {
        return indexes[virtualID][1];
    }

    public void insertRealThreadIDs(ArrayList<Long> keys, int startID) {
        int virtualID = startID;
        for (Long l : keys) {
            indexReference.put(l, virtualID);
            virtualID++;
        }
        VirtualIDManager.getInstance().setTable(indexReference);
    }

    public synchronized boolean isEmpty() {
        return indexReference.isEmpty();
    }

    /*
     * out[0] = from; out[1] = to
     */
    public synchronized int[] getFromTo(long tid) {
        if (indexReference.containsKey(tid)) {
            int id = indexReference.get(tid);
            int[] out = new int[2];
            out[0] = indexes[id][0];
            out[1] = indexes[id][1];
            return out;
        } else {
            return null;
        }
    }

    public void clean() {
        indexes = null;
        indexReference.clear();
        instance = null;
    }
}
