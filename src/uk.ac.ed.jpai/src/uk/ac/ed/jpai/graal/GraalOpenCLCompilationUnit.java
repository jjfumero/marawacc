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
package uk.ac.ed.jpai.graal;

import java.util.ArrayList;

import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;

import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.StructuredGraph;

public class GraalOpenCLCompilationUnit {

    private RuntimeObjectTypeInfo inputType;
    private RuntimeObjectTypeInfo outputType;
    private Object[] scopeArrays;
    private ArrayList<Node> scopeNodes;
    private StructuredGraph graph;

    public GraalOpenCLCompilationUnit(RuntimeObjectTypeInfo inputType, RuntimeObjectTypeInfo ouputType, StructuredGraph graph) {
        super();
        this.inputType = inputType;
        this.outputType = ouputType;
        this.graph = graph;
        this.scopeArrays = null;
    }

    public void setScopeArrays(Object[] scope) {
        this.scopeArrays = scope;
    }

    public void setScopeNodes(ArrayList<Node> scopeNodes) {
        this.scopeNodes = scopeNodes;
    }

    public ArrayList<Node> getScopeNodes() {
        return scopeNodes;
    }

    public Object[] getScopeArrays() {
        return scopeArrays;
    }

    /**
     * @return the ouputType
     */
    public RuntimeObjectTypeInfo getOuputType() {
        return outputType;
    }

    /**
     * @param ouputType the ouputType to set
     */
    public void setOuputType(RuntimeObjectTypeInfo ouputType) {
        this.outputType = ouputType;
    }

    /**
     * @return the graph
     */
    public StructuredGraph getGraph() {
        return graph;
    }

    /**
     * @param inputType the inputType to set
     */
    public void setInputType(RuntimeObjectTypeInfo inputType) {
        this.inputType = inputType;
    }

    public RuntimeObjectTypeInfo getInputType() {
        return this.inputType;
    }

}
