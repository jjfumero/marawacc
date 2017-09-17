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

import java.lang.reflect.Method;

import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.ed.compiler.utils.GraalOCLBackendConnector;
import uk.ac.ed.replacements.ocl.OCLHotSpotReplacementsImpl;
import uk.ac.ed.replacements.ocl.OCLMathIntrinsicNode;

import com.oracle.graal.compiler.common.GraalOptions;
import com.oracle.graal.compiler.target.Backend;
import com.oracle.graal.debug.Debug;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.iterators.NodeIterable;
import com.oracle.graal.hotspot.meta.HotSpotProviders;
import com.oracle.graal.nodes.InvokeNode;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.StructuredGraph.AllowAssumptions;
import com.oracle.graal.nodes.java.MethodCallTargetNode;
import com.oracle.graal.phases.OptimisticOptimizations;
import com.oracle.graal.phases.PhaseSuite;
import com.oracle.graal.phases.common.CanonicalizerPhase;
import com.oracle.graal.phases.common.ConditionalEliminationPhase;
import com.oracle.graal.phases.common.DeadCodeEliminationPhase;
import com.oracle.graal.phases.common.inlining.InliningPhase;
import com.oracle.graal.phases.tiers.HighTierContext;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.printer.GraphPrinterDumpHandler;

public class MapToGraal {

    private StructuredGraph graphLambda;
    private Method lambdaMethod;
    private ResolvedJavaMethod lambdaResolvedJavaMethod;

    public MapToGraal() {
    }

    public StructuredGraph getGraphLambda() {
        return graphLambda;
    }

    private static Backend getBackend() {
        return GraalOCLBackendConnector.getHostBackend();
    }

    private static PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite() {
        return getBackend().getSuites().getDefaultGraphBuilderSuite().copy();
    }

    private static StructuredGraph optimisticOptimisationsLambda(StructuredGraph graph, boolean mathReplacements) {

        Backend backend = getBackend();
        Providers providers = backend.getProviders();
        HotSpotProviders hotspotProviders = (HotSpotProviders) backend.getProviders();
        PhaseSuite<HighTierContext> graphBuilderSuite = getDefaultGraphBuilderSuite();
        HighTierContext context = null;

        if (!mathReplacements) {
            context = new HighTierContext(providers, graphBuilderSuite, OptimisticOptimizations.ALL);
        } else {
            OCLHotSpotReplacementsImpl replacements = new OCLHotSpotReplacementsImpl(providers, hotspotProviders.getSnippetReflection(), providers.getCodeCache().getTarget());
            replacements.setGraphBuilderPlugins(hotspotProviders.getGraphBuilderPlugins());
            Providers providersWithMathReplacements = providers.copyWith(replacements);
            context = new HighTierContext(providersWithMathReplacements, graphBuilderSuite, OptimisticOptimizations.ALL);
        }

        new CanonicalizerPhase().apply(graph, context);

        for (Node node : graph.getNodes()) {
            if (node instanceof InvokeNode) {
                InvokeNode invokeNode = (InvokeNode) node;
                if (invokeNode.callTarget().targetName().startsWith("AF")) {
                    invokeNode.setUseForInlining(false);
                }
            }
        }

        // Inlining
        new InliningPhase(new CanonicalizerPhase()).apply(graph, context);

        new CanonicalizerPhase().apply(graph, context);
        new DeadCodeEliminationPhase().apply(graph);
        new ConditionalEliminationPhase().apply(graph);
        return graph;
    }

    private static Method getLambdaMethod(Class<?> function) {
        Method applyMethod = null;
        for (Method m : function.getMethods()) {
            if (m.getName().equals("apply")) {
                assert applyMethod == null : "found more than one implementation: " + function;
                applyMethod = m;
            }
        }
        return applyMethod;
    }

    private static ResolvedJavaMethod getCFGLambda(Method applyMethod) {

        Providers providers = GraalOCLBackendConnector.getProviders();

        StructuredGraph graph = new StructuredGraph(((HotSpotMetaAccessProvider) providers.getMetaAccess()).lookupJavaMethod(applyMethod), AllowAssumptions.YES, null);

        GraalOCLBackendConnector.apply(graph);

        NodeIterable<MethodCallTargetNode> calls = graph.getNodes(MethodCallTargetNode.TYPE);
        ResolvedJavaMethod lambdaCode = calls.first().targetMethod();
        return lambdaCode;
    }

    private void getCompiledLambda(Class<?> function) {

        Method applyMethod = getLambdaMethod(function);
        this.lambdaMethod = applyMethod;
        this.lambdaResolvedJavaMethod = getCFGLambda(applyMethod);

        // printByteCodes(lambdaResolvedJavaMethod);

        graphLambda = optimiseLamdaGraph(lambdaResolvedJavaMethod);
    }

    private static boolean isOCLMathIntrinsic(StructuredGraph graph) {
        for (Node node : graph.getNodes()) {
            if (node instanceof OCLMathIntrinsicNode) {
                return true;
            }
        }
        return false;
    }

    private static StructuredGraph optimiseLamdaGraph(ResolvedJavaMethod lambdaMethod) {

        StructuredGraph graph = new StructuredGraph(lambdaMethod, AllowAssumptions.YES, null);
        GraalOCLBackendConnector.apply(graph);

        StructuredGraph optimalGraph = (StructuredGraph) graph.copy();

        saveGraph(graph, "nonOptimal");

        // LambdaInfo lambdaInfo = profilingCounter(graph);
        // lambdaInfo.printProfilingInformation();

        graph = optimisticOptimisationsLambda(graph, true);

        if (!isOCLMathIntrinsic(graph)) {
            graph = optimisticOptimisationsLambda(optimalGraph, false);
        }

        return graph;
    }

    public void createGraalIRForLambda(Class<?> classLambdaFunction) {
        try {
            getCompiledLambda(classLambdaFunction);
        } catch (Throwable e) {
            Debug.log("WARNING: Graal compilation failed");
            e.printStackTrace();
        }
    }

    private static void saveGraph(StructuredGraph graph, String name) {
        GraalOptions.PrintIdealGraphFile.setValue(true);
        GraphPrinterDumpHandler printer = new GraphPrinterDumpHandler();
        printer.dump(graph, name);
        printer.close();
    }

    public Method getLambdaMethod() {
        return this.lambdaMethod;
    }

}
