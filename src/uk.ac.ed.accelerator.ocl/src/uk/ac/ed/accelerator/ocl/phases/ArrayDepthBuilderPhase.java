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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.graal.compiler.common.type.ObjectStamp;
import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.java.AccessIndexedNode;
import com.oracle.graal.nodes.java.ArrayLengthNode;
import com.oracle.graal.nodes.java.LoadFieldNode;
import com.oracle.graal.phases.Phase;

public class ArrayDepthBuilderPhase extends Phase {

    private Map<Node, ArrayDepth> depthInfo;

    public ArrayDepthBuilderPhase() {
        depthInfo = new HashMap<>();
    }

    @Override
    protected void run(StructuredGraph graph) {
        List<Node> arrayRefNodes = new ArrayList<>();

        try {
            for (ParameterNode paramNode : graph.getNodes(ParameterNode.TYPE)) {
                if (!paramNode.getStackKind().isPrimitive() && ((ObjectStamp) paramNode.stamp()).type().isArray()) {
                    depthInfo.put(paramNode, new ArrayDepth(paramNode, 0));
                    arrayRefNodes.add(paramNode);
                }
            }
        } catch (Exception e) {

        }

        for (Node n : graph.getNodes()) {
            if (n instanceof LoadFieldNode) {
                LoadFieldNode lfn = (LoadFieldNode) n;
                if (!lfn.field().getJavaKind().isPrimitive() && lfn.field().getType().getComponentType() != null) {
                    depthInfo.put(lfn, new ArrayDepth(lfn, 0));
                    arrayRefNodes.add(lfn);
                }
            }
        }

        for (Node n : graph.getNodes()) {
            if (n instanceof AccessIndexedNode) {
                AccessIndexedNode ain = (AccessIndexedNode) n;
                // get distance to the root (ParameterNodes)
                int dist = distanceToTop(n);
                // get the origin of the array e.g. input parameter.
                ValueNode paramNode = origin(n);
                depthInfo.put(n, new ArrayDepth(paramNode, dist));
                arrayRefNodes.add(ain);
            }
        }

        for (Node n : arrayRefNodes) {
            buildDepthInfo(n);
        }

        try {
            for (ParameterNode paramNode : graph.getNodes(ParameterNode.TYPE)) {
                if (!paramNode.getStackKind().isPrimitive() && ((ObjectStamp) paramNode.stamp()).type().isArray()) {
                    depthInfo.put(paramNode, new ArrayDepth(paramNode, 1));
                }
            }
        } catch (Exception e) {

        }

        for (Node n : graph.getNodes()) {
            if (n instanceof LoadFieldNode) {
                LoadFieldNode lfn = (LoadFieldNode) n;
                if (!lfn.field().getJavaKind().isPrimitive() && lfn.field().getType().getComponentType() != null) {
                    depthInfo.put(lfn, new ArrayDepth(lfn, 1));
                }
            }
        }
    }

    // Use DFS to build depth info table.
    protected void buildDepthInfo(Node start) {
        ArrayDepth ad = depthInfo.get(start);
        for (Node usage : start.usages()) {
            // check if already visited.
            if (!depthInfo.containsKey(usage) && usage instanceof ArrayLengthNode) {
                depthInfo.put(usage, new ArrayDepth(ad.getNode(), ad.getDimensionAccessedAt() + 1));
            }
        }
    }

    protected ValueNode origin(Node n) {
        if (n instanceof ParameterNode | n instanceof LoadFieldNode) {
            return (ValueNode) n;
        }
        if (n instanceof AccessIndexedNode) {
            return origin(((AccessIndexedNode) n).array());
        }
        return null;
    }

    protected int distanceToTop(Node n) {
        if (n instanceof AccessIndexedNode) {
            AccessIndexedNode ain = (AccessIndexedNode) n;
            return 1 + distanceToTop(ain.array());
        }
        return 0;
    }

    public Map<Node, ArrayDepth> getDepthInfo() {
        return depthInfo;
    }
}
