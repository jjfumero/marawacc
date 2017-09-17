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

package uk.ac.ed.accelerator.ocl.runtime;

import java.util.Arrays;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import com.oracle.graal.compiler.common.GraalOptions;
import com.oracle.graal.graph.Node;
import com.oracle.graal.java.BytecodeDisassembler;
import com.oracle.graal.nodeinfo.Verbosity;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.printer.GraphPrinterDumpHandler;

public interface GraalIRUtilities {

    static void printGraph(StructuredGraph graph, boolean verbose) {
        // Method only for debugging
        for (Node node : graph.getNodes()) {
            if (verbose) {
                System.out.println(node.toString(Verbosity.All));
            } else {
                System.out.println(node);
                for (Node n : node.cfgSuccessors()) {
                    System.out.println("\t -> " + n);
                }
            }
        }
    }

    static void printByteCodes(ResolvedJavaMethod method) {
        byte[] bytecodes = method.getCode();
        System.out.println(Arrays.toString(bytecodes));
        System.out.println(new BytecodeDisassembler().disassemble(method));
    }

    static void saveGraph(StructuredGraph graph, String name) {
        GraalOptions.PrintIdealGraphFile.setValue(true);
        GraphPrinterDumpHandler printer = new GraphPrinterDumpHandler();
        printer.dump(graph, name);
        printer.close();
    }

    static void dumpGraph(StructuredGraph graph, String name) {
        GraphPrinterDumpHandler printer = new GraphPrinterDumpHandler();
        printer.dump(graph, name);
        printer.close();
    }

    static ResolvedJavaType getType(Class<?> clazz, MetaAccessProvider metaAccess) {
        ResolvedJavaType type = metaAccess.lookupJavaType(clazz);
        type.initialize();
        return type;
    }

    static ResolvedJavaMethod getMethod(ResolvedJavaType type, String methodName) {
        for (ResolvedJavaMethod method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

}
