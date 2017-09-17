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

public final class PipelinePartitionInfo {

    private static PipelinePartitionInfo instance = null;
    private int chunk;
    private int numPartitions;
    private ArrayList<int[]> indexes;
    private boolean lastResize;
    private boolean preAllocated;

    public static PipelinePartitionInfo getInstance() {
        if (instance == null) {
            instance = new PipelinePartitionInfo();
        }
        return instance;
    }

    private PipelinePartitionInfo() {
        preAllocated = false;
    }

    public void setChunk(int c) {
        chunk = c;
    }

    public void setIndexes(ArrayList<int[]> ind) {
        indexes = ind;
        preAllocated = true;
    }

    public int getChunk() {
        return chunk;
    }

    public int getLastChunk() {
        int[] fromTo = indexes.get(indexes.size() - 1);
        int size = fromTo[1] - fromTo[0];
        return size;
    }

    public void setNumPartitions(int numOfPartitions) {
        numPartitions = numOfPartitions;
    }

    public int getNumPartitions() {
        return numPartitions;
    }

    public void lastIsLess(boolean b) {
        lastResize = b;
    }

    public boolean resize() {
        return lastResize;
    }

    public boolean preAllocation() {
        return preAllocated;
    }

    public void clean() {
        if (indexes != null) {
            indexes.clear();
            instance = null;
        }
    }
}
