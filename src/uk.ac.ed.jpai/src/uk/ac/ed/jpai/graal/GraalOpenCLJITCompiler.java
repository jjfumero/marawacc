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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.ParallelSkeleton;
import uk.ac.ed.accelerator.ocl.GraalOpenCLRuntime;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.GraalIRConversion;
import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.compiler.utils.CompilationUtils;
import uk.ac.ed.compiler.utils.JITGraalCompilerUtil;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.interop.Interoperable;
import uk.ac.ed.jpai.DataTypeAPIHelper;
import uk.ac.ed.jpai.cache.GraphCache;
import uk.ac.ed.marawacc.graal.CompilerUtils;
import uk.ac.ed.marawacc.graal.GraalOCLBackendConnector;

import com.oracle.graal.graph.Node;
import com.oracle.graal.java.BytecodeDisassembler;
import com.oracle.graal.nodes.FixedGuardNode;
import com.oracle.graal.nodes.FixedWithNextNode;
import com.oracle.graal.nodes.PiNode;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.calc.IntegerLessThanNode;
import com.oracle.graal.nodes.calc.IsNullNode;
import com.oracle.graal.nodes.calc.NarrowNode;
import com.oracle.graal.nodes.java.CheckCastNode;
import com.oracle.graal.nodes.java.InstanceOfNode;
import com.oracle.graal.nodes.java.LoadFieldNode;
import com.oracle.graal.nodes.java.LoadIndexedNode;
import com.oracle.graal.phases.common.CanonicalizerPhase;
import com.oracle.graal.phases.common.DeadCodeEliminationPhase;
import com.oracle.graal.phases.tiers.PhaseContext;
import com.oracle.graal.phases.util.Providers;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.KnownType;

public class GraalOpenCLJITCompiler {

    public static void debugMethod(InstalledCode code, ResolvedJavaMethod resolvedJavaMethod) {
        System.out.println("[MARAWACC] Compilation name: " + code.getName());
        System.out.println("[MARAWACC] Compilation code: " + code.getCode());
        System.out.println(new BytecodeDisassembler().disassemble(resolvedJavaMethod));
    }

    public static <T, R> GraalOpenCLCompilationUnit inferTypes(PArray<T> input, StructuredGraph graph) {

        RuntimeObjectTypeInfo inputType = TypeFactory.inferFromObject(input.get(0));
        RuntimeObjectTypeInfo outputType = null;

        JITGraalCompilerUtil compiler = new JITGraalCompilerUtil();
        ResolvedJavaMethod resolvedJavaMethod = graph.method();
        InstalledCode code = compiler.compile(resolvedJavaMethod);
        Object executeVarargs = null;
        try {
            executeVarargs = code.executeVarargs(input.get(0));
            debugMethod(code, resolvedJavaMethod);
        } catch (InvalidInstalledCodeException e) {
            e.printStackTrace();
        }
        outputType = TypeFactory.inferFromObject(executeVarargs);
        return new GraalOpenCLCompilationUnit(inputType, outputType, graph);
    }

    private static RuntimeObjectTypeInfo createOutputTypeFromInteropObject(Interoperable interoperable) {
        // Create String to return
        String[] klassArray = interoperable.getInterop().getTupleClass().toString().split("\\.");
        // last index access
        String klass = klassArray[klassArray.length - 1];
        StringBuffer subTypes = new StringBuffer();
        subTypes.append(klass + "<");
        Class<?>[] arraySubType = interoperable.getClassObjects();
        for (Class<?> c : arraySubType) {
            String[] type = c.toString().split("\\.");
            String t = type[type.length - 1];
            subTypes.append(t + ",");
        }
        subTypes.replace(subTypes.length() - 1, subTypes.length(), ">");
        RuntimeObjectTypeInfo outputType = TypeFactory.Tuple(subTypes.toString());
        return outputType;
    }

    public static <T, R> GraalOpenCLCompilationUnit inferTypes(PArray<T> input, StructuredGraph graph, Object firstOutputValue, Interoperable interoperable) {

        RuntimeObjectTypeInfo inputType = TypeFactory.inferFromObject(input.get(0));
        RuntimeObjectTypeInfo outputType = null;

        // JITGraalCompilerUtil compiler = new JITGraalCompilerUtil();
        // ResolvedJavaMethod resolvedJavaMethod = graph.method();
        // InstalledCode code = compiler.compile(resolvedJavaMethod);

        if (interoperable == null) {
            Object executeVarargs = firstOutputValue;
            outputType = TypeFactory.inferFromObject(executeVarargs);
        } else {
            outputType = createOutputTypeFromInteropObject(interoperable);
        }
        return new GraalOpenCLCompilationUnit(inputType, outputType, graph);
    }

