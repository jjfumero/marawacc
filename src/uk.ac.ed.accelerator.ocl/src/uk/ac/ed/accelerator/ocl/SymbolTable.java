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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jdk.vm.ci.meta.JavaKind;
import uk.ac.ed.accelerator.ocl.phases.ArrayDepth;

import com.oracle.graal.graph.Node;

/**
 * Symbol Table class to store and lookup variable information.
 *
 */
public class SymbolTable {

    private LinkedList<Map<String, Node>> nameToNode;
    private LinkedList<Map<Node, String>> nodeToName;
    private LinkedList<Map<String, JavaKind>> nameToType;
    private LinkedList<Map<String, JavaKind>> dynamicArraysName;
    private LinkedList<Map<String, Integer>> dynamicArraysDim;
    private Map<Node, ArrayDepth> arrayAccDim;
    private Map<Node, Integer> arrayDimension;
    private int count = 0;

    public static final String LOOP_VAR = "loop";
    public static final String CONDITION_VAR = "cond";
    public static final String CAST_VAR = "cast";
    public static final String PHI_VAR = "phi";
    public static final String RESULT_VAR = "result";
    public static final String FUNCTION_RESULT = "fuctresult";
    public static final String ARRAY_ELEMENT = "array_elem_val";
    public static final String PARAM_VALUE = "param";
    public static final String FIELD_VALUE = "field";
    public static final String THREAD_VALUE = "thread";

    public static final String ILLEGAL = "ILLEGAL";

    public SymbolTable(Map<Node, ArrayDepth> arrayAccDim, Map<Node, Integer> arrayDimension) {
        nameToNode = new LinkedList<>();
        nodeToName = new LinkedList<>();
        nameToType = new LinkedList<>();
        dynamicArraysDim = new LinkedList<>();
        dynamicArraysName = new LinkedList<>();
        enterScope();
        this.arrayAccDim = arrayAccDim;
        this.arrayDimension = arrayDimension;
    }

    public void add(String variableName, Node n, JavaKind type) {
        nameToNode.getFirst().put(variableName, n);
        nodeToName.getFirst().put(n, variableName);
        nameToType.getLast().put(variableName, type);
    }

    public void enterScope() {
        nameToNode.addFirst(new HashMap<String, Node>());
        nodeToName.addFirst(new HashMap<Node, String>());
        nameToType.addFirst(new HashMap<String, JavaKind>());
        dynamicArraysDim.addFirst(new HashMap<String, Integer>());
        dynamicArraysName.addFirst(new HashMap<String, JavaKind>());
    }

    public void exitScope() {
        nameToNode.removeFirst();
        nodeToName.removeFirst();
        nameToType.removeFirst();
        dynamicArraysName.removeFirst();
        dynamicArraysDim.removeFirst();
    }

    public boolean exists(Node n) {
        return lookupName(n) != null;
    }

    public String lookupName(Node n) {
        for (Map<Node, String> m : nodeToName) {
            if (m.containsKey(n)) {
                return m.get(n);
            }
        }
        return null;
    }

    public JavaKind lookupType(String varName) {
        for (Map<String, JavaKind> m : nameToType) {
            if (m.containsKey(varName)) {
                return m.get(varName);
            }
        }
        return null;
    }

    public Node lookupNode(String varName) {
        for (Map<String, Node> m : nameToNode) {
            if (m.containsKey(varName)) {
                return m.get(varName);
            }
        }
        return null;
    }

    public JavaKind lookupDynamicArrayType(String name) {
        for (Map<String, JavaKind> m : dynamicArraysName) {
            if (m.containsKey(name)) {
                return m.get(name);
            }
        }
        return null;
    }

    public Integer lookupDynamicArrayDim(String name) {
        for (Map<String, Integer> m : dynamicArraysDim) {
            if (m.containsKey(name)) {
                return m.get(name);
            }
        }
        return null;
    }

    public boolean existsDynamicArrayVar(String arrayName) {
        for (Map<String, JavaKind> m : dynamicArraysName) {
            if (m.containsKey(arrayName)) {
                return true;
            }
        }
        return false;
    }

    public void addDynamicArrayVar(String variableName, JavaKind type, Integer dim) {
        dynamicArraysName.getLast().put(variableName, type);
        dynamicArraysDim.getLast().put(variableName, dim);
    }

    public int lookupArrayDimension(Node n) {
        return arrayDimension.get(n);
    }

    public ArrayDepth lookupArrayAccessInfo(Node n) {
        return arrayAccDim.get(n);
    }

    public String newVariable(String type) {
        String var = type + "_" + count;
        count++;
        return var;
    }
}
