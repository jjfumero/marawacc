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
package uk.ac.ed.accelerator.ocl.phases;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ed.accelerator.ocl.helper.ArrayUtil;

import com.oracle.graal.compiler.common.type.ObjectStamp;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.phases.Phase;

public class BuildGPUArrayIndexDataPhase extends Phase {

    private Object[] params;
    private Map<ParameterNode, Integer[]> arraySizes;

    public BuildGPUArrayIndexDataPhase(Object[] params) {
        this.params = params;
        arraySizes = new HashMap<>();
    }

    @Override
    protected void run(StructuredGraph graph) {
        int isInstanceMethod = Modifier.isStatic(graph.method().getModifiers()) ? 0 : 1;
        for (ParameterNode paramNode : graph.getNodes(ParameterNode.TYPE)) {
            if (!paramNode.getStackKind().isPrimitive() && ((ObjectStamp) paramNode.stamp()).type().isArray()) {
                Object param = params[paramNode.index() - isInstanceMethod];
                Integer[] indexData = buildIndexData(param, -1);
                arraySizes.put(paramNode, indexData);
            }
        }
    }

    public Map<ParameterNode, Integer[]> getIndexData() {
        return arraySizes;
    }

    public static Integer[] buildIndexData(Object srcArray, int extraParam) {
        List<Integer> indexData = new ArrayList<>();
        int dimSize = ArrayUtil.getNumArrayDimensions(srcArray);
        getIndexData(srcArray, indexData, 1, dimSize, 0, 0);

        Integer[] result = new Integer[indexData.size() + 1];
        indexData.toArray(result);

        if (extraParam != -1) {
            result[2] = extraParam;
        }

        return result;
    }

    protected static int getIndexData(Object srcArray, List<Integer> indexData, int currentDepth, final int depthLevel, int nextIndex, int copySize) {
        if (srcArray == null) {
            indexData.add(-1); // for null reference.
            return 0;
        } else if (srcArray.getClass().isArray()) {
            int length = Array.getLength(srcArray);
            indexData.add(nextIndex, length); // add size of array first.
            int nextLoc = indexData.size();
            if (currentDepth == depthLevel) {
                // then we have reached last dimension so have a pointer from here to data array.
                indexData.add(copySize);
                return length;
            } else {
                // allocate space for the number of arrays in this current array.
                for (int i = 0; i < length; i++) {
                    indexData.add(0);
                }
                // Recursively build rest of index table.
                int currentSize = copySize;
                for (int i = 0; i < length; i++) {
                    int nextBase = indexData.size();
                    // set start of next array of arrays.
                    indexData.set(nextLoc + i, nextBase);
                    // recurse.
                    currentSize += getIndexData(Array.get(srcArray, i), indexData, currentDepth + 1, depthLevel, nextBase, currentSize);
                }
                return currentSize - copySize;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        String str = "";
        for (Map.Entry<ParameterNode, Integer[]> entry : arraySizes.entrySet()) {
            ParameterNode key = entry.getKey();
            Integer[] value = entry.getValue();
            str += "[" + key.toString() + "," + value.toString() + "]\n";
        }
        return str;
    }

}