    private static void debug(StructuredGraph optimalGraph) {
        // Dump if GPU Debug Enabled
        CompilationUtils.dumpGraph(optimalGraph, "GraphToCompileIntoGPU");
        System.out.println("[ASTx] #NODES: " + optimalGraph.getNodeCount());
        for (Node node : optimalGraph.getNodes()) {
            System.out.println(node);
            for (Node n : node.cfgSuccessors()) {
                System.out.println("\t" + n);
            }
        }
    }

    public static class DataDependencyVisitor {
        private StructuredGraph graph;
        private HashSet<Node> visited;
        private HashSet<Node> dependencies;
        private ArrayList<Node> nullNodes;
        private ArrayList<Node> fixedGuardNodes;
        private ArrayList<Node> instanceOfNodes;
        private ArrayList<Node> checkCastNodes;
        private ArrayList<Node> conditions;
        private ArrayList<Node> piNodes;
        private ArrayList<ValueNode> annotations;
        private StringBuffer debug = new StringBuffer();

        public DataDependencyVisitor(StructuredGraph graph) {
            this.graph = graph;
            visited = new HashSet<>();
            dependencies = new HashSet<>();
            nullNodes = new ArrayList<>();
            fixedGuardNodes = new ArrayList<>();
            instanceOfNodes = new ArrayList<>();
            checkCastNodes = new ArrayList<>();
            conditions = new ArrayList<>();
            piNodes = new ArrayList<>();
            annotations = new ArrayList<>();
        }

        public void emitDebug(String s) {
            debug.append(s + "\n");
        }

        public void run() {
            for (Node node : graph.getNodes()) {
                debug.append("\tvisiting: " + node);
                this.dispatch(node);
            }
            annotateInputNodes();
        }

        public void annotateInputNodes() {
            for (ValueNode n : annotations) {
                n.setAnnotation(KnownType.class);
            }
        }

        public void dispatch(Node node) {
            if (!visited.contains(node)) {
                try {
                    Method m = this.getClass().getMethod("visit", new Class[]{node.getClass()});
                    m.invoke(this, node);
                } catch (NoSuchMethodException e) {
                    this.visit(node);
                } catch (SecurityException e) {
                } catch (IllegalAccessException e) {
                } catch (IllegalArgumentException e) {
                } catch (InvocationTargetException e) {
                }
            }
        }

        /**
         * We check from the {@link LoadIndexedNode} because this is the VirtualFrame (
         * FrameWithoutBoxing annotation after the Partial Evaluation.
         *
         * @param node
         */
        public void visit(LoadIndexedNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (node.getAnnotation() == KnownType.class) {
                dependencies.add(node);
            } else if (!visited.contains(node.array())) {
                this.dispatch(node.array());
            }

            if (dependencies.contains(node.array())) {
                dependencies.add(node);
                // We add the annotation to this node
                node.setAnnotation(KnownType.class);
                annotations.add(node);
            }

        }

        public void visit(LoadFieldNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (!visited.contains(node.object())) {
                this.dispatch(node.object());
            }

            if (dependencies.contains(node.object())) {
                dependencies.add(node);
                node.setAnnotation(KnownType.class);
                annotations.add(node);
            }
        }

        public void visit(IsNullNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (!visited.contains(node.getValue())) {
                this.dispatch(node.getValue());
            }

            if (dependencies.contains(node.getValue())) {
                dependencies.add(node);
                nullNodes.add(node);
            }

        }

        public void visit(FixedGuardNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (!visited.contains(node.getCondition())) {
                this.dispatch(node.getCondition());
            }

            if (dependencies.contains(node.getCondition())) {
                dependencies.add(node);
                fixedGuardNodes.add(node);

                if ((node.getCondition().getClass() != InstanceOfNode.class)) {
                    if ((node.getCondition().getClass() != IsNullNode.class)) {
                        conditions.add(node.getCondition());
                    }
                }
            }

        }

        public void visit(InstanceOfNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (!visited.contains(node.getValue())) {
                this.dispatch(node.getValue());
            }
            if (dependencies.contains(node.getValue())) {
                dependencies.add(node);
                instanceOfNodes.add(node);
            }

        }

        public void visit(CheckCastNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (!visited.contains(node.object())) {
                this.dispatch(node.object());
            }
            if (dependencies.contains(node.object())) {
                dependencies.add(node);
                checkCastNodes.add(node);
            }

        }

        public void visit(NarrowNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (!visited.contains(node.getValue())) {
                this.dispatch(node.getValue());
            }
            if (dependencies.contains(node.getValue())) {
                dependencies.add(node);
                conditions.add(node);
            }
        }

        public void visit(PiNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);

