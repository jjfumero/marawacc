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

package uk.ac.ed.compiler.utils;

import java.lang.reflect.Method;
import java.util.Arrays;

import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import com.oracle.graal.compiler.common.GraalOptions;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.iterators.NodeIterable;
import com.oracle.graal.java.BytecodeDisassembler;
import com.oracle.graal.nodeinfo.Verbosity;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.StructuredGraph.AllowAssumptions;
import com.oracle.graal.nodes.java.MethodCallTargetNode;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.printer.GraphPrinterDumpHandler;

/**
 * Graal compile and Java reflection utilities.
 *
 */
public interface CompilationUtils {

    /**
     * Find a method given the class and the string.
     *
     * @param klass
     * @param nameMethod
     * @return {@link Method}
     */
    default Method getMethodFromName(Class<?> klass, String nameMethod) {
        Method method = null;
        for (Method m : klass.getMethods()) {
            if (m.getName().equals(nameMethod)) {
                assert method == null : "found more than one implementation: " + klass;
                method = m;
            }
        }
        return method;
    }

    /**
     * Build the {@link StructuredGraph} (GraalIR) for a given {@link Method}.
     *
     * @param method
     * @return {@link StructuredGraph}
     */
    default StructuredGraph getStructuredGraph(Method method) {
        Providers providers = GraalOCLBackendConnector.getProviders();
        StructuredGraph graph = new StructuredGraph(((HotSpotMetaAccessProvider) providers.getMetaAccess()).lookupJavaMethod(method), AllowAssumptions.YES);
        GraalOCLBackendConnector.apply(graph);
        return graph;
    }

    /**
     * Get the RevelvedJavaMethod for a given Method.
     *
     * @param applyMethod
     * @return {@link ResolvedJavaMethod}
     */
    default ResolvedJavaMethod getResolvedApplyJavaMethod(Method applyMethod) {
        Providers providers = GraalOCLBackendConnector.getProviders();
        StructuredGraph graph = new StructuredGraph(((HotSpotMetaAccessProvider) providers.getMetaAccess()).lookupJavaMethod(applyMethod), AllowAssumptions.YES);
        GraalOCLBackendConnector.apply(graph);
        NodeIterable<MethodCallTargetNode> calls = graph.getNodes(MethodCallTargetNode.TYPE);
        ResolvedJavaMethod resolvedJavaMethod = calls.first().targetMethod();
        return resolvedJavaMethod;
    }

    /**
     * Get the {@link ResolvedJavaMethod} given the name and the Class. Method been able to compile
     * code to the VM. See CGO tutorials.
     *
     * @param klass
     * @param methodName
     * @return {@link ResolvedJavaMethod}
     */
    default ResolvedJavaMethod getResolvedJavaMethod(Class<?> klass, String methodName) {
        Method method = null;
        for (Method m : klass.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                method = m;
            }
        }
        if (method == null) {
            return null;
        } else {
            return GraalOCLBackendConnector.getMetaAccessProvider().lookupJavaMethod(method);
        }
    }

    /**
     * Utility for printing the GraalIR
     *
     * @param graph
     * @param verbose
     */
    default void printGraph(StructuredGraph graph, boolean verbose) {
        for (Node node : graph.getNodes()) {
            if (verbose) {
                System.out.println(node.toString(Verbosity.All));
            } else {
                System.out.println(node);
            }
        }
    }

    /**
     * Utility for printing the bytecodes associated to the method.
     *
     * @param method
     */
    default void printByteCodes(ResolvedJavaMethod method) {
        byte[] bytecodes = method.getCode();
        System.out.println(Arrays.toString(bytecodes));
        System.out.println(new BytecodeDisassembler().disassemble(method));
    }

    /**
     * Save the GraalIR into a file. It is useful to print it with IGV.
     *
     * @param graph
     * @param name
     */
    static void saveGraph(StructuredGraph graph, String name) {
        GraalOptions.PrintIdealGraphFile.setValue(true);
        GraphPrinterDumpHandler printer = new GraphPrinterDumpHandler();
        printer.dump(graph, name);
        printer.close();
    }

    /**
     * Dump the GraalIR. It is useful to print it with IGV.
     *
     * @param graph
     * @param name
     */
    static void dumpGraph(StructuredGraph graph, String name) {
        GraphPrinterDumpHandler printer = new GraphPrinterDumpHandler();
        printer.dump(graph, name);
        printer.close();
    }
}
