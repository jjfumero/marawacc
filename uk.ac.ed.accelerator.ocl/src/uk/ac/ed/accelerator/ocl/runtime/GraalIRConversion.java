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
import java.util.ArrayList;
import java.util.UUID;

import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.ParallelSkeleton;
import uk.ac.ed.accelerator.ocl.GraalOpenCLRuntime;
import uk.ac.ed.accelerator.ocl.ParallelOptions;
import uk.ac.ed.accelerator.ocl.tier.MarawaccHighTier;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;
import uk.ac.ed.compiler.utils.GraalOCLBackendConnector;
import uk.ac.ed.replacements.ocl.OCLMathIntrinsicNode;

import com.oracle.graal.compiler.target.Backend;
import com.oracle.graal.debug.internal.DebugScope;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.iterators.NodeIterable;
import com.oracle.graal.graphbuilderconf.GraphBuilderConfiguration;
import com.oracle.graal.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import com.oracle.graal.graphbuilderconf.InvocationPlugins;
import com.oracle.graal.java.GraphBuilderPhase;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.StructuredGraph.AllowAssumptions;
import com.oracle.graal.nodes.java.MethodCallTargetNode;
import com.oracle.graal.phases.OptimisticOptimizations;
import com.oracle.graal.phases.PhaseSuite;
import com.oracle.graal.phases.tiers.HighTierContext;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.printer.GraphPrinterDumpHandler;

public class GraalIRConversion implements GraalIRUtilities {

    public static Backend getBackend() {
        return GraalOCLBackendConnector.getHostBackend();
    }

    public static MetaAccessProvider getMetaAccess() {
        return GraalOCLBackendConnector.getHostBackend().getProviders().getMetaAccess();
    }

    protected static PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite(Backend backend) {
        return backend.getSuites().getDefaultGraphBuilderSuite().copy();
    }

    private static void dumpGraph(StructuredGraph graph, String name) {
        GraphPrinterDumpHandler printer = new GraphPrinterDumpHandler();
        DebugScope.forceDump(graph, name);
        printer.dump(graph, name);
        printer.close();
    }

    public static StructuredGraph optimisticOptimisationsLambda(StructuredGraph graph, boolean mathReplacements) {
        HighTierContext context = MarawaccHighTier.applyMathReplacements(mathReplacements, graph);
        MarawaccHighTier.applyHighTierForGPUs(graph, context);
        return graph;
    }

    public static boolean isOCLMathIntrinsic(StructuredGraph graph) {
        for (Node node : graph.getNodes()) {
            if (node instanceof OCLMathIntrinsicNode) {
                return true;
            }
        }
        return false;
    }

    public static StructuredGraph optimiseGraph(ResolvedJavaMethod lambdaMethod) {

        StructuredGraph graph = new StructuredGraph(lambdaMethod, AllowAssumptions.NO);
        GraalOCLBackendConnector.apply(graph);

        StructuredGraph optimalGraph = (StructuredGraph) graph.copy();

        graph = optimisticOptimisationsLambda(graph, true);

        // GraalIR node replacements in the case of Java Math operations
        if (!isOCLMathIntrinsic(graph)) {
            graph = optimisticOptimisationsLambda(optimalGraph, false);
        }

        if (GraalAcceleratorOptions.dumpGraalIRToFile) {
            dumpGraph(graph, "graphToCompiletoOpenCL");
        }

        return graph;
    }

    public static ResolvedJavaMethod getControlFlowGraphLambda(Method applyMethod) {
        Providers providers = GraalOCLBackendConnector.getProviders();
        StructuredGraph graph = new StructuredGraph(((HotSpotMetaAccessProvider) providers.getMetaAccess()).lookupJavaMethod(applyMethod), AllowAssumptions.YES);
        GraalOCLBackendConnector.apply(graph);
        NodeIterable<MethodCallTargetNode> calls = graph.getNodes(MethodCallTargetNode.TYPE);
        ResolvedJavaMethod javaMethodLambda = calls.first().targetMethod();
        return javaMethodLambda;
    }