            if (!visited.contains(node.object())) {
                this.dispatch(node.object());
            }

            if (!visited.contains(node.getGuard())) {
                this.dispatch((FixedGuardNode) node.getGuard());
            }

            if (dependencies.contains(node.object())) {
                dependencies.add(node);
                piNodes.add(node);
            }

            if (dependencies.contains(node.getGuard())) {
                dependencies.add(node);
                if (!piNodes.contains(node)) {
                    piNodes.add(node);
                }
            }
        }

        public void visit(IntegerLessThanNode node) {
            emitDebug("\tvisiting: " + node);
            visited.add(node);
            if (!visited.contains(node.getY())) {
                this.dispatch(node.getY());
            }

            if (!visited.contains(node.getX())) {
                this.dispatch(node.getX());
            }

            if (dependencies.contains(node.getY())) {
                dependencies.add(node);
            }

            if (dependencies.contains(node.getX())) {
                dependencies.add(node);
            }
        }

        private void visit(Node node) {
            visited.add(node);
            emitDebug("\tvisiting: " + node);
        }

        public HashSet<Node> getDependencies() {
            return this.dependencies;
        }

        public ArrayList<Node> getIsNullNodes() {
            return nullNodes;
        }

        public ArrayList<Node> getInstanceOfNodes() {
            return instanceOfNodes;
        }

        public ArrayList<Node> getFixedGuardNodes() {
            return fixedGuardNodes;
        }

