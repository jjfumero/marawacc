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

import java.util.Map;

import uk.ac.ed.accelerator.ocl.phases.ArrayDepth;
import uk.ac.ed.accelerator.ocl.phases.ArrayDepthBuilderPhase;
import uk.ac.ed.accelerator.ocl.phases.ArrayDimensionPhase;

import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.StructuredGraph;

class SymbolPhases {

    private Map<Node, ArrayDepth> arrayAccessDim;
    private Map<Node, Integer> arrayDimensions;
    private Map<String, OpenCLStructure> structures;

    public Map<Node, ArrayDepth> getArrayAccessDim() {
        return arrayAccessDim;
    }

    public Map<Node, Integer> getArrayDimensions() {
        return arrayDimensions;
    }

    public Map<String, OpenCLStructure> getStructures() {
        return structures;
    }

    /**
     * Build array node to array dimension mapping.
     *
     * @param graph
     */
    private void applyArrayDephPhase(StructuredGraph graph) {

        ArrayDepthBuilderPhase arrayPhase2 = new ArrayDepthBuilderPhase();
        arrayPhase2.apply(graph);
        arrayAccessDim = arrayPhase2.getDepthInfo();
    }

    /**
     * Add array dimension information for local nodes.
     *
     * @param graph
     */
    private void applyArrayDimensionPhase(StructuredGraph graph) {
        ArrayDimensionPhase arrayPhase3 = new ArrayDimensionPhase();
        arrayPhase3.apply(graph);
        arrayDimensions = arrayPhase3.getDimensions();
    }

    public void applyPhases(StructuredGraph graph) {
        applyArrayDephPhase(graph);
        applyArrayDimensionPhase(graph);
    }
}