    public static int getNumberOfLambdaArguments(StructuredGraph graph) {
        return graph.method().getSignature().getParameterCount(false);
    }

    public static Method getLambdaMethod(Class<?> function) {
        Method applyMethod = null;
        for (Method m : function.getMethods()) {
            if (m.getName().equals("apply")) {
                assert applyMethod == null : "found more than one implementation: " + function;
                applyMethod = m;
            }
        }
        return applyMethod;
    }

    public static StructuredGraph getOptimizedGraalIRLambda(Class<?> function) {
        Method applyMethod = getLambdaMethod(function);
        ResolvedJavaMethod lambdaResolvedJavaMethod = getControlFlowGraphLambda(applyMethod);
        StructuredGraph graph = optimiseGraph(lambdaResolvedJavaMethod);
        return graph;
    }

    public static StructuredGraph createCFGGraalIR(ResolvedJavaMethod method) {
        StructuredGraph graph = new StructuredGraph(method, AllowAssumptions.YES);
        GraalOCLBackendConnector.apply(graph);
        return graph;
    }

    public static ResolvedJavaMethod getResolvedJavaMethodForUserFunction(Class<?> function) {
        Method applyMethod = getLambdaMethod(function);
        ResolvedJavaMethod lambdaResolvedJavaMethod = getControlFlowGraphLambda(applyMethod);
        return lambdaResolvedJavaMethod;
    }

    public static StructuredGraph getGraalIRForMethod(ParallelSkeleton skeleton) {
        return getGraalIRForParallelSkeleton(skeleton);
    }

    public static StructuredGraph getGraalIRForParallelSkeleton(ParallelSkeleton skeleton) {
        Method method = FunctionalPatternTemplate.getMethod(skeleton);
        Backend backend = getBackend();
        Providers providers = backend.getProviders();
        Plugins plugins = new Plugins(new InvocationPlugins(providers.getMetaAccess()));
        StructuredGraph graph = new StructuredGraph(((HotSpotMetaAccessProvider) providers.getMetaAccess()).lookupJavaMethod(method), AllowAssumptions.YES, null);
        new GraphBuilderPhase.Instance(providers.getMetaAccess(), providers.getStampProvider(), null, GraphBuilderConfiguration.getEagerDefault(plugins), OptimisticOptimizations.ALL, null).apply(graph);
        return graph;
    }

    public static LambdaFunctionMetadata createMetadata(ParallelSkeleton type) {
        LambdaFunctionMetadata metadata = null;
        if (type == ParallelSkeleton.MAP) {
            metadata = new LambdaFunctionMetadata(LambdaFunctionMetadata.TypeOfFunction.FUNCTION);
        } else if (type == ParallelSkeleton.REDUCE) {
            metadata = new LambdaFunctionMetadata(LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION);
        }
        return metadata;
    }

    private static GPUParameters createGPUParameters(Object[] lambdaInputParameters, Object[] outputMetadata) {
        GPUParameters paramsGPU = new GPUParameters();
        paramsGPU.putInput(lambdaInputParameters);
        paramsGPU.setOutput(outputMetadata);
        return paramsGPU;
    }

    public static void generateOffloadOpenCLKernel(StructuredGraph graphTemplate, StructuredGraph graphLamda, Class<?> klass, Object[] outputMetadata, AcceleratorOCLInfo typeInfoOCL,
                    ParallelSkeleton type,
                    UUID uuidKernel, Object[] lambdaInputParameters, boolean isTruffleCode, ArrayList<Node> scopedNodes, int inputArgs) throws KernelOffloadException {

        LambdaFunctionMetadata oclmetadata = createMetadata(type);
        int convertionSoA = typeInfoOCL.getClassInput().getType().getNumAttributes();
        ParallelOptions.UseSoAWithValue.setValue(convertionSoA);
        GPUParameters paramsGPU = createGPUParameters(lambdaInputParameters, outputMetadata);
        GraalOpenCLRuntime.generateOffloadKernel(graphTemplate, graphLamda, klass, oclmetadata, typeInfoOCL, uuidKernel, paramsGPU.getParameters(), isTruffleCode, scopedNodes, inputArgs);
    }
}