        public ArrayList<Node> getCheckCastNodes() {
            return checkCastNodes;
        }
    }

    private static void deadCodeElimination(StructuredGraph graph) {
        Providers providers = GraalOCLBackendConnector.getProviders();
        new CanonicalizerPhase().apply(graph, new PhaseContext(providers));
        new DeadCodeEliminationPhase().apply(graph);
    }

    private static void removeNodes(@SuppressWarnings("unused") StructuredGraph graph, ArrayList<Node> listNodes) {
        for (Node n : listNodes) {
            n.replaceAtUsages(null);
            n.safeDelete();
        }
    }

    @SuppressWarnings("unused")
    private static void removeCheckCast(StructuredGraph graph, ArrayList<Node> nodesCheckCast) {
        for (Node n : nodesCheckCast) {
            graph.replaceFixed((FixedWithNextNode) n, n.predecessor());
        }
    }

    @SuppressWarnings("unused")
    private static void removeDeopt(StructuredGraph graph, ArrayList<Node> nodesDeopt) {
        for (Node n : nodesDeopt) {
            n.replaceAtUsages(null);
            graph.removeFixed((FixedWithNextNode) n);
        }
    }

    private static void cleanPhases(StructuredGraph optimalGraph, DataDependencyVisitor visitor) {
        // Clean annotations inference
        if (!visitor.dependencies.isEmpty()) {
            removeNodes(optimalGraph, visitor.nullNodes);
            CompilerUtils.dumpGraph(optimalGraph, "A");
            deadCodeElimination(optimalGraph);
            removeNodes(optimalGraph, visitor.instanceOfNodes);
            CompilerUtils.dumpGraph(optimalGraph, "C");

            // removeCheckCast(optimalGraph, visitor.checkCastNodes);
            // CompilerUtils.dumpGraph(optimalGraph, "D");
            //
            // System.out.println(visitor.conditions);
            // removeNodes(optimalGraph, visitor.conditions);
            // CompilerUtils.dumpGraph(optimalGraph, "B");
            //
            // for (Node n : visitor.piNodes) {
            // if (n.getId() < 0) {
            // visitor.piNodes.remove(n);
            // }
            // }
            //
            // System.out.println(visitor.piNodes);
            // removeNodes(optimalGraph, visitor.piNodes);
            // CompilerUtils.dumpGraph(optimalGraph, "PINODES");
            //
            // System.out.println(visitor.fixedGuardNodes);
            // removeDeopt(optimalGraph, visitor.fixedGuardNodes);
            // CompilerUtils.dumpGraph(optimalGraph, "E");
            // deadCodeElimination(optimalGraph);
        }

        // CompilerUtils.dumpGraph(optimalGraph, "AfterCleaningPhases");
    }

    public static <T, R> GraalOpenCLCompilationUnit compileGraphToOpenCL(PArray<T> input, StructuredGraph graphLambda, boolean replacements, CallTarget callTarget, Object firstValue,
                    boolean isTruffleCode, Interoperable interoperable, Object[] scopeArrays, ArrayList<Node> scopeNodes, int nArgs) {

        long start = System.nanoTime();

        // StructuredGraph optimalGraph = (StructuredGraph) graphLambda.copy();
        StructuredGraph optimalGraph = graphLambda;

        if (GraalAcceleratorOptions.dumpGraalIR) {
            CompilationUtils.dumpGraph(optimalGraph, "Before OpenCL Optimizations");
        }

        if (!replacements) {
            optimalGraph = GraalIRConversion.optimisticOptimisationsLambda(optimalGraph, false);
        } else {
            optimalGraph = GraalIRConversion.optimisticOptimisationsLambda(optimalGraph, true);
            if (!GraalIRConversion.isOCLMathIntrinsic(graphLambda)) {
                optimalGraph = GraalIRConversion.optimisticOptimisationsLambda(optimalGraph, false);
            }
        }

        if (GraalAcceleratorOptions.dumpGraalIR) {
            debug(optimalGraph);
        }

        // CompilerUtils.dumpGraph(optimalGraph, "FinalGraphOpenCL");

        // Data dependencies for annotations
        DataDependencyVisitor visitor = new DataDependencyVisitor(optimalGraph);
        visitor.run();
        cleanPhases(optimalGraph, visitor);

        StructuredGraph skeletonGraph = GraalIRConversion.getGraalIRForMethod(ParallelSkeleton.MAP);

        // Create a new UUID for the Control Flow Graph
        UUID uuidLambda = GraphCache.INSTANCE.insertGraph(graphLambda);

        GraalOpenCLCompilationUnit gpuCompilationUnit = null;
        if (callTarget != null) {
            gpuCompilationUnit = inferTypes(input, optimalGraph, firstValue, interoperable);
        } else {
            gpuCompilationUnit = inferTypes(input, optimalGraph);
        }

        RuntimeObjectTypeInfo outputType = gpuCompilationUnit.getOuputType();
        RuntimeObjectTypeInfo inputType = gpuCompilationUnit.getInputType();

        if (interoperable != null) {
            outputType.setInterop(interoperable.getInterop());
        }

        Object[] outputMetadata = null;

        try {
            if (interoperable != null) {
                outputMetadata = DataTypeAPIHelper.createOneOutputElement(interoperable.getInterop());
            } else {
                outputMetadata = DataTypeAPIHelper.createOneOutputElement(outputType.getClassObject());
            }
        } catch (Exception e) {
            throw new RuntimeException("Compilation OpenCL Kernel Exception");
        }

        if (outputMetadata != null) {
            try {
                Object[] scope = scopeArrays;
                if (scopeArrays == null) {
                    scope = new Object[]{};
                }

                AcceleratorOCLInfo acceleratorOCLInfo = new AcceleratorOCLInfo(inputType.getNestedTypesOrSelf(), outputType.getNestedTypesOrSelf(), scope);

                int size = 1 + scope.length;
                Object[] lambdaParameters = new Object[size];
                lambdaParameters[0] = input;
                for (int i = 0; i < scope.length; i++) {
                    lambdaParameters[i + 1] = scope[i];
                }

                GraalIRConversion.generateOffloadOpenCLKernel(skeletonGraph, optimalGraph, inputType.getClass(), outputMetadata, acceleratorOCLInfo, ParallelSkeleton.MAP, uuidLambda,
                                lambdaParameters, isTruffleCode, scopeNodes, nArgs);
                GraalOpenCLRuntime.compileOpenCLKernelAndInstallBinaryWithDriver(uuidLambda);

            } catch (Exception e) {
                // launch exception for deoptimization
                e.printStackTrace();
                throw new RuntimeException("Compilation OpenCL Kernel Exception");
            }

            long total = System.nanoTime() - start;
            if (GraalAcceleratorOptions.profileOffload) {
                Profiler.getInstance().writeInBuffer(ProfilerType.Graal_OpenCL_Code_Generation, "total", total);
            }

            gpuCompilationUnit.setScopeArrays(scopeArrays);
            gpuCompilationUnit.setScopeNodes(scopeNodes);

            return gpuCompilationUnit;
        } else {
            return null;
        }
    }

    public static <T, R> GraalOpenCLCompilationUnit compileGraphToOpenCL(PArray<T> input, StructuredGraph graphLambda) {
        return compileGraphToOpenCL(input, graphLambda, true, null, null, false, null, null, null, 0);
    }

    public static <T, R> GraalOpenCLCompilationUnit compileGraphToOpenCL(PArray<T> input, StructuredGraph graphLambda, CallTarget callTarget, Object firstValue, boolean isTruffleCode,
                    Interoperable interoperable, Object[] scopeArrays, ArrayList<Node> scopeNodes, int nArgs) {
        return compileGraphToOpenCL(input, graphLambda, false, callTarget, firstValue, isTruffleCode, interoperable, scopeArrays, scopeNodes, nArgs);
    }
}
