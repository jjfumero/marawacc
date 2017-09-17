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

import java.util.HashMap;
import java.util.Map;

import com.oracle.graal.compiler.common.type.ObjectStamp;
import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.java.LoadFieldNode;
import com.oracle.graal.phases.Phase;

public class ArrayDimensionPhase extends Phase {

    private Map<Node, Integer> arrayDimensions;

    public ArrayDimensionPhase() {
        arrayDimensions = new HashMap<>();
    }

    @Override
    protected void run(StructuredGraph graph) {

        try {
            for (ParameterNode paramNode : graph.getNodes(ParameterNode.TYPE)) {
                if (!paramNode.getStackKind().isPrimitive() && ((ObjectStamp) paramNode.stamp()).type().isArray()) {
                    String name = ((ObjectStamp) paramNode.stamp()).type().getName();
                    int dim = name.lastIndexOf('[') - name.indexOf('[') + 1;
                    arrayDimensions.put(paramNode, dim);
                }
            }
        } catch (Exception e) {

        }

        for (Node n : graph.getNodes()) {
            if (n instanceof LoadFieldNode) {
                LoadFieldNode lfn = (LoadFieldNode) n;
                if (!lfn.field().getJavaKind().isPrimitive() && lfn.field().getType().getComponentType() != null) {
                    int dim = lfn.field().getType().getName().length() - 1;
                    arrayDimensions.put(lfn, dim);
                }
            }
        }
    }

    public Map<Node, Integer> getDimensions() {
        return arrayDimensions;
    }

}
