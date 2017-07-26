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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.Vector;

import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;
import jdk.vm.ci.runtime.JVMCI;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.ocl.ExtraArrayNamesManager.ArrayInfo;
import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.ocl.helper.TypeUtil;
import uk.ac.ed.accelerator.ocl.krnonos.OpenCLExtension;
import uk.ac.ed.accelerator.ocl.phases.ArrayDepth;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType.DataType;
import uk.ac.ed.accelerator.ocl.runtime.GPUParameters;
import uk.ac.ed.accelerator.ocl.runtime.KernelOffloadException;
import uk.ac.ed.accelerator.ocl.runtime.MapToGraal;
import uk.ac.ed.accelerator.ocl.runtime.ScopeTruffle;
import uk.ac.ed.accelerator.ocl.scope.PArrayScopeManager;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata.TypeOfFunction;
import uk.ac.ed.accelerator.wocl.OCLDeviceInfo;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.replacements.ocl.OCLMathIntrinsicNode;

import com.oracle.graal.api.runtime.GraalJVMCICompiler;
import com.oracle.graal.compiler.common.spi.ForeignCallDescriptor;
import com.oracle.graal.compiler.common.type.ObjectStamp;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.NodeInputList;
import com.oracle.graal.graph.NodePosIterator;
import com.oracle.graal.hotspot.nodes.CompressionNode;
import com.oracle.graal.hotspot.nodes.CurrentJavaThreadNode;
import com.oracle.graal.hotspot.replacements.arraycopy.ArrayCopyNode;
import com.oracle.graal.loop.LoopEx;
import com.oracle.graal.loop.LoopsData;
import com.oracle.graal.nodeinfo.Verbosity;
import com.oracle.graal.nodes.AbstractBeginNode;
import com.oracle.graal.nodes.BeginNode;
import com.oracle.graal.nodes.ConstantNode;
import com.oracle.graal.nodes.DeoptimizeNode;
import com.oracle.graal.nodes.DynamicDeoptimizeNode;
import com.oracle.graal.nodes.EndNode;
import com.oracle.graal.nodes.FixedGuardNode;
import com.oracle.graal.nodes.IfNode;
import com.oracle.graal.nodes.InvokeNode;
import com.oracle.graal.nodes.InvokeWithExceptionNode;
import com.oracle.graal.nodes.KillingBeginNode;
import com.oracle.graal.nodes.LogicConstantNode;
import com.oracle.graal.nodes.LogicNode;
import com.oracle.graal.nodes.LoopBeginNode;
import com.oracle.graal.nodes.LoopEndNode;
import com.oracle.graal.nodes.LoopExitNode;
import com.oracle.graal.nodes.MergeNode;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.PhiNode;
import com.oracle.graal.nodes.PiArrayNode;
import com.oracle.graal.nodes.PiNode;
import com.oracle.graal.nodes.ReturnNode;
import com.oracle.graal.nodes.StartNode;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.ValuePhiNode;
import com.oracle.graal.nodes.ValueProxyNode;
import com.oracle.graal.nodes.calc.AbsNode;
import com.oracle.graal.nodes.calc.AddNode;
import com.oracle.graal.nodes.calc.AndNode;
import com.oracle.graal.nodes.calc.ConditionalNode;
import com.oracle.graal.nodes.calc.DivNode;
import com.oracle.graal.nodes.calc.FloatConvertNode;
import com.oracle.graal.nodes.calc.FloatEqualsNode;
import com.oracle.graal.nodes.calc.FloatLessThanNode;
import com.oracle.graal.nodes.calc.IntegerBelowNode;
import com.oracle.graal.nodes.calc.IntegerDivNode;
import com.oracle.graal.nodes.calc.IntegerEqualsNode;
import com.oracle.graal.nodes.calc.IntegerLessThanNode;
import com.oracle.graal.nodes.calc.IntegerRemNode;
import com.oracle.graal.nodes.calc.IntegerTestNode;
import com.oracle.graal.nodes.calc.IsNullNode;
import com.oracle.graal.nodes.calc.LeftShiftNode;
import com.oracle.graal.nodes.calc.MulNode;
import com.oracle.graal.nodes.calc.NarrowNode;
import com.oracle.graal.nodes.calc.NegateNode;
import com.oracle.graal.nodes.calc.NormalizeCompareNode;
import com.oracle.graal.nodes.calc.ObjectEqualsNode;
import com.oracle.graal.nodes.calc.OrNode;
import com.oracle.graal.nodes.calc.ReinterpretNode;
import com.oracle.graal.nodes.calc.RightShiftNode;
import com.oracle.graal.nodes.calc.SignExtendNode;
import com.oracle.graal.nodes.calc.SqrtNode;
import com.oracle.graal.nodes.calc.SubNode;
import com.oracle.graal.nodes.calc.UnsignedRightShiftNode;
import com.oracle.graal.nodes.calc.XorNode;
import com.oracle.graal.nodes.calc.ZeroExtendNode;
import com.oracle.graal.nodes.extended.BoxNode;
import com.oracle.graal.nodes.extended.ForeignCallNode;
import com.oracle.graal.nodes.extended.IntegerSwitchNode;
import com.oracle.graal.nodes.extended.JavaReadNode;
import com.oracle.graal.nodes.extended.NullCheckNode;
import com.oracle.graal.nodes.extended.UnboxNode;
import com.oracle.graal.nodes.extended.UnsafeLoadNode;
import com.oracle.graal.nodes.java.ArrayLengthNode;
import com.oracle.graal.nodes.java.CheckCastNode;
import com.oracle.graal.nodes.java.InstanceOfNode;
import com.oracle.graal.nodes.java.LoadFieldNode;
import com.oracle.graal.nodes.java.LoadIndexedNode;
import com.oracle.graal.nodes.java.MethodCallTargetNode;
import com.oracle.graal.nodes.java.NewArrayNode;
import com.oracle.graal.nodes.java.NewInstanceNode;
import com.oracle.graal.nodes.java.StoreFieldNode;
import com.oracle.graal.nodes.java.StoreIndexedNode;
import com.oracle.graal.nodes.memory.FloatingReadNode;
import com.oracle.graal.nodes.memory.ReadNode;
import com.oracle.graal.nodes.memory.address.AddressNode;
import com.oracle.graal.nodes.memory.address.OffsetAddressNode;
import com.oracle.graal.nodes.virtual.AllocatedObjectNode;
import com.oracle.graal.nodes.virtual.CommitAllocationNode;
import com.oracle.graal.nodes.virtual.VirtualArrayNode;
import com.oracle.graal.nodes.virtual.VirtualInstanceNode;
import com.oracle.graal.nodes.virtual.VirtualObjectNode;
import com.oracle.graal.phases.Phase;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.replacements.amd64.AMD64MathIntrinsicNode;
import com.oracle.graal.replacements.amd64.AMD64MathIntrinsicNode.Operation;
import com.oracle.graal.replacements.nodes.arithmetic.IntegerAddExactNode;
import com.oracle.graal.replacements.nodes.arithmetic.IntegerMulExactNode;
import com.oracle.graal.replacements.nodes.arithmetic.IntegerSubExactNode;
import com.oracle.graal.runtime.RuntimeProvider;
import com.oracle.truffle.api.CompilerDirectives.KnownType;

/**
 * Main GraalIR To OpenCL code generator. This corresponds to the Map Parallel skeleton.
 */
public class GraalOpenCLGenerator extends AbstractOpenCLGenerator {

    private CodeBuffer buffer;
    private SymbolTable symbolTable;
    private String kernelName;
    private LoopsData loopsData;
    private ParameterNode lastParam;    // Use only in the case of lambda expression
    private Map<Direction, Object[]> parametersDirection;
    private StructuredGraph graphLambda;
    private int numExtraArgs;
    private boolean isGeneratingFunction;
    private Map<Node, String> references;
    private boolean isTupleEnabled = false;
    private boolean isTupleEnabledSignature = false;

    private Map<String, String> objectSymbolTable;
    private Map<String, String> generatedFunctions;

    private ObjectsManagement tuplesManagement;
    private ObjectsManagement objectOutputManagement;
    private HashMap<String, HashMap<String, String>> globalObjectTable;
    private LambdaFunctionMetadata metadata;
    private AcceleratorType[] tuplesInputDataTypes;
    private AcceleratorType[] tuplesOutputDataTypes;
    private AcceleratorOCLInfo typeInfoOCL;
    private AcceleratorType outputDataType;
    private Set<Node> returnNodes;
    private boolean generateExtraArrayInFunctionSignature = false;
    private int lastIndexParam = 0;
    private ArrayList<String> outputVariableNames;
    private String structTohide;
    private static final int MAX_NUM_TUPLES = 11;
    private boolean simpleDataTypeLambdaParameter;

    private Deque<CodeBuffer> nestedFunctions;
    private Set<String> structTypedefs;

    private static final String NESTED_DATA = "NESTED_DATA_x12398Q1?";

    private UUID uuid;

    private Stack<Boolean> explicitLoop;

    private HashMap<String, String> structType;
    private AcceleratorType[] oclScope;

    // Scope From Truffle
    private ArrayList<ScopeTruffle> scopeTruffleList;
    private ArrayList<Node> scopedNodes;

    private String returnTypeFunction;

    // ConstantNodes as scope arrays
    private ArrayList<ConstantNode> arrayConstantNodes;
    private HashMap<ConstantNode, Integer> arrayConstantIndex;
    private int inputArgs;

    private static final int R_START_PARAM_INDEX = 9;
    private static final int R_INDEX_DEPTH = 6;
    private static final int R_INDEX_IS_IRREGULAR = 7;

    private static final int RUBY_INDEX_START = 6;

    // Input Alias List for Ruby Inputs
    private ArrayList<String> inputAliasList = new ArrayList<>();

    /**
     * Simple fall back (deopt) flag in global memory.
     */
    private static final String DEOPT_BUFFER_SIGNATURE_TEMPLATE = "__global int* deoptFlag";
    @SuppressWarnings("unused") private static final String DEOPT_BUFFER_SET = "deoptFlag[0] = 1;";
    private static final String DEOPT_BUFFER_INITIALIZE = "deoptFlag[0] = 0;";
    private static final String DEOPT_BUFFER_PARAM = "deoptFlag";

    public GraalOpenCLGenerator(boolean commentsEnabled, SymbolTable symbolTable) {
        this.buffer = new CodeBuffer(commentsEnabled);
        this.references = new HashMap<>();
        this.symbolTable = symbolTable;
        this.parametersDirection = null;
        this.returnNodes = new HashSet<>();
        this.outputVariableNames = new ArrayList<>();
        this.nestedFunctions = new LinkedList<>();
        this.structTypedefs = new HashSet<>();
        this.explicitLoop = new Stack<>();
        this.structType = new HashMap<>();
        this.scopedNodes = new ArrayList<>();
        this.arrayConstantIndex = new HashMap<>();

        initializeObjectsTable();
        initializeObjectsTablePArray();
        initializeGeneratedFunctions();
    }

    public GraalOpenCLGenerator(Boolean value, SymbolTable table, UUID uuidKernel) {
        this(value, table);
        this.uuid = uuidKernel;
    }

    private void initializeGeneratedFunctions() {

        // Method for insertion of
        // Tuple1._1 : 1
        // Tuple2._1 : 1
        // Tuple2._2 : 2
        // and so on

        if (generatedFunctions == null) {
            this.generatedFunctions = new HashMap<>();
        }

        String fieldName = "._";
        String tupleName = "Tuple";
        for (int i = 1; i <= MAX_NUM_TUPLES; i++) {
            for (int j = 1; j <= i; j++) {
                String tupleAndFieldName = tupleName + i + fieldName + j;
                this.generatedFunctions.put(tupleAndFieldName, new Integer(j).toString());
            }
        }
    }

    private void initializeObjectsTable() {

        if (objectSymbolTable == null) {
            objectSymbolTable = new HashMap<>();
        }

        final String classBaseJavaName = "Luk/ac/ed/datastructures/tuples/Tuple";
        final String classBaseName = "class uk.ac.ed.datastructures.tuples.Tuple";
        final String structBaseName = "struct_tuples";

        for (int i = 1; i <= MAX_NUM_TUPLES; i++) {
            String classFullJavaName = classBaseJavaName + i + ";";
            String classFullName = classBaseName + i;
            String structFullName = structBaseName + i;

            // Hash structure:
            //
            // Luk.ac.ed.datastructures.tuples.Tuple1 : struct_tuples1
            // ..
            // class uk.ac.ed.datastructures.tuples.Tuple1 : struct_tuples1

            objectSymbolTable.put(classFullJavaName, structFullName);
            objectSymbolTable.put(classFullName, structFullName);
        }

        for (int i = 1; i <= MAX_NUM_TUPLES; i++) {
            objectSymbolTable.put("interface uk.ac.ed.datastructures.tuples.Tuple" + i, structBaseName + i);
            objectSymbolTable.put(classBaseJavaName + i + "Generic;", structBaseName + i);
            objectSymbolTable.put(classBaseName + i + "Generic", structBaseName + i);
        }
    }

    private void initializeObjectsTablePArray() {

        if (objectSymbolTable == null) {
            this.objectSymbolTable = new HashMap<>();
        }

        final String classBaseJavaName = "Luk/ac/ed/datastructures/common/PArray;";
        final String classBaseName = "class uk.ac.ed.datastructures.common.PArray";
        final String structBaseName = "struct_tuples";

        objectSymbolTable.put(classBaseJavaName, structBaseName + "1");
        objectSymbolTable.put(classBaseName, structBaseName + "1");

        objectSymbolTable.put(classBaseJavaName, NESTED_DATA);
        objectSymbolTable.put(classBaseName, NESTED_DATA);
    }

    @Override
    public boolean isExtraArrayforFunction() {
        return this.generateExtraArrayInFunctionSignature;
    }

    @Override
    public void setExtraArrayforFunction(boolean condition) {
        this.generateExtraArrayInFunctionSignature = condition;
    }

    @Override
    public void setParametersDirection(Map<Direction, Object[]> parameters) {
        this.parametersDirection = parameters;
    }

    @Override
    public void setLastParamLambda(ParameterNode paramNode) {
        this.lastParam = paramNode;
    }

    @Override
    public void setLambdaGraph(StructuredGraph lambda) {
        this.graphLambda = lambda;
    }

    @Override
    public void addInitCode(StructuredGraph sg) {
        // add 1D dimension access variables.
        for (ParameterNode paramNode : sg.getNodes(ParameterNode.TYPE)) {
            if (ParallelOptions.UseFunctionalJPAIGPU.getValue() && (paramNode == this.lastParam)) {
                String name = symbolTable.lookupName(paramNode);
                String localVariable = symbolTable.newVariable(SymbolTable.PARAM_VALUE);
                symbolTable.add(localVariable, paramNode, paramNode.getStackKind());
                // buffer.emitString(name + " = get_global_id(0);");
                buffer.emitCode(paramNode.getStackKind().getJavaName() + " " + localVariable + " = get_global_id(0);");
                buffer.emitCode("if ( get_global_id(0) < " + name + ") {");
                symbolTable.enterScope();
                buffer.beginBlock();
            }
            try {
                if (!paramNode.getStackKind().isPrimitive() && ((ObjectStamp) paramNode.stamp()).type().isArray()) {
                    String name = symbolTable.lookupName(paramNode);
                    buffer.emitCode("int " + name + "_dim_1 = 0;");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addLocalVariables(StructuredGraph graph) {
        for (Node n : graph.getNodes()) {
            if (n instanceof PhiNode) {
                PhiNode phiNode = (PhiNode) n;
                if (phiNode.getStackKind().isPrimitive()) {
                    String localVarName = symbolTable.newVariable(SymbolTable.PHI_VAR);
                    symbolTable.add(localVarName, phiNode, phiNode.getStackKind());
                    buffer.emitCode(phiNode.getStackKind().getJavaName() + " " + localVarName + "; ");
                }
            }
        }
    }

    @Override
    public String includeKhronosPragmas() {
        StringBuffer pragmas = new StringBuffer();
        // Enabled pragmas we would like to have for the target architecture.
        // FP_64 bits by now
        pragmas.append("#pragma OPENCL EXTENSION " + OpenCLExtension.cl_khr_fp64 + " : enable\n");
        return pragmas.toString();
    }

    public String includeAllPragmas() {
        StringBuffer pragmas = new StringBuffer();
        pragmas.append("#pragma OPENCL EXTENSION all : enable\n");
        return pragmas.toString();
    }

    @Override
    public String generateKernelSignature(StructuredGraph graph, Class<?> klass, ArrayList<ParameterNode> ioParams) throws KernelOffloadException {
        StringBuffer kernelSignature = new StringBuffer("(");
        kernelSignature.append(generateParametersSignature(graph, klass, ioParams));
        kernelSignature.append(")");
        return kernelSignature.toString();
    }

    protected String generateKernelSignature(StructuredGraph graph) {
        String kernelSignature = "(";
        kernelSignature += generateParametersSignature(graph);
        return kernelSignature;
    }

    private class VariableParameterInfo {

        private JavaKind kind;
        private String signature;

        // Add a new dimension
        private void resolveWithObject(String type, int counter) {
            if (type.equals("[I")) {
                signature = "__global int *p" + counter + ",__constant int *p" + counter + "_index_data";
                kind = JavaKind.Int;
            } else if (type.equals("[F")) {
                signature = "__global float *p" + counter + ",__constant int *p" + counter + "_index_data";
                kind = JavaKind.Float;
            } else if (type.equals("[D")) {
                signature = "__global double *p" + counter + ",__constant int *p" + counter + "_index_data";
                kind = JavaKind.Double;
            }
        }

        public void resolveNameAndType(String type, int counter, boolean isObject) {
            if (isObject) {
                resolveWithObject(type, counter);
            } else if (type.equals("[I")) {
                signature = "int p" + counter;
                kind = JavaKind.Int;
            } else if (type.equals("[F")) {
                signature = "float p" + counter;
                kind = JavaKind.Float;
            } else if (type.equals("[S")) {
                kind = JavaKind.Short;
                signature = "short p" + counter;
            } else if (type.equals("[J")) {
                signature = "long p" + counter;
                kind = JavaKind.Long;
            } else if (type.equals("[D")) {
                signature = "double p" + counter;
                kind = JavaKind.Double;
            } else if (type.equals("[[I")) {
                signature = "__global int *p" + counter + ",__constant int *p" + counter + "_index_data";
                kind = JavaKind.Int;
            } else if (type.equals("[[F")) {
                signature = "__global float *p" + counter + ",__constant int *p" + counter + "_index_data";
                kind = JavaKind.Float;
            } else if (type.equals("[[D")) {
                signature = "__global double *p" + counter + ",__constant int *p" + counter + "_index_data";
                kind = JavaKind.Double;
            }
        }

        public JavaKind getJavaKind() {
            return kind;
        }

        public String getSignature() {
            return signature;
        }
    }

    @SuppressWarnings("rawtypes")
    private String generateLambdaSignature(ParameterNode paramNode, int counter, boolean isObject) {
        StringBuffer lambdaSignature = new StringBuffer();
        Object[] input = parametersDirection.get(Direction.INPUT);
        String type = "";

        try {
            type = TypeUtil.getTypeFromArrayNonPrimitive(input[0].getClass());
        } catch (Exception e) {
            if (input[0].getClass() == PArray.class) {
                type = TypeUtil.getTypeFromArrayNonPrimitiveClass(((PArray) input[0]).getClassObject());
                if (type != null) {
                    simpleDataTypeLambdaParameter = true;
                }
            } else {
                type = null;
            }
        }

        if (type == null) {
            try {
                type = TypeUtil.getTypeFromArray2DNonPrimitive(input[0].getClass());
            } catch (Exception e) {
                type = null;
            }
        }

        // Assume 1D or 2D. In this point the type should be known
        if (type != null) {
            VariableParameterInfo varInfo = new VariableParameterInfo();
            varInfo.resolveNameAndType(type, counter, isObject);
            symbolTable.add("p" + counter, paramNode, varInfo.getJavaKind());
            lambdaSignature.append(varInfo.getSignature());
        }

        if (!PArrayScopeManager.INSTANCE.hasScope(uuid)) {
            if (isTruffleFrontEnd()) {
                lambdaSignature.append(generateStructSignatureForTruffle(paramNode, counter, tuplesInputDataTypes, ((PArray) input[0]).getClassObject()));
            } else {
                lambdaSignature.append(generateStructSignature(paramNode, counter, tuplesInputDataTypes));
            }
        }
        lambdaSignature.append(",");

        return lambdaSignature.toString();
    }

    private String generateStructSignature(ParameterNode paramNode, int counter, AcceleratorType[] dataType) {
        StringBuffer signature = new StringBuffer();
        String objectType = "class " + ((ObjectStamp) paramNode.stamp()).type().toClassName();

        // Look for in the Object Table (what OpenCL knows how to translate it)

        if (objectSymbolTable.containsKey(objectType)) {
            String typeKind = getTupleName(dataType);
            JavaKind kind = JavaKind.Object;
            if (dataType != null && dataType.length > 1) {
                typeKind = getTupleName(dataType);
            }
            signature.append(typeKind + "  inputStruct" + counter);
            symbolTable.add("inputStruct" + counter, paramNode, kind);
            references.put(paramNode, typeKind);
            inputAliasList.add("inputStruct" + counter);
        }

        return signature.toString();
    }

    private String generateStructSignatureForTruffle(ParameterNode paramNode, int counter, AcceleratorType[] dataType, Class<?> klass) {
        StringBuffer signature = new StringBuffer();
        String objectType = klass.toString();

        // Search in the Object Table (what OpenCL knows how to translate it)
        if (objectSymbolTable.containsKey(objectType)) {
            String typeKind = getTupleName(dataType);
            JavaKind kind = JavaKind.Object;
            if (dataType != null && dataType.length > 1) {
                typeKind = getTupleName(dataType);
            }
            final String structName = " inputStruct";
            signature.append(typeKind + "  inputStruct" + counter);
            symbolTable.add("inputStruct" + counter, paramNode, kind);
            references.put(paramNode, typeKind);
            structType.put("inputStruct" + counter, typeKind);
            inputAliasList.add("inputStruct" + counter);
        }
        return signature.toString();
    }

    private static String checkDataTypeinTuple(int fieldNum, AcceleratorType[] types) {
        if (fieldNum < types.length && types[fieldNum].isValid()) {
            DataType d = types[fieldNum].getType();
            return d.getOCLName();
        } else {
            return null;
        }
    }

    // Generate ParameterNode information for an array.
    private String generateDefaultSignature(ParametersNodeHelper nodeHelper, ParameterNode paramNode, JavaKind typeKind) {
        String kernelSignature = "";
        kernelSignature += "__global " + typeKind.getJavaName() + " *p" + nodeHelper.getParamCount() + ",";
        kernelSignature += "__constant int *" + "p" + nodeHelper.getParamCount() + "_index_data,";
        symbolTable.add("p" + nodeHelper.getParamCount(), paramNode, typeKind);
        nodeHelper.incParamCount();
        return kernelSignature;
    }

    private String generateTupleInput(ParametersNodeHelper nodeHelper, boolean isObject, ParameterNode paramNode) {
        String kernelSignature = "";
        JavaKind kind;

        if ((isObject) && (ParallelOptions.UseSoAWithValue.getValue() != 0)) {
            int expandInput = tuplesInputDataTypes.length;
            kind = JavaKind.Object;
            tuplesManagement = new ObjectsManagement(expandInput);
            for (int i = 0; i < expandInput; i++) {
                String realType = checkDataTypeinTuple(i, tuplesInputDataTypes);
                String starts = "*";

                if (realType == null) {
                    realType = this.tuplesInputDataTypes[i].getArrayDataType().getOCLName();
                }

                kernelSignature += "__global " + realType + " " + starts + " p" + nodeHelper.getParamCount() + ",";

                symbolTable.add("p" + nodeHelper.getParamCount(), paramNode, kind);
                tuplesManagement.insertVariableName("p" + nodeHelper.getParamCount(), true);
                // tuplesManagement.insertAuxData("p" + nodeHelper.getParamCount() + "_index_data");

                // Add to the table of Tuples (Should be a table for objects)
                references.put(paramNode, realType);

                nodeHelper.incParamCount();
            }
            int c = nodeHelper.getParamCount() - 1;
            kernelSignature += "__constant int *" + "p" + c + "_index_data,";
            tuplesManagement.insertAuxData("p" + c + "_index_data");
        }
        return kernelSignature;
    }

    private String generateTupleOutput(ParametersNodeHelper nodeHelper, boolean isObject, ParameterNode paramNode) {
        String kernelSignature = "";
        JavaKind kind;
        // if ((isObject) && (ParallelOptions.UseSoAWithValue.getValue() != 0)) {
        if (isObject) {
            // int unroll = outputDataType.getType().getNumAttributes();
            int unroll = tuplesOutputDataTypes.length;
            kind = JavaKind.Object;
            objectOutputManagement = new ObjectsManagement(unroll);
            for (int i = 0; i < unroll; i++) {
                // String realType =
                // this.tuplesOutputDataTypes.get(i).getArrayDataType().getOCLName();
                String realType = this.tuplesOutputDataTypes[i].getType().getOCLName();
                String starts = "*";

                kernelSignature += "__global " + realType + " " + starts + " p" + nodeHelper.getParamCount() + ",";
                // kernelSignature += "__global int *" + "p" + nodeHelper.getParamCount() +
                // "_index_data,";

                symbolTable.add("p" + nodeHelper.getParamCount(), paramNode, kind);
                objectOutputManagement.insertVariableName("p" + nodeHelper.getParamCount(), false);

                // Create a list with meta information related with the output
                ExtraArrayNamesManager manager = ExtraArrayNamesManager.getInstance();
                ArrayList<String> metaInformation = new ArrayList<>();

                metaInformation.add("p" + nodeHelper.getParamCount());
                metaInformation.add(new Integer(i).toString());
                metaInformation.add(realType);

                manager.addOutput(metaInformation);

                // Add to the table of Tuples (It should be a table for objects)
                references.put(paramNode, realType);
                nodeHelper.incParamCount();
            }
            // int c = nodeHelper.getParamCount() - 1;
            // kernelSignature += "__global int *" + "p" + c + "_index_data,";
        }
        return kernelSignature;
    }

    private String generateParameterForSignature(ParameterNode paramNode, Class<?> klass, ArrayList<ParameterNode> ioParams, ParametersNodeHelper nodeHelper) {

        // This is safe because this is the generator for Lambda expression: pattern
        // kernel(in[], out[])
        // Note: the counter is only incremented when the code generator writes a new variable
        // So: (0) is always input
        // --- (1) is always output

        String kernelSignature = "";
        if (isGeneratingFunction && nodeHelper.isLambdaArgument()) {
            kernelSignature += generateLambdaSignature(paramNode, nodeHelper.getParamCount(), false);
            nodeHelper.incParamCount();
            return kernelSignature;
        } else if (isGeneratingFunction && paramNode.getStackKind().isPrimitive()) {
            // Primitives as shouldn't be passed as pointers to lambda
            kernelSignature += paramNode.getStackKind().getJavaName() + " p" + nodeHelper.getParamCount() + ",";
            symbolTable.add("p" + nodeHelper.getParamCount(), paramNode, paramNode.getStackKind());
            nodeHelper.incParamCount();
            return kernelSignature;
        } else if (isGeneratingFunction && paramNode.getStackKind().isObject()) {
            kernelSignature = generateLambdaSignature(paramNode, nodeHelper.getParamCount(), true);
            nodeHelper.incParamCount();

            if (kernelSignature.equals(",")) {
                JavaKind typeKind = null;
                String type = "F";
                try {
                    typeKind = JavaKind.fromTypeString("" + type.charAt(type.length() - 1));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                assert typeKind != null;

                if ((typeKind != null) && (typeKind.isPrimitive())) {
                    kernelSignature = generateDefaultSignature(nodeHelper, paramNode, typeKind);
                }
            }

            return kernelSignature;
        } else if (paramNode.getStackKind().isPrimitive()) {
            kernelSignature += paramNode.getStackKind().getJavaName() + " p" + nodeHelper.getParamCount() + ",";
            symbolTable.add("p" + nodeHelper.getParamCount(), paramNode, paramNode.getStackKind());
            nodeHelper.incParamCount();
            return kernelSignature;
        } else if (((ObjectStamp) paramNode.stamp()).type().isArray()) {

            // Generic case

            String type = "";
            String objectType = null;
            if (nodeHelper.getParamCount() > 0) {
                try {
                    objectType = parametersDirection.get(Direction.OUTPUT)[0].getClass().getComponentType().toString();
                } catch (NullPointerException e) {
                    objectType = parametersDirection.get(Direction.OUTPUT)[0].getClass().toString();
                }
            }
            if (objectType == null) {
                try {
                    type = ((ObjectStamp) paramNode.stamp()).type().getArrayClass().asExactType().getName();
                } catch (NullPointerException e) {
                    try {
                        type = TypeUtil.getTypeFromArrayNonPrimitive(klass);

                    } catch (NullPointerException e2) {
                        type = null;
                    }
                }
            } else {
                type = null;
            }

            if (type == null) {
                if ((nodeHelper.isTuple())) {
                    isTupleEnabled = true;

                    boolean isJavaObject = false;
                    boolean isOutput = (nodeHelper.getParamCount() == 0) ? false : true;
                    if (!isOutput) {
                        try {
                            objectType = parametersDirection.get(Direction.INPUT)[0].getClass().getComponentType().toString();
                        } catch (NullPointerException e) {
                            objectType = parametersDirection.get(Direction.INPUT)[0].getClass().toString();
                        }
                    } else {
                        try {
                            objectType = parametersDirection.get(Direction.OUTPUT)[0].getClass().getComponentType().toString();
                        } catch (NullPointerException e) {
                            objectType = parametersDirection.get(Direction.OUTPUT)[0].getClass().toString();
                        }
                    }

                    JavaKind kind = null;
                    String typeKind = null;
                    if (objectSymbolTable.containsKey(objectType)) {
                        typeKind = getTupleName(tuplesOutputDataTypes);
                        kind = JavaKind.Object;
                        isJavaObject = true;
                    }

                    if (!isOutput) {
                        kernelSignature += generateTupleInput(nodeHelper, isJavaObject, paramNode);
                        return kernelSignature;
                    } else {

                        if ((tuplesOutputDataTypes != null) && (tuplesOutputDataTypes.length > 1)) {
                            kernelSignature += generateTupleOutput(nodeHelper, isJavaObject, paramNode);
                            return kernelSignature;
                        }
                    }

                    if (typeKind != null) {
                        // if isn't primative but is array and array is of primative type then
                        // convert.
                        kernelSignature += "__global " + typeKind + "  *p" + nodeHelper.getParamCount() + ",";
                        kernelSignature += "__constant int *" + "p" + nodeHelper.getParamCount() + "_index_data,";
                        symbolTable.add("p" + nodeHelper.getParamCount(), paramNode, kind);
                        this.outputVariableNames.add("p" + nodeHelper.getParamCount());
                        this.outputVariableNames.add("p" + nodeHelper.getParamCount() + "_index_data");
                        nodeHelper.incParamCount();

                        // Add to the table of Tuples (Should be a table for objects)
                        references.put(paramNode, typeKind);
                        return kernelSignature;
                    } else {

                        // If it is still null it is likely to be the last parameter
                        kind = JavaKind.Float;   // Float by default
                        typeKind = "float";

                        // ioParams (0) = <T> input in the lambda template
                        // ioParams (1) = <R> output in the lambda template
                        if (paramNode.equals(ioParams.get(1))) {
                            if (outputDataType.getArrayDim() != 0) {
                                typeKind = outputDataType.getArrayNameType();
                                kind = getKindType(outputDataType.getArrayDataType());
                            } else {
                                typeKind = outputDataType.getType().getOCLName();
                                kind = getKindType(outputDataType.getType());
                            }
                        }

                        kernelSignature += "__global " + typeKind + "  *p" + nodeHelper.getParamCount() + ",";
                        // kernelSignature += "__global int *" + "p" + nodeHelper.getParamCount() +
                        // "_index_data,";

                        symbolTable.add("p" + nodeHelper.getParamCount(), paramNode, kind);
                        this.outputVariableNames.add("p" + nodeHelper.getParamCount());
                        this.outputVariableNames.add("p" + nodeHelper.getParamCount() + "_index_data");
                        nodeHelper.incParamCount();
                    }

                } else {
                    // TODO: infer the data type (to check we suppose float)
                    // It is not tuple
                    type = ((ObjectStamp) paramNode.stamp()).type().getArrayClass().asExactType().getName();
                }
            } else {
                type = "F";
            }

            // Prevent false parameter in the signature
            // If type is still null we have to continue with the next parameter
            if (type == null) {
                // Continue with the next parameterNode
                return kernelSignature;
            }

            JavaKind typeKind = null;
            try {
                // TODO: Doesn't work for boxed types
                typeKind = JavaKind.fromTypeString("" + type.charAt(type.length() - 1));
            } catch (Exception e) {
                e.printStackTrace();
            }

            assert typeKind != null;

            if ((typeKind != null) && (typeKind.isPrimitive())) {
                kernelSignature += generateDefaultSignature(nodeHelper, paramNode, typeKind);
            }
        } else if (isGeneratingFunction) {
            // Assuming it's a struct from an outer lambda

            // XXX: REVIEW THE SCOPE
            kernelSignature += generateStructSignature(paramNode, nodeHelper.getParamCount(), typeInfoOCL.getOCLScope());
            kernelSignature += ",";
            nodeHelper.incParamCount();
            return kernelSignature;
        }
        return kernelSignature;
    }

    private static class ParametersNodeHelper {
        private boolean isTuple;
        private boolean isLambdaArgument;
        private int paramCounter;

        public ParametersNodeHelper() {
            this.isTuple = false;
            this.isLambdaArgument = false;
            this.paramCounter = 0;
        }

        public boolean isTuple() {
            return isTuple;
        }

        public void setTuple(boolean isTuple) {
            this.isTuple = isTuple;
        }

        public boolean isLambdaArgument() {
            return isLambdaArgument;
        }

        public void setLambdaArgument(boolean isLambdaArgument) {
            this.isLambdaArgument = isLambdaArgument;
        }

        public int getParamCount() {
            return paramCounter;
        }

        public void incParamCount() {
            this.paramCounter++;
        }

        @Override
        public String toString() {
            return "isTuple: " + isTuple + " :: isLambadArgument: " + isLambdaArgument;
        }
    }

    private String generateMetadataForScopeVarsFromTruffle(ParametersNodeHelper nodeHelper) {
        String partialSignature = "";
        if (oclScope != null && oclScope.length > 0) {

            if (scopeTruffleList == null) {
                scopeTruffleList = new ArrayList<>();
            }

            // we have parameters from the scope not registered as external params.
            for (int i = 0; i < oclScope.length; i++) {
                AcceleratorType acceleratorType = oclScope[i];
                if (acceleratorType.getArrayDataType() == AcceleratorType.DataType.DOUBLE) {
                    int count = nodeHelper.getParamCount();
                    partialSignature += "__global double *p" + count + ",__constant int *p" + count + "_index_data,";
                    scopeTruffleList.add(new ScopeTruffle("__global double *", "p" + count, "double"));
                    scopeTruffleList.add(new ScopeTruffle("__constant int *", "p" + count + "_index_data", "int"));
                    nodeHelper.incParamCount();
                } else if (acceleratorType.getArrayDataType() == AcceleratorType.DataType.INTEGER) {
                    int count = nodeHelper.getParamCount();
                    partialSignature += "__global int *p" + count + ",__constant int *p" + count + "_index_data,";
                    scopeTruffleList.add(new ScopeTruffle("__global int *", "p" + count, "int"));
                    scopeTruffleList.add(new ScopeTruffle("__constant int *", "p" + count + "_index_data", "int"));
                    nodeHelper.incParamCount();
                } else if (acceleratorType.getArrayDataType() == AcceleratorType.DataType.BOOLEAN) {
                    int count = nodeHelper.getParamCount();
                    partialSignature += "__global bool *p" + count + ",__constant int *p" + count + "_index_data,";
                    scopeTruffleList.add(new ScopeTruffle("__global bool *", "p" + count, "double"));
                    scopeTruffleList.add(new ScopeTruffle("__constant int *", "p" + count + "_index_data", "int"));
                    nodeHelper.incParamCount();
                } else {
                    throw new RuntimeException("Data type not supported yet: " + acceleratorType.getArrayDataType());
                }
            }
        }
        return partialSignature;
    }

    @Override
    public String generateParametersSignature(StructuredGraph graph, Class<?> klass, ArrayList<ParameterNode> ioParams) throws KernelOffloadException {
        StringBuffer kernelSignature = new StringBuffer();

        ParametersNodeHelper nodeHelper = new ParametersNodeHelper();

        int index = -1;

        // iterate through local nodes to generate parameter list.
        for (ParameterNode paramNode : graph.getNodes(ParameterNode.TYPE)) {

            index++;

            if (index == 0) {
                continue;
            }

            String paramString = paramNode.stamp().toString();
            boolean isTuple = paramString.endsWith("a [Ljava/lang/Object;");

            // Assuming tuples objects - if more generic -> inspect constant pool here
            nodeHelper.setTuple(isTuple);
            int paramCount = graph.method().getSignature().getParameterCount(false);

            int numLambdaArgs = metadata.getType().getNumParams();
            boolean isLamdaArgument = paramNode.index() >= (paramCount - numLambdaArgs);
            nodeHelper.setLambdaArgument(isLamdaArgument);

            String partialSignature = generateParameterForSignature(paramNode, klass, ioParams, nodeHelper);

            if (!isGeneratingFunction && paramNode.index() == 1) {
                // Add any additional parameters required by the lambda to the signature
                for (int i = 0; i < numExtraArgs; i++) {
                    ParameterNode extraParam = graphLambda.getParameter(i);
                    isTuple = extraParam.stamp().toString().endsWith("a [Ljava/lang/Object;");
                    nodeHelper.setTuple(isTuple);
                    partialSignature += generateParameterForSignature(extraParam, klass, ioParams, nodeHelper);
                }
            }

            // Mechanism to generate the scope variable without the corresponding ParamNode (if it
            // comes from Truffle)
            if (index == 1 && isTruffleFrontEnd()) {
                partialSignature += generateMetadataForScopeVarsFromTruffle(nodeHelper);
            }

            if (!partialSignature.equals("")) {
                kernelSignature.append(partialSignature);
            }
        }
        if (nodeHelper.getParamCount() > 0) {
            kernelSignature = new StringBuffer(removeLastCharacter(kernelSignature.toString()));
        }
        this.lastIndexParam = nodeHelper.getParamCount();
        return kernelSignature.toString();
    }

    protected String generateParametersSignature(StructuredGraph graph) {
        String kernelSignature = "";

        ParametersNodeHelper nodeHelper = new ParametersNodeHelper();

        // First, inspect Truffle scope variables (they normally come from ConstantNode[])
        if (isGeneratingFunction && (isTruffleFrontEnd) && (scopeTruffleList != null) && !scopeTruffleList.isEmpty()) {

            int j = 0;
            for (int i = 0; i < scopeTruffleList.size(); i += 2) {
                int indexVar = nodeHelper.getParamCount();
                ScopeTruffle scope = scopeTruffleList.get(i);
                // name and index_aux
                String type = scope.getGlobalType();
                String nameVar = "p" + indexVar;
                scope.updateName(nameVar);

                if (arrayConstantNodes != null && !arrayConstantNodes.isEmpty()) {
                    this.arrayConstantIndex.put(arrayConstantNodes.get(j), i);
                    j++;
                }

                scope = scopeTruffleList.get(i + 1);
                String type2 = scope.getGlobalType();
                String nameVar2 = "p" + indexVar + "_index_data";
                scope.updateName(nameVar);

                kernelSignature += type + nameVar + "," + type2 + nameVar2 + ",";
                nodeHelper.incParamCount();
            }
        }

        // iterate through local nodes to generate parameter list.
        for (ParameterNode paramNode : graph.getNodes(ParameterNode.TYPE)) {

            String paramString = paramNode.stamp().toString();
            boolean isTuple = paramString.endsWith("a [Ljava/lang/Object;");
            nodeHelper.setTuple(isTuple);
            int paramCount = graph.method().getSignature().getParameterCount(false);

            // Bi-Function takes last 2 arguments from the lambda expression
            int numLambdaArgs = metadata.getType() == TypeOfFunction.BIFUNCTION ? 2 : 1;
            boolean isLamdaArgument = paramNode.index() >= (paramCount - numLambdaArgs);
            nodeHelper.setLambdaArgument(isLamdaArgument);

            String partialSignature = generateParameterForSignature(paramNode, null, null, nodeHelper);

            if (!isGeneratingFunction && paramNode.index() == 1) {
                // Add any additional parameters required by the lambda to the signature
                for (int i = 0; i < numExtraArgs; i++) {
                    ParameterNode extraParam = graphLambda.getParameter(i);
                    isTuple = extraParam.stamp().toString().endsWith("a [Ljava/lang/Object;");
                    nodeHelper.setTuple(isTuple);
                    partialSignature += generateParameterForSignature(extraParam, null, null, nodeHelper);
                }
            }

            if (!partialSignature.equals("")) {
                kernelSignature += partialSignature;
            }
        }
        if (nodeHelper.getParamCount() > 0) {
            kernelSignature = removeLastCharacter(kernelSignature);
        }
        this.lastIndexParam = nodeHelper.getParamCount();
        kernelSignature += ")";

        return kernelSignature;
    }

    @Override
    public void generateBodyKernel(StructuredGraph graph) {
        buffer.emitCode("{ ");
        buffer.beginBlock();
        addInitCode(graph);
        addLocalVariables(graph);

        if (this.metadata.getType() != LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION) {
            buffer.emitCode("int gs = get_global_size(0);");
        }

        if (GraalAcceleratorOptions.deoptGuardsEnabled) {
            buffer.emitCode(DEOPT_BUFFER_INITIALIZE);
        }

        this.dispatch(graph.start());
        buffer.endBlock();
        buffer.emitCode("}");
    }

    @Override
    public void updateSymbolTable(StructuredGraph graph) {

        for (Node node : graph.getNodes()) {
            if (node instanceof ConstantNode) {
                ConstantNode constant = (ConstantNode) node;
                if (constant.getStackKind() == JavaKind.Int) {
                    symbolTable.add("" + constant.asJavaConstant().asInt(), constant, JavaKind.Int);
                } else if (constant.getStackKind() == JavaKind.Boolean) {
                    symbolTable.add("" + constant.asJavaConstant().asBoolean(), constant, JavaKind.Boolean);
                } else if (constant.getStackKind() == JavaKind.Long) {
                    symbolTable.add("" + constant.asJavaConstant().asLong(), constant, JavaKind.Long);
                } else if (constant.getStackKind() == JavaKind.Float) {
                    symbolTable.add("" + constant.asJavaConstant().asFloat(), constant, JavaKind.Float);
                } else if (constant.getStackKind() == JavaKind.Double) {
                    if (constant.getValue().toValueString().equals("NaN")) {
                        ConstantNode c = ConstantNode.forDouble(0x7ff00000000007a2L);
                        symbolTable.add("" + c.asJavaConstant().asDouble(), constant, JavaKind.Double);
                        symbolTable.add("" + c.asJavaConstant().asDouble(), c, JavaKind.Double);
                    } else {
                        symbolTable.add("" + constant.asJavaConstant().asDouble(), constant, JavaKind.Double);
                    }
                } else if (constant.getStackKind() == JavaKind.Object) {
                    /*
                     * When an Object comes and it is double, we save it, otherwise, we store it as
                     * an Object (it could be a NULL). Normally it is not used in the code
                     * generation (only VirtualNodes are responsible for managing NULL constants. /
                     */
                    try {
                        if (Double.isFinite(Double.parseDouble(constant.asJavaConstant().toValueString()))) {
                            symbolTable.add("" + constant.asJavaConstant().toValueString(), constant, JavaKind.Double);
                        }
                    } catch (NumberFormatException e) {
                        symbolTable.add("" + constant.asJavaConstant().toValueString(), constant, JavaKind.Object);
                    }
                } else {
                    throw new RuntimeException("Type not supported yet as a constant in the code generator");
                }
            }
        }
    }

    private void addLocalDimsFunction() {
        if (this.generateExtraArrayInFunctionSignature) {
            buffer.emitCode("int p" + this.lastIndexParam + "_dim_1 = 0;");
        }
    }

    protected void generateBodyFunction(StructuredGraph graph) {
        Node start = graph.start();
        buffer.emitCode("{ ");
        buffer.beginBlock();
        addInitCode(graph);
        addLocalVariables(graph);
        addLocalDimsFunction();
        this.dispatch(start);
        buffer.endBlock();
        buffer.emitCode("}");
    }

    private String getExtraParametersForLambdaCall() {
        String functionArguments = "";
        for (int i = 0; i < numExtraArgs; i++) {
            ParameterNode argument = graphLambda.getParameter(i);
            String variableName = symbolTable.lookupName(argument);
            functionArguments += variableName + ",";
            if (!argument.getStackKind().isPrimitive() && ((ObjectStamp) argument.stamp()).type().isArray()) {
                // Also need to pass the index data for an array
                functionArguments += variableName + "_index_data,";
            }
        }
        return functionArguments;
    }

    private String generateStructForTuples(AcceleratorType[] typesObjects) {
        if ((typesObjects.length == 1) && typesObjects[0].getType().getFather() == null) {
            // We do not need structures
            isTupleEnabledSignature = false;
            return "";
        }

        isTupleEnabledSignature = true;

        // int numberOfElements = getNumberOfTupleComponents(typesObjects);
        int numberOfElements = typesObjects.length;

        if (numberOfElements == 0) {
            // The elements in the tuple are arrays, so we serialize work with
            // global memory
            return "";
        }

        String tupleName = getTupleName(typesObjects);

        String ifndef = "#ifndef " + tupleName + "_DEFINED\n#define " + tupleName + "_DEFINED\n";

        StringBuffer structCodeOpenCL = new StringBuffer();
        structCodeOpenCL.append(ifndef + "typedef struct { ");

        String dataType = "";

        List<String> dtList = new ArrayList<>();

        HashMap<String, String> internalObjectTable = new HashMap<>();

        // For vector type specialisation
        OCLDeviceInfo dev = (OCLDeviceInfo) GraalAcceleratorSystem.getInstance().getPlatform().getDevice().getDeviceInfo();
        // HashMap<Integer, Integer> deviceVectorTypes = dev.getDeviceVectorTypes();

        for (int i = 0; i < numberOfElements; i++) {
            // dataType = getTupleComponentDataType(tupleComponents, i);
            dataType = typesObjects[i].getType().getOCLName();

// // process dataType variable for OpenCL vector types
// if (GraalAcceleratorOptions.useVectorTypes) {
// if () {
//
// }
// }

            dtList.add(dataType);
            int field = i + 1;
            String fieldName = "_" + field;
            structCodeOpenCL.append("\n\t" + dataType + " " + fieldName + "; ");
            internalObjectTable.put(fieldName, dataType);
        }

        structCodeOpenCL.append("\n} " + tupleName + ";\n");

        if (globalObjectTable == null) {
            globalObjectTable = new HashMap<>();
        }

        globalObjectTable.put(tupleName, internalObjectTable);

        for (int i = 0; i < numberOfElements; i++) {
            // dataType = getTupleComponentDataType(tupleComponents, i);
            dataType = dtList.get(i);
            structCodeOpenCL.append("inline " + dataType + " _" + tupleName + "" + (i + 1) + "(" + tupleName + " p) { return p._" + (i + 1) + "; } \n");
        }

        structCodeOpenCL.append("#endif\n");

        return structCodeOpenCL.toString();
    }

    private static String getTupleName(AcceleratorType[] tuple) {

        if (tuple.length == 1) {
            return tuple[0].getType().getOCLName();
        } else {
            int numberOfElements = tuple.length;
            String tupleName = "Tuple";

            for (int i = 0; i < numberOfElements; i++) {
                String dataType = tuple[i].getType().getOCLName();
                tupleName += "_" + dataType;
            }
            return tupleName;
        }
    }

    private void setupParametersForKernelGeneration(StructuredGraph graph, LambdaFunctionMetadata oclmetadata, AcceleratorOCLInfo typeInfoOCL) {
        this.metadata = oclmetadata;
        this.loopsData = new LoopsData(graph);
        this.tuplesInputDataTypes = typeInfoOCL.getOCLInput();
        this.outputDataType = typeInfoOCL.getClassOutput();
        this.tuplesOutputDataTypes = typeInfoOCL.getOCLOutput();
        this.typeInfoOCL = typeInfoOCL;
        isGeneratingFunction = true;
    }

    public class ScopeDetectionPhase extends Phase {

        private ArrayList<ConstantNode> arrayConstantNodes;

        @Override
        protected void run(StructuredGraph graph) {
            if (arrayConstantNodes == null) {
                arrayConstantNodes = new ArrayList<>();
            }
            checkLoadIndexedNodes(graph);
        }

        public ArrayList<ConstantNode> getConstantNodes() {
            return arrayConstantNodes;
        }

        private void analyseConstant(Node node) {
            Constant value = ((ConstantNode) node).getValue();
            if (value instanceof HotSpotObjectConstant) {
                arrayConstantNodes.add((ConstantNode) node);
            }
        }

        private void iterateLoadIndexInputs(NodePosIterator iterator) {
            while (iterator.hasNext()) {
                Node scopeNode = iterator.next();
                if (scopeNode instanceof ConstantNode) {
                    if (!arrayConstantNodes.contains(scopeNode)) {
                        analyseConstant(scopeNode);
                    }
                }
            }
        }

        private void checkLoadIndexedNodes(StructuredGraph graph) {
            for (Node node : graph.getNodes()) {
                if (node instanceof LoadIndexedNode) {
                    LoadIndexedNode loadIndexed = (LoadIndexedNode) node;
                    NodePosIterator iterator = loadIndexed.inputs().iterator();
                    iterateLoadIndexInputs(iterator);
                }
            }
        }
    }

    @Override
    public void generateOpenCLForLambdaFunction(StructuredGraph graph, LambdaFunctionMetadata oclmetadata, AcceleratorOCLInfo acceleratorOCLInfo) throws KernelOffloadException {

        if (isTruffleFrontEnd) {
            ScopeDetectionPhase scope = new ScopeDetectionPhase();
            scope.apply(graph);
            arrayConstantNodes = scope.getConstantNodes();
            // we order them by the ID
            Collections.sort(arrayConstantNodes);
        }

        generateLambdaKernel(graph, oclmetadata, acceleratorOCLInfo);

        // Add struct typedefs and nested functions to the front
        CodeBuffer finalBuffer = new CodeBuffer(true);
        for (String value : structTypedefs) {
            finalBuffer.emitCode(value);
        }

        for (CodeBuffer cgb : nestedFunctions) {
            finalBuffer.append(cgb);
        }

        finalBuffer.append(buffer);
        buffer = finalBuffer;
    }

    private String generateLambdaSignature(StructuredGraph graph) {
        String type = null;
        String returnType = null;
        boolean generateExtraArraysTupleOutput = false;
        if (parametersDirection != null) {
            if (outputDataType.getNumAttributes() != -1) {
                this.structTohide = getTupleName(tuplesOutputDataTypes);
                type = "void";
                generateExtraArraysTupleOutput = true;
            } else {
                // XXX: Improve the data type to multidimensional
                type = TypeUtil.getTypeFromArrayNonPrimitiveObject(parametersDirection.get(Direction.OUTPUT)[0].getClass());
                if (type == null) {
                    Object[] output = parametersDirection.get(Direction.OUTPUT);
                    String objectType = output[0].getClass().getComponentType().toString();
                    if (objectSymbolTable.containsKey(objectType)) {
                        type = getTupleName(tuplesOutputDataTypes);

                    }
                }
            }
        }

        // If type is still null, we can obtain the dataType from DataType Enum
        boolean generateExtraArray = false;
        if ((type == null) && (outputDataType != null)) {
            if (outputDataType.getArrayDim() != 0) {
                type = "void";
                returnType = outputDataType.getArrayNameType();
                generateExtraArray = true;
            } else {
                type = getTupleName(tuplesOutputDataTypes);
            }
        }

        String optimizationInline = "inline ";
        String kernelSignature = "";
        String functionName = " " + graph.method().getName();

        kernelSignature = optimizationInline + type + functionName;
        returnTypeFunction = type;

        kernelSignature += generateKernelSignature(graph);

        String globalAccess = "__global ";
        String localAccess = " ";
        if (generateExtraArray) {
            int paramCount = lastIndexParam;
            kernelSignature = removeLastCharacter(kernelSignature);
            kernelSignature += ", ";
            kernelSignature += globalAccess + returnType + "  *p" + paramCount;
            kernelSignature += ",";
            kernelSignature += localAccess + " int *" + "p" + paramCount + "_index_data";
            kernelSignature += ")";
            symbolTable.add("p" + paramCount, null, JavaKind.Short);
            lastIndexParam = paramCount;
            generateExtraArrayInFunctionSignature = generateExtraArray;
        }

        ArrayList<ArrayList<String>> extraParameters = ExtraArrayNamesManager.getInstance().getArrayInputList();
        if (!extraParameters.isEmpty()) {
            kernelSignature = removeLastCharacter(kernelSignature);
            for (ArrayList<String> param : extraParameters) {
                returnType = param.get(ArrayInfo.TYPE.getIdx());
                String parameter = param.get(ArrayInfo.NAME.getIdx());
                kernelSignature += ", ";
                kernelSignature += globalAccess + returnType + " * " + parameter;
            }
            kernelSignature += ")";
            generateExtraArrayInFunctionSignature = generateExtraArray;
        }

        if (generateExtraArraysTupleOutput) {
            int paramCount = lastIndexParam;
            kernelSignature = removeLastCharacter(kernelSignature);

            ArrayList<ArrayList<String>> outputExtraParams = ExtraArrayNamesManager.getInstance().getArrayOutputList();

            for (ArrayList<String> param : outputExtraParams) {
                returnType = param.get(ArrayInfo.TYPE.getIdx());
                String parameter = param.get(ArrayInfo.NAME.getIdx());
                kernelSignature += ", ";
                kernelSignature += globalAccess + returnType + " * " + parameter + ",";
                kernelSignature += globalAccess + " int *" + parameter + "_index_data";
                paramCount++;
            }
            kernelSignature += ")";
            generateExtraArrayInFunctionSignature = generateExtraArray;
            lastIndexParam = paramCount;
        }

        if (GraalAcceleratorOptions.deoptGuardsEnabled) {
            kernelSignature = kernelSignature.substring(0, kernelSignature.length() - 1);
            kernelSignature += "," + DEOPT_BUFFER_SIGNATURE_TEMPLATE + ")";
        }
        return kernelSignature;
    }

    private void generateTupleStructTypedefs() {
        String structTypedef;
        structTypedef = generateStructForTuples(tuplesInputDataTypes);
        structTypedefs.add(structTypedef);

        structTypedef = generateStructForTuples(tuplesOutputDataTypes);
        structTypedefs.add(structTypedef);
    }

    private void generateLambdaKernel(StructuredGraph graph, LambdaFunctionMetadata oclmetadata, AcceleratorOCLInfo acceleratorOCLInfo) throws RuntimeException {
        // Debug.dump(graph, "REAL GRAPH INTO THE GPU CODE GEN");
        setupParametersForKernelGeneration(graph, oclmetadata, acceleratorOCLInfo);
        generateTupleStructTypedefs();

        String kernelSignature = generateLambdaSignature(graph);
        buffer.emitCode(kernelSignature);

        updateSymbolTable(graph);
        generateBodyFunction(graph);
    }

    @Override
    public void generateMainSkeletonKernel(StructuredGraph graph, Class<?> klass, LambdaFunctionMetadata oclmetadata, AcceleratorOCLInfo typeOCL, ArrayList<ParameterNode> ioParams,
                    UUID uuidKernel) throws KernelOffloadException {

        this.metadata = oclmetadata;
        typeOCL.getClassInput();
        this.tuplesInputDataTypes = typeOCL.getOCLInput();
        this.outputDataType = typeOCL.getClassOutput();
        this.tuplesOutputDataTypes = typeOCL.getOCLOutput();
        this.oclScope = typeOCL.getOCLScope();
        this.loopsData = new LoopsData(graph);
        this.isGeneratingFunction = false;
        int numLambdaArgs = (metadata.getType() == TypeOfFunction.BIFUNCTION) ? 2 : 1;
        this.numExtraArgs = graphLambda.method().getSignature().getParameterCount(false) - numLambdaArgs;
        this.uuid = uuidKernel;

        this.kernelName = graph.method().getName() + "Kernel";
        String kernelSignature = "__kernel void " + kernelName + " ";
        kernelSignature += generateKernelSignature(graph, klass, ioParams);

        if (GraalAcceleratorOptions.deoptGuardsEnabled) {
            kernelSignature = kernelSignature.substring(0, kernelSignature.length() - 1);
            kernelSignature += "," + DEOPT_BUFFER_SIGNATURE_TEMPLATE + ")";
        }

        buffer.emitCode(kernelSignature);
        updateSymbolTable(graph);

        generateBodyKernel(graph);
    }

    public ArrayList<ScopeTruffle> getScopeTruffleList() {
        return scopeTruffleList;
    }

    @SuppressWarnings("unchecked")
    public void setScopeTruffleList(ArrayList<ScopeTruffle> scopeList) {
        if (scopeList != null) {
            this.scopeTruffleList = (ArrayList<ScopeTruffle>) scopeList.clone();
        } else {
            scopeTruffleList = null;
        }
    }

    @Override
    public String getCode() {
        return buffer.getCode();
    }

    @Override
    public String getKernelName() {
        return kernelName;
    }

    @Override
    public void dispatch(Node node) {
        Method visitorMethod;
        try {
            visitorMethod = this.getClass().getMethod("visit", new Class[]{node.getClass()});
            visitorMethod.invoke(this, node);
        } catch (NoSuchMethodException e) {
            // Call to generic visitor to handle it (print the error)
            this.visit(node);
        } catch (StackOverflowError e) {
            e.printStackTrace();
            throw new RuntimeException("StackOverFlow " + e);
        } catch (Exception e) {
            // Any other exception -> launch a RuntimeException (deoptimize in higher layers)
            throw new RuntimeException(">>> EXCEPTION " + e);
        }
    }

    /**
     * What follows is the visit method implementations for all the Graal StructuredGraph IR Nodes.
     */
    @Override
    public void visit(ArrayLengthNode arrayLengthNode) {
        buffer.emitComment("visited ArrayLengthNode");
        buffer.emitComment(arrayLengthNode.toString(Verbosity.All));

        // is the array coming in from the parameter list?
        if (symbolTable.lookupArrayAccessInfo(arrayLengthNode) != null) {
            ArrayDepth arrayDepth = symbolTable.lookupArrayAccessInfo(arrayLengthNode);
            Node arrayNode = arrayDepth.getNode();
            int dim = arrayDepth.getDimensionAccessedAt();
            String srcArrayName = symbolTable.lookupName(arrayNode);
            String newVar = srcArrayName + "_len_dim_" + dim;

            JavaKind kind = JavaKind.Int;
            if (!srcArrayName.startsWith(JavaKind.Illegal.getJavaName())) {
                buffer.emitCode("int " + newVar + " = " + srcArrayName + "_index_data[" + srcArrayName + "_dim_" + dim + "]; ");
            } else {
                buffer.emitCode("// This is an illegal type for: " + newVar);
                kind = JavaKind.Illegal;
            }
            symbolTable.add(newVar, arrayLengthNode, kind);
        } else {
            buffer.emitComment("ArrayLengthNode() - not of type LocalNode or LoadFieldNode");
        }

        for (Node node : arrayLengthNode.successors()) {
            this.dispatch(node);
        }
    }

    @Override
    public void visit(BeginNode beginNode) {
        buffer.emitComment("visited BeginNode");
        buffer.emitComment(beginNode.toString(Verbosity.All));
        symbolTable.enterScope();

        for (Node successor : beginNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
        symbolTable.exitScope();
    }

    @Override
    public void visit(ConstantNode constantNode) {
        // ConstantNodes get added to the symbol symbolTable earlier
        buffer.emitComment("visited ContantNode");
        buffer.emitComment(constantNode.toString(Verbosity.All));
    }

    @Override
    public void visit(ConditionalNode conditionalNode) {
        buffer.emitComment("visited ConditionalNode");
        buffer.emitComment(conditionalNode.toString(Verbosity.All));

        if (!symbolTable.exists(conditionalNode.trueValue())) {
            this.dispatch(conditionalNode.trueValue());
        }

        if (!symbolTable.exists(conditionalNode.falseValue())) {
            this.dispatch(conditionalNode.falseValue());
        }

        LogicNode condition = conditionalNode.condition();
        if (!symbolTable.exists(condition)) {
            this.dispatch(condition);
        }

        // String xVar = symbolTable.lookupName(conditionalNode.trueValue());
        // String yVar = symbolTable.lookupName(conditionalNode.falseValue());
        String conditionName = symbolTable.lookupName(condition);

        // String conditionVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);

        // symbolTable.add(conditionVar, conditionalNode, JavaKind.Boolean);
        // buffer.emitCode("bool " + conditionVar + " = " + xVar + " " + conditionName + " " + yVar
        // + ";");
        // symbolTable.add(conditionVar, conditionalNode, JavaKind.Boolean);

        symbolTable.add(conditionName, conditionalNode, JavaKind.Boolean);

        for (Node successor : conditionalNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(FloatConvertNode convertNode) {
        buffer.emitComment("visited FloatConvertNode");
        buffer.emitComment(convertNode.toString(Verbosity.All));

        buffer.emitComment(convertNode.getValue().toString());
        ValueNode valueNode = convertNode.getValue();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String varToCast = symbolTable.lookupName(valueNode);
        String varName = symbolTable.newVariable(SymbolTable.CAST_VAR);
        String toType = convertNode.getFloatConvert().toString();
        JavaKind kind = null;

        // Conversions
        if (toType.equals("I2F")) {
            toType = "float";
            kind = JavaKind.Float;
        } else if (toType.equals("F2I")) {
            toType = "int";
            kind = JavaKind.Int;
        } else if (toType.equals("L2F")) {
            toType = "float";
            kind = JavaKind.Float;
        } else if (toType.equals("I2D")) {
            toType = "double";
            kind = JavaKind.Double;
        } else if (toType.equals("D2I")) {
            toType = "int";
            kind = JavaKind.Int;
        } else if (toType.equals("D2F")) {
            toType = "float";
            kind = JavaKind.Float;
        } else if (toType.equals("F2D")) {
            toType = "double";
            kind = JavaKind.Double;
        } else if (toType.equals("L2D")) {
            toType = "double";
            kind = JavaKind.Double;
        }

        JavaKind kindCast = symbolTable.lookupType(varToCast);
        if (kindCast == JavaKind.Illegal) {
            kind = JavaKind.Illegal;
        } else {
            buffer.emitCode(toType + " " + varName + " = (" + toType + ") " + varToCast + ";");
        }

        symbolTable.add(varName, convertNode, kind);

        for (Node successor : convertNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(EndNode endNode) {
        buffer.emitComment("visited EndNode");
        buffer.emitComment(endNode.toString(Verbosity.All));

        // Inserted into the Symbol Table before starting the visitor
        for (PhiNode phiNode : endNode.merge().phis()) {

            // if (!symbolTable.exists(phiNode)) {
            // this.dispatch(phiNode);
            // }

            String phiVariableName = symbolTable.lookupName(phiNode);

            ValueNode valueNode = phiNode.valueAt(endNode);
            if (!symbolTable.exists(valueNode)) {
                this.dispatch(valueNode);
            }

            String value = symbolTable.lookupName(valueNode);

            if (!value.equals("null")) {
                buffer.emitCode(phiVariableName + " = " + value + ";");
            }
        }

        for (Node successor : endNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(AddNode addNode) {
        buffer.emitComment("visited AddNode");
        buffer.emitComment(addNode.toString(Verbosity.All));

        if (!symbolTable.exists(addNode.getX())) {
            this.dispatch(addNode.getX());
        }

        if (!symbolTable.exists(addNode.getY())) {
            this.dispatch(addNode.getY());
        }

        String xVar = symbolTable.lookupName(addNode.getX());
        String yVar = symbolTable.lookupName(addNode.getY());

        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

        JavaKind kind = addNode.getStackKind();

        symbolTable.add(resultVar, addNode, kind);
        String type = addNode.getStackKind().getJavaName();
        buffer.emitCode(type + " " + resultVar + " = " + xVar + " + " + yVar + ";");

        for (Node node : addNode.cfgSuccessors()) {
            this.dispatch(node);
        }
    }

    @Override
    public void visit(DivNode divNode) {
        buffer.emitComment("visited DivNode");
        buffer.emitComment(divNode.toString(Verbosity.All));

        if (!symbolTable.exists(divNode.getX())) {
            this.dispatch(divNode.getX());
        }

        if (!symbolTable.exists(divNode.getY())) {
            this.dispatch(divNode.getY());
        }

        String xVar = symbolTable.lookupName(divNode.getX());
        String yVar = symbolTable.lookupName(divNode.getY());

        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

        JavaKind kind = divNode.getStackKind();

        symbolTable.add(resultVar, divNode, kind);

        String type = divNode.getStackKind().getJavaName();

        if (type.equals("int") && yVar.equals("2")) {
            buffer.emitCode(type + " " + resultVar + " = " + xVar + " >>  1;");
        } else {
            buffer.emitCode(type + " " + resultVar + " = " + xVar + " / " + yVar + ";");
        }

        for (Node succ : divNode.cfgSuccessors()) {
            this.dispatch(succ);
        }

    }

    public void visit(IntegerMulExactNode node) {
        buffer.emitComment("visited IntegerMulExactNode");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getX())) {
            this.dispatch(node.getX());
        }

        if (!symbolTable.exists(node.getY())) {
            this.dispatch(node.getY());
        }

        String xVar = symbolTable.lookupName(node.getX());
        String yVar = symbolTable.lookupName(node.getY());
        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

        symbolTable.add(resultVar, node, JavaKind.Int);
        String type = JavaKind.Int.getJavaName();       // It should be "int"

        String code = type + " " + resultVar + " = " + xVar + " * " + yVar + "; ";
        buffer.emitCode(code);

        for (Node succ : node.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    public void visit(IntegerSubExactNode node) {
        buffer.emitComment("visited IntegerSubExactNode");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getX())) {
            this.dispatch(node.getX());
        }

        if (!symbolTable.exists(node.getY())) {
            this.dispatch(node.getY());
        }

        String xVar = symbolTable.lookupName(node.getX());
        String yVar = symbolTable.lookupName(node.getY());
        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

        symbolTable.add(resultVar, node, JavaKind.Int);
        String type = JavaKind.Int.getJavaName();

        String code = type + " " + resultVar + " = " + xVar + " - " + yVar + "; ";
        buffer.emitCode(code);

        for (Node succ : node.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    public void visit(IntegerAddExactNode node) {
        buffer.emitComment("visited IntegerAddExactNode");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getX())) {
            this.dispatch(node.getX());
        }

        if (!symbolTable.exists(node.getY())) {
            this.dispatch(node.getY());
        }

        String xVar = symbolTable.lookupName(node.getX());
        String yVar = symbolTable.lookupName(node.getY());
        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

        symbolTable.add(resultVar, node, JavaKind.Int);
        String type = JavaKind.Int.getJavaName();

        String code = type + " " + resultVar + " = " + xVar + " + " + yVar + "; ";
        buffer.emitCode(code);

        for (Node succ : node.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    @Override
    public void visit(MulNode mulNode) {
        buffer.emitComment("visited MulNode");
        buffer.emitComment(mulNode.toString(Verbosity.All));

        if (!symbolTable.exists(mulNode.getX())) {
            this.dispatch(mulNode.getX());
        }

        if (!symbolTable.exists(mulNode.getY())) {
            this.dispatch(mulNode.getY());
        }

        String xVar = symbolTable.lookupName(mulNode.getX());
        String yVar = symbolTable.lookupName(mulNode.getY());
        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

        JavaKind kind = mulNode.getStackKind();
        symbolTable.add(resultVar, mulNode, kind);
        String type = mulNode.getStackKind().getJavaName();

        String code = type + " " + resultVar + " = " + xVar + " * " + yVar + "; ";
        buffer.emitCode(code);

        for (Node succ : mulNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    @Override
    public void visit(SubNode subNode) {
        buffer.emitComment("visited SubNode");
        buffer.emitComment(subNode.toString(Verbosity.All));

        if (!symbolTable.exists(subNode.getX())) {
            this.dispatch(subNode.getX());
        }

        if (!symbolTable.exists(subNode.getY())) {
            this.dispatch(subNode.getY());
        }

        String xVar = symbolTable.lookupName(subNode.getX());
        String yVar = symbolTable.lookupName(subNode.getY());

        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

        JavaKind kind = subNode.getStackKind();

        symbolTable.add(resultVar, subNode, kind);

        String type = subNode.getStackKind().getJavaName();
        buffer.emitCode(type + " " + resultVar + " = " + xVar + " - " + yVar + ";");

        for (Node succ : subNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    public void visit(FloatLessThanNode floatLessThanNode) {
        buffer.emitComment("visited FloatLessThanNode");
        buffer.emitComment(floatLessThanNode.toString(Verbosity.All));

        if (!symbolTable.exists(floatLessThanNode.getX())) {
            this.dispatch(floatLessThanNode.getX());
        }

        if (!symbolTable.exists(floatLessThanNode.getY())) {
            this.dispatch(floatLessThanNode.getY());
        }

        String xVar = symbolTable.lookupName(floatLessThanNode.getX());
        String yVar = symbolTable.lookupName(floatLessThanNode.getY());

        String conditionVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);
        symbolTable.add(conditionVar, floatLessThanNode, JavaKind.Boolean);

        JavaKind kindX = symbolTable.lookupType(xVar);
        JavaKind kindY = symbolTable.lookupType(yVar);
        JavaKind kind = JavaKind.Boolean;

        if (kindX != JavaKind.Illegal && kindY != JavaKind.Illegal) {
            buffer.emitCode("bool " + conditionVar + " = " + xVar + " < " + yVar + ";");
        } else {
            kind = JavaKind.Illegal;
        }

        symbolTable.add(conditionVar, floatLessThanNode, kind);

        for (Node succ : floatLessThanNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    public void visit(FloatEqualsNode floatEqualsNode) {
        buffer.emitComment("visited FloatEqualsNode");
        buffer.emitComment(floatEqualsNode.toString(Verbosity.All));

        if (!symbolTable.exists(floatEqualsNode.getX())) {
            this.dispatch(floatEqualsNode.getX());
        }

        if (!symbolTable.exists(floatEqualsNode.getY())) {
            this.dispatch(floatEqualsNode.getY());
        }

        String xVar = symbolTable.lookupName(floatEqualsNode.getX());
        String yVar = symbolTable.lookupName(floatEqualsNode.getY());

        String varName = symbolTable.newVariable(SymbolTable.CONDITION_VAR);
        symbolTable.add(varName, floatEqualsNode, JavaKind.Boolean);
        buffer.emitCode("bool " + varName + " = " + xVar + " == " + yVar + ";  ");

        for (Node succ : floatEqualsNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    @Override
    public void visit(IfNode ifNode) {
        buffer.emitComment("visited IfNode");
        buffer.emitComment(ifNode.toString(Verbosity.All));

        LogicNode condition = ifNode.condition();

        if (!symbolTable.exists(condition)) {
            this.dispatch(condition);
        }

        String conditionVar = symbolTable.lookupName(condition);

        // condition
        buffer.emitCode("if (" + conditionVar + ")");

        // true block
        buffer.emitCode("{");
        buffer.beginBlock();

        AbstractBeginNode trueSuccessor = ifNode.trueSuccessor();

        if (!symbolTable.exists(trueSuccessor)) {
            this.dispatch(trueSuccessor);
        }

        buffer.endBlock();
        buffer.emitCode("}");

        // else block
        buffer.emitCode("else");
        buffer.emitCode("{");
        int sizeCurrent = buffer.size();

        buffer.beginBlock();

        AbstractBeginNode falseSuccessor = ifNode.falseSuccessor();
        if (!symbolTable.exists(falseSuccessor)) {
            this.dispatch(falseSuccessor);
        }

        buffer.endBlock();
        int sizeWithElse = buffer.size();
        buffer.emitCode("}");

        if ((sizeWithElse - sizeCurrent) == 0) {
            // Remove ElsePart because the block is empty
            buffer.removeLastNLines(3);
        }
    }

    @Override
    public void visit(IntegerBelowNode integerBelowThanNode) {
        buffer.emitComment("visited IntegerBelowThanNode");
        buffer.emitComment(integerBelowThanNode.toString(Verbosity.All));

        if (!symbolTable.exists(integerBelowThanNode.getX())) {
            this.dispatch(integerBelowThanNode.getX());
        }

        if (!symbolTable.exists(integerBelowThanNode.getY())) {
            this.dispatch(integerBelowThanNode.getY());
        }

        String xVar = symbolTable.lookupName(integerBelowThanNode.getX());
        String yVar = symbolTable.lookupName(integerBelowThanNode.getY());

        String conditionVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);
        JavaKind kind = JavaKind.Boolean;

        JavaKind kindX = symbolTable.lookupType(xVar);
        JavaKind kindY = symbolTable.lookupType(yVar);
        if (kindX != JavaKind.Illegal && kindY != JavaKind.Illegal) {
            buffer.emitCode("bool " + conditionVar + " = " + xVar + " < " + yVar + ";");
        } else {
            kind = JavaKind.Illegal;
        }
        symbolTable.add(conditionVar, integerBelowThanNode, kind);

        for (Node succ : integerBelowThanNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    @Override
    public void visit(IntegerDivNode integerDivNode) {
        buffer.emitComment("visited IntegerDivNode");
        buffer.emitComment(integerDivNode.toString(Verbosity.All));

        if (!symbolTable.exists(integerDivNode.getX())) {
            this.dispatch(integerDivNode.getX());
        }

        if (!symbolTable.exists(integerDivNode.getY())) {
            this.dispatch(integerDivNode.getY());
        }

        String xVar = symbolTable.lookupName(integerDivNode.getX());
        String yVar = symbolTable.lookupName(integerDivNode.getY());

        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        symbolTable.add(resultVar, integerDivNode, JavaKind.Int);
        buffer.emitCode("int " + resultVar + " = " + xVar + " / " + yVar + ";");

        for (Node succ : integerDivNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    @Override
    public void visit(IntegerEqualsNode integerEqualsNode) {
        buffer.emitComment("visited IntegerEqualsNode");
        buffer.emitComment(integerEqualsNode.toString(Verbosity.All));

        if (!symbolTable.exists(integerEqualsNode.getX())) {
            this.dispatch(integerEqualsNode.getX());
        }

        if (!symbolTable.exists(integerEqualsNode.getY())) {
            this.dispatch(integerEqualsNode.getY());
        }

        String xVar = symbolTable.lookupName(integerEqualsNode.getX());
        String yVar = symbolTable.lookupName(integerEqualsNode.getY());
        String varName = symbolTable.newVariable(SymbolTable.CONDITION_VAR);

        if (integerEqualsNode.getX().getClass() != ReinterpretNode.class) {
            JavaKind kind = symbolTable.lookupType(xVar);
            if (kind != JavaKind.Illegal) {
                if (isTruffleFrontEnd() && integerEqualsNode.getY().getClass() == ReinterpretNode.class) {
                    /*
                     * If coming from Ruby with this constant => is a comparison with -1 but from
                     * binary representation.
                     *
                     * In Ruby: [4607182418800017408].pack('Q').unpack('D') => [1.0]
                     */
                    double s = Double.longBitsToDouble(Long.parseLong(xVar));
                    xVar = Double.toString(s);
                }
                buffer.emitCode("bool " + varName + " = " + xVar + " == " + yVar + "; // Integer EQUALS NODE: " + integerEqualsNode.getX().getClass());
            }
            symbolTable.add(varName, integerEqualsNode, JavaKind.Boolean);
        } else {
            symbolTable.add(varName, integerEqualsNode, JavaKind.Illegal);
        }

        for (Node node : integerEqualsNode.successors()) {
            this.dispatch(node);
        }
    }

    @Override
    public void visit(IntegerLessThanNode integerLessThanNode) {
        buffer.emitComment("visited IntegerLessThanNode");
        buffer.emitComment(integerLessThanNode.toString(Verbosity.All));

        if (!symbolTable.exists(integerLessThanNode.getX())) {
            this.dispatch(integerLessThanNode.getX());
        }

        if (!symbolTable.exists(integerLessThanNode.getY())) {
            this.dispatch(integerLessThanNode.getY());
        }

        String xVar = symbolTable.lookupName(integerLessThanNode.getX());
        String yVar = symbolTable.lookupName(integerLessThanNode.getY());
        String conditionVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);

        JavaKind kind = JavaKind.Boolean;
        JavaKind kindX = symbolTable.lookupType(xVar);
        JavaKind kindY = symbolTable.lookupType(yVar);
        if (kindX != JavaKind.Illegal && kindY != JavaKind.Illegal) {
            buffer.emitCode("bool " + conditionVar + " = " + xVar + " < " + yVar + ";");
        } else {
            kind = JavaKind.Illegal;
        }
        symbolTable.add(conditionVar, integerLessThanNode, kind);

        for (Node successor : integerLessThanNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(IntegerRemNode integerRemNode) {
        buffer.emitComment("visited IntegerRemNode");
        buffer.emitComment(integerRemNode.toString(Verbosity.All));

        if (!symbolTable.exists(integerRemNode.getX())) {
            this.dispatch(integerRemNode.getX());
        }

        if (!symbolTable.exists(integerRemNode.getY())) {
            this.dispatch(integerRemNode.getY());
        }

        String xVar = symbolTable.lookupName(integerRemNode.getX());
        String yVar = symbolTable.lookupName(integerRemNode.getY());

        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        symbolTable.add(resultVar, integerRemNode, JavaKind.Int);
        buffer.emitCode("int " + resultVar + " = " + xVar + " % " + yVar + ";");

        for (Node succ : integerRemNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    @Override
    public void visit(InvokeWithExceptionNode invokeWithExceptionNode) {
        buffer.emitComment("visited InvokeWithExceptionNode");
        buffer.emitComment(invokeWithExceptionNode.toString(Verbosity.All));
        buffer.emitComment(invokeWithExceptionNode.methodCallTarget().targetName());
        MethodCallTargetNode mct = invokeWithExceptionNode.methodCallTarget();

        String methodArgs = "";
        for (ValueNode arg : mct.arguments()) {
            if (!symbolTable.exists(arg)) {
                this.dispatch(arg);
            }
            String varName = symbolTable.lookupName(arg);

            // If not a primitive and not an array then it must be a structure passed by reference
            if (!arg.getStackKind().isPrimitive() && !((ObjectStamp) arg.stamp()).type().isArray()) {
                varName = "&" + varName;
            }

            methodArgs += (varName + ",");
        }

        // remove last comma
        methodArgs = removeLastCharacter(methodArgs);

        JavaKind returnType = mct.returnType().getJavaKind();

        if (!returnType.getJavaName().equals("void")) {
            String resultVar = symbolTable.newVariable(SymbolTable.FUNCTION_RESULT);
            symbolTable.add(resultVar, invokeWithExceptionNode, returnType);

            buffer.emitCode(returnType.getJavaName() + " " + resultVar + " = " + mct.targetName() + "(" + methodArgs + ");");
        } else {
            buffer.emitCode(mct.targetName() + "(" + methodArgs + ");");
        }

        for (Node succ : invokeWithExceptionNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    private static JavaKind getKindType(DataType arrayType) {
        switch (arrayType) {
            case FLOAT:
                return JavaKind.Float;
            case DOUBLE:
                return JavaKind.Double;
            case LONG:
                return JavaKind.Long;
            case INTEGER:
                return JavaKind.Int;
            case SHORT:
                return JavaKind.Short;
            case BOOLEAN:
                return JavaKind.Boolean;
            case CHAR:
                return JavaKind.Char;
            case BYTE:
                return JavaKind.Byte;
            default:
                break;
        }
        return JavaKind.Illegal;
    }

    private void generateArgumentsForFunction(String type, String generatedName, String resultVar, String functionName, String functionArguments, String originalName) {
        if (type.startsWith("null *")) {
            int fieldNum = Integer.parseInt(generatedName);
            HashMap<Integer, Integer> indexes = ExtraArrayNamesManager.getInstance().getInIndex();
            int access = indexes.get(fieldNum - 1);
            ArrayList<String> parameters = ExtraArrayNamesManager.getInstance().getArrayInputList().get(access);
            buffer.emitCode("__global " + parameters.get(ArrayInfo.TYPE.getIdx()) + " * " + resultVar + " = & " + parameters.get(ArrayInfo.NAME.getIdx()) + " [0]; ");
        } else if ((PArrayScopeManager.INSTANCE.hasScope(uuid)) && (originalName.equals("PArray.get"))) {
            // Get or Set into PArray. First argument is the array name, second is the index.
            String[] split = functionArguments.split(",");
            buffer.emitCode(type + " " + resultVar + " = " + split[0] + "[" + split[1] + "];  /** NEW INTERFACE */ ");
        } else if ((PArrayScopeManager.INSTANCE.hasScope(uuid)) && (originalName.equals("PArray.size"))) {
            String[] split = functionArguments.split(",");
            buffer.emitCode(type + " " + resultVar + " = " + split[0] + "_index_data[0];  /* Param SIZE */ ");
        } else {
            // Normal function generation
            buffer.emitCode(type + " " + resultVar + " = " + functionName + "(" + functionArguments + "); ");
        }
    }

    private void generateCallForLambdaExpression(String returnType, String resultVar, String functionName, String functionArgs) {
        String functionArguments = functionArgs;
        ArrayList<ArrayList<String>> arraysInTuplesArgument = ExtraArrayNamesManager.getInstance().getArrayInputList();

        if (PArrayScopeManager.INSTANCE.hasScope(uuid)) {
            functionArguments = functionArguments.replace("null,", "");
            ArrayList<String> inputVars = tuplesManagement.getInputVars();
            ArrayList<String> inputAux = tuplesManagement.getInputAuxVars();
            StringBuffer additional = new StringBuffer();
            for (int i = 0; i < inputVars.size(); i++) {
                additional.append(inputVars.get(i) + "," + inputAux.get(i) + ",");
            }
            functionArguments = additional.toString() + functionArguments;
        }

        StringBuffer scopeArguments = new StringBuffer();
        if ((isTruffleFrontEnd() && (scopeTruffleList != null) && !scopeTruffleList.isEmpty())) {
            for (ScopeTruffle scope : scopeTruffleList) {
                scopeArguments.append(scope.getName() + ",");
            }
            functionArguments = scopeArguments.toString() + functionArguments;
        }

        String code = null;
        if (!GraalAcceleratorOptions.deoptGuardsEnabled) {
            code = returnType + " " + resultVar + " = " + functionName + "(" + functionArguments + ");  ";
        } else {
            code = returnType + " " + resultVar + " = " + functionName + "(" + functionArguments + "," + DEOPT_BUFFER_PARAM + ");";
        }

        if (!arraysInTuplesArgument.isEmpty()) {
            // We need to generate extra arguments for the lambda expression because they
            // are arrays to global memory
            functionArguments += ",";
            for (ArrayList<String> parameter : arraysInTuplesArgument) {
                functionArguments += parameter.get(ArrayInfo.NAME.getIdx()) + ",";
            }
            functionArguments = removeLastCharacter(functionArguments);
            if (!GraalAcceleratorOptions.deoptGuardsEnabled) {
                code = returnType + " " + resultVar + " = " + functionName + "(" + functionArguments + "); ";
            } else {
                code = returnType + " " + resultVar + " = " + functionName + "(" + functionArguments + "," + DEOPT_BUFFER_PARAM + ");";
            }
        }

        ArrayList<ArrayList<String>> arraysOutput = ExtraArrayNamesManager.getInstance().getArrayOutputList();

        String postCall = "";

        StringBuffer spaces = buffer.getSpaces();
        if (!arraysOutput.isEmpty()) {
            byte tupleIdx = 1;
            for (ArrayList<String> parameter : arraysOutput) {
                String nameArg = parameter.get(ArrayInfo.NAME.getIdx());
                postCall += nameArg + "[loop_1] = " + resultVar + "._" + tupleIdx + ";\n" + spaces.toString();
                tupleIdx++;
            }
        }

        buffer.emitCode(code);
        buffer.emitCode(postCall);
    }

    public void visit(InvokeNode invokeNode) throws Exception {
        buffer.emitComment("visited InvokeNode");
        buffer.emitComment(invokeNode.toString(Verbosity.All));
        invokeNode.setUseForInlining(true);

        String functionName = invokeNode.callTarget().targetName();
        String originalName = invokeNode.callTarget().targetName();
        String generatedName = (generatedFunctions.containsKey(functionName)) ? generatedFunctions.get(functionName) : "";
        functionName = functionName.replace(".", "_");

        String returnType = null;
        JavaKind returnKind = null;

        if (functionName.startsWith("ArrayFunction")) {
            handleNestedLambda(invokeNode);
            return;
        } else if ((isGeneratingFunction) && (ParallelOptions.UseSoAWithValue.getValue() != 0)) {

            // XXX: Assuming access to the Tuple -
            // TODO: Improve this control
            if (generatedName.equals("")) {
                generatedName = "1";
            }

            int fieldNum = Integer.parseInt(generatedName);
            returnType = checkDataTypeinTuple(fieldNum - 1, tuplesInputDataTypes);
            if (tuplesInputDataTypes[fieldNum - 1].getArrayDim() != 0) {
                returnType = returnType + " *";
                returnKind = getKindType(tuplesInputDataTypes[fieldNum - 1].getArrayDataType());
            }
        } else {
            returnType = TypeUtil.getTypeFromArrayNonPrimitive(parametersDirection.get(Direction.OUTPUT)[0].getClass());

            if ((returnType == null)) {
                if (!isGeneratingFunction) {
                    String objectType = parametersDirection.get(Direction.OUTPUT)[0].getClass().getComponentType().toString();
                    if (objectSymbolTable.containsKey(objectType)) {
                        returnType = getTupleName(tuplesOutputDataTypes);
                        returnKind = JavaKind.Object;
                    }
                } else {
                    String objectType = parametersDirection.get(Direction.OUTPUT)[0].getClass().getComponentType().toString();
                    if (objectSymbolTable.containsKey(objectType)) {
                        returnType = getTupleName(new AcceleratorType[]{outputDataType});
                    }
                    // TODO: Defaults to float
                    // Support for float by default
                    returnType = "float";
                    returnKind = JavaKind.Float;
                }
            } else if (returnType.equals("[I")) {
                returnType = "int";
                returnKind = JavaKind.Int;
            } else if (returnType.equals("[J")) {
                returnType = "long";
                returnKind = JavaKind.Long;
            } else if (returnType.equals("[D")) {
                returnType = "double";
                returnKind = JavaKind.Double;
            } else if (returnType.equals("[F")) {
                returnType = "float";
                returnKind = JavaKind.Float;
            } else if (returnType.equals("[S")) {
                returnType = "short";
                returnKind = JavaKind.Short;
            } else if (returnType.equals("[B")) {
                returnType = "byte";
                returnKind = JavaKind.Byte;
            } else if (returnType.equals("[C")) {
                returnType = "char";
                returnKind = JavaKind.Char;
            }
        }

        String functionArguments = "";
        String extraString = "";

        // Add extra arguments to the function call in case of a lambda
        if (functionName.equals("Function_apply") || functionName.equals("BiFunction_apply")) {
            functionName = graphLambda.method().getName();
            functionArguments += getExtraParametersForLambdaCall();
        }

        for (ValueNode argument : invokeNode.callTarget().arguments()) {
            String variableName = symbolTable.lookupName(argument);
            if (variableName != null) {
                // If the variable is null means other kind of node that has no ValueNode like
                // |LoadField#function
                if (references.containsKey(argument)) {
                    extraString = references.get(argument);
                }
                functionArguments += (variableName + ",");
            }
        }
        functionArguments = removeLastCharacter(functionArguments);
        functionName = isGeneratingFunction ? "_" + extraString + generatedName : functionName;

        symbolTable.add(functionName, invokeNode, returnKind);

        if ((returnType != null) && (!returnType.equals("void"))) {
            String resultVar = symbolTable.newVariable(SymbolTable.FUNCTION_RESULT);
            symbolTable.add(resultVar, invokeNode, returnKind);

            // Lambda call into buffer
            if (isGeneratingFunction) {
                generateArgumentsForFunction(returnType, generatedName, resultVar, functionName, functionArguments, originalName);
            } else {
                generateCallForLambdaExpression(returnType, resultVar, functionName, functionArguments);
            }
        } else {
            if (!generateExtraArrayInFunctionSignature) {
                buffer.emitCode(functionName + "(" + functionArguments + "); ");
            }
        }

        for (Node successor : invokeNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    private void handleNestedLambda(InvokeNode invokeNode) throws Exception {
        Node currentNode = invokeNode;
        boolean stillSearching = true;

        Vector<NestedLambdaInfo> nestedLambdas = new Vector<>();

        while (stillSearching) {

            for (Node successor : currentNode.cfgSuccessors()) {

                if (currentNode instanceof InvokeNode) {
                    InvokeNode currentInvokeNode = (InvokeNode) currentNode;
                    String currentFunctionName = currentInvokeNode.callTarget().targetName();

                    if (currentFunctionName.endsWith("apply")) {

                        ValueNode firstArgument = currentInvokeNode.callTarget().arguments().get(1);

                        if (!symbolTable.exists(firstArgument)) {
                            this.dispatch(firstArgument);
                        }

                        String inputVar = symbolTable.lookupName(firstArgument);
                        String loopVar = symbolTable.newVariable(SymbolTable.LOOP_VAR);
                        String returnType = "";
                        String resultVar = "";
                        String iterateFor = inputVar + "_index_data[0]";

                        for (NestedLambdaInfo nestedLambda : nestedLambdas) {

                            resultVar = symbolTable.newVariable(SymbolTable.FUNCTION_RESULT);

                            ResolvedJavaType accessingClass = graphLambda.method().getDeclaringClass();
                            Signature signature = nestedLambda.lambdaGraph.method().getSignature();
                            JavaType nestedReturnType = signature.getReturnType(accessingClass);
                            String objectType = nestedReturnType.getName();

                            if (objectSymbolTable.containsKey(objectType)) {
                                returnType = getTupleName(new AcceleratorType[]{nestedLambda.typeInfo.getClassOutput()});
                            } else {
                                returnType = nestedReturnType.toJavaName();
                            }

                            String inputType = signature.getParameterType(signature.getParameterCount(false) - 1, accessingClass).getName();
                            if (nestedLambda.meta.getType() == TypeOfFunction.FUNCTION) {
                                // TODO: Need to dynamically allocate space for the intermediate
                                // result. Use APART?
                                buffer.emitCode(returnType + " " + resultVar + "[10];");

                                buffer.emitCode("for (int " + loopVar + " = 0; " + loopVar + " < " + iterateFor + "; " + loopVar + "++) {");
                                buffer.emitStringNoNL(resultVar + "[" + loopVar + "] = " + nestedLambda.lambdaGraph.method().getName() + "(");

                                for (ValueNode vn : nestedLambda.externalArgs) {
                                    buffer.emitStringNoNL(symbolTable.lookupName(vn) + ", ");
                                }

                                if (inputType.lastIndexOf('[') != -1) {
                                    buffer.emitCode(inputVar + ", " + inputVar + "_index_data + " + inputVar + "_index_data[" + loopVar + "+1]);");
                                } else {
                                    buffer.emitCode(inputVar + "[" + loopVar + "]);");
                                }

                                buffer.emitCode("}");
                            } else {
                                buffer.emitCode(returnType + " " + resultVar + "[1];");

                                buffer.emitCode(resultVar + "[0] = " + inputVar + "[0];");

                                buffer.emitCode("for (int " + loopVar + " = 0; " + loopVar + " < " + iterateFor + "; " + loopVar + "++) {");
                                buffer.emitStringNoNL(resultVar + "[0] = " + nestedLambda.lambdaGraph.method().getName() + "(");

                                for (ValueNode vn : nestedLambda.externalArgs) {
                                    buffer.emitStringNoNL(symbolTable.lookupName(vn) + ", ");
                                }

                                buffer.emitCode(resultVar + "[0], " + inputVar + "[" + loopVar + "]);");
                                buffer.emitCode("}");

                                iterateFor = "1";
                            }

                            inputVar = resultVar;
                        }

                        symbolTable.add(resultVar, currentInvokeNode, JavaKind.Object);
                        if (returnType.contains("Tuple")) {
                            references.put(currentInvokeNode, returnType);
                        }

                        stillSearching = false;

                    } else if (currentFunctionName.contains("interpret")) {
                        // Assuming it's a lambda function
                        ResolvedJavaMethod method = currentInvokeNode.callTarget().targetMethod();
                        JavaConstant[] args = new JavaConstant[1];

                        args[0] = currentInvokeNode.callTarget().arguments().first().asJavaConstant();

                        Constant result = method.invoke(null, args);

                        // Class<?> resultClass = ((HotSpotObjectConstantImpl)
                        // result).getObjectClass(); // HotSpotObjectConstant.asObject(result);
                        // XXX: See changeset
                        // http://hg.openjdk.java.net/graal/graal-jvmci-8/rev/c33f0cb02b34
                        // CHECK THIS CAREFULLY
                        JavaConstant forObject = ((HotSpotResolvedJavaType) result).getJavaClass();
                        Class<?> resultClass = forObject.getJavaKind().toJavaClass();   // / XXX:

                        GraalJVMCICompiler c = (GraalJVMCICompiler) JVMCI.getRuntime().getCompiler();
                        RuntimeProvider runtimeProvider = c.getGraalRuntime().getCapability(RuntimeProvider.class);
                        Providers providers = runtimeProvider.getHostBackend().getProviders();

                        ResolvedJavaType lambdaType = ((HotSpotMetaAccessProvider) providers.getMetaAccess()).lookupJavaType(resultClass);

                        MapToGraal mtg = new MapToGraal();
                        mtg.createGraalIRForLambda(resultClass);
                        StructuredGraph nestedLambdaGraph = mtg.getGraphLambda();

                        NestedLambdaInfo newLambda = new NestedLambdaInfo(nestedLambdaGraph, lambdaType, typeInfoOCL.getNested().get(nestedLambdas.size()));
                        nestedLambdas.add(newLambda);

                        generateNestedLambda(newLambda);
                    }
                } else if (currentNode instanceof NewInstanceNode) {
                    // Assuming it's a lambda function, get the graph
                    NewInstanceNode newInstanceNode = (NewInstanceNode) currentNode;
                    HotSpotResolvedJavaType lambdaType = (HotSpotResolvedJavaType) newInstanceNode.instanceClass();

                    MapToGraal mtg = new MapToGraal();
                    mtg.createGraalIRForLambda(lambdaType.mirror());
                    StructuredGraph nestedLambdaGraph = mtg.getGraphLambda();

                    NestedLambdaInfo newLambda = new NestedLambdaInfo(nestedLambdaGraph, lambdaType, typeInfoOCL.getNested().get(nestedLambdas.size()));
                    nestedLambdas.add(newLambda);

                    generateNestedLambda(newLambda);
                } else if (currentNode instanceof StoreFieldNode) {
                    StoreFieldNode storeFieldNode = (StoreFieldNode) currentNode;

                    ValueNode p = storeFieldNode.value();

                    // arg$ followed by the number of the argument
                    int fieldNum = Integer.parseInt(storeFieldNode.field().getName().split("\\$")[1]);

                    NestedLambdaInfo lastLambda = nestedLambdas.lastElement();
                    lastLambda.externalArgs.add(fieldNum - 1, p);

                }

                currentNode = successor;
            }
        }

        for (Node successor : currentNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    private void generateNestedLambda(NestedLambdaInfo nestedLambdaInfo) throws Exception {

        StructuredGraph nestedLambdaGraph = nestedLambdaInfo.lambdaGraph;

        SymbolPhases symbolPhases = new SymbolPhases();
        symbolPhases.applyPhases(nestedLambdaGraph);
        SymbolTable lambdaTable = new SymbolTable(symbolPhases.getArrayAccessDim(), symbolPhases.getArrayDimensions());

        GraalOpenCLGenerator cgen = new GraalOpenCLGenerator(GraalAcceleratorOptions.debugOCLKernel, lambdaTable);
        cgen.setLambdaGraph(nestedLambdaGraph);

        LambdaFunctionMetadata lambdaMeta = nestedLambdaInfo.meta;
        AcceleratorOCLInfo nestedTypeInfo = nestedLambdaInfo.typeInfo;

        Signature lambdaSignature = nestedLambdaGraph.method().getSignature();
        int parameterCount = lambdaSignature.getParameterCount(false);

        // Adjust the parameter count for bifunction to match what
        // would be passed by the API
        if (lambdaMeta.getType() == TypeOfFunction.BIFUNCTION) {
            parameterCount--;
        }

        Object[] inputArgs1 = new Object[parameterCount];
        Class<?> outputClass = nestedTypeInfo.getOCLOutput().getClass();

        inputArgs1[0] = Array.newInstance(nestedTypeInfo.getOCLInput().getClass(), 1);

        for (int i = 0; i < nestedTypeInfo.getOCLScope().length; i++) {
            inputArgs1[i + 1] = nestedTypeInfo.getOCLScope()[i];
        }

        GPUParameters params = new GPUParameters();
        params.putInput(inputArgs1);
        params.setOutput(Array.newInstance(outputClass, 1));
        cgen.setParametersDirection(params.getParameters());

        Integer oldSoaValue = ParallelOptions.UseSoAWithValue.getValue();
        ParallelOptions.UseSoAWithValue.setValue(nestedTypeInfo.getClassInput().getType().getNumAttributes());

        cgen.generateLambdaKernel(nestedLambdaGraph, lambdaMeta, nestedTypeInfo);
        nestedFunctions.addFirst(cgen.buffer);
        structTypedefs.addAll(cgen.structTypedefs);

        Iterator<CodeBuffer> iterator = cgen.nestedFunctions.descendingIterator();
        while (iterator.hasNext()) {
            nestedFunctions.addFirst(iterator.next());
        }
        ParallelOptions.UseSoAWithValue.setValue(oldSoaValue);
    }

    @Override
    public void visit(KillingBeginNode killingBeginNode) {
        // TODO: works, but might need to check for differences from BeginNode
        buffer.emitComment("visited KillingBeginNode");
        buffer.emitComment(killingBeginNode.toString(Verbosity.All));
        symbolTable.enterScope();

        for (Node node : killingBeginNode.cfgSuccessors()) {
            this.dispatch(node);
        }
        symbolTable.exitScope();
    }

    // FIXME: Partial solution. FastR dependency
    private static boolean isLegalFromR(String objectName, String fieldName) {
        if (objectName.startsWith("RDoubleVector") && !fieldName.startsWith("complete")) {
            return false;
        } else if (objectName.startsWith("RErrorHandling")) {
            return false;
        } else if (!objectName.startsWith("RDoubleVector") && !fieldName.startsWith("attribute")) {
            return true;
        }
        return true;
    }

    public void generateCodeForFieldNode(LoadFieldNode loadFieldNode, ValueNode objectNode) {

        String objectName = symbolTable.lookupName(objectNode);
        JavaKind kindObject = symbolTable.lookupType(objectName);
        String fieldName = loadFieldNode.field().getName();

        // Could use Node.graph() and so on to determine whether it's an instance
        // method or not to know whether to use . or ->

        if (loadFieldNode.field().getJavaKind().isPrimitive()) {

            String fieldType = loadFieldNode.field().getJavaKind().getJavaName();
            String fieldVar = symbolTable.newVariable(SymbolTable.FIELD_VALUE);
            String code = "";
            JavaKind type = loadFieldNode.field().getJavaKind();
            if (kindObject == JavaKind.Object) {
                if (isTruffleFrontEnd() && objectName.startsWith("RDoubleVector") && fieldName.startsWith("complete")) {
                    code = " // not valid: access to object";
                    type = JavaKind.Illegal;
                } else if (loadFieldNode.getAnnotation() == KnownType.class) {
                    code = " // not valid: access to object : DynamicObjectField";
                    type = JavaKind.Illegal;
                    if (inputAliasList.contains(objectName)) {
                        inputAliasList.add(fieldVar);
                    }
                } else {
                    code = fieldType + " " + fieldVar + " = " + objectName + "." + fieldName + "; ";
                }
            } else {
                code = fieldType + " " + fieldVar + " = " + objectName + "; ";
            }
            buffer.emitCode(code);
            symbolTable.add(fieldVar, loadFieldNode, type);

        } else if (loadFieldNode.field().getType().getComponentType() != null) {

            // This case is for 2D arrays

            // TODO: Complete array
            // Assume it's an integer array
            String fieldVar = symbolTable.newVariable(SymbolTable.FIELD_VALUE);
            String fieldType = "int";
            symbolTable.add(fieldVar, loadFieldNode, JavaKind.Int);
            String code = "__global " + fieldType + " *" + fieldVar + " = " + objectName + "." + fieldName + ";\n";
            String spaces = buffer.getSpaces().toString();
            code += spaces + "__global int *" + fieldVar + "_index_data = " + objectName + "." + fieldName + "_index_data;\n";
            code += spaces + "int " + fieldVar + "_dim_1 = 0;\n ";

            buffer.emitCode(code);
        } else {
            // General case

            // Find the object in the globalObjectsTable that was built at the beginning of the code
            // generation.
            String fieldObjectType = null;
            if (references.containsKey(loadFieldNode.getValue())) {
                String name = references.get(loadFieldNode.getValue());
                if (globalObjectTable.containsKey(name)) {
                    HashMap<String, String> t = globalObjectTable.get(name);
                    if (t.containsKey(fieldName)) {
                        fieldObjectType = t.get(fieldName);
                    }
                }
            }

            // If there is no object --> set up a float by default
            String fieldType = (fieldObjectType != null) ? fieldObjectType : "float";

            // Get a new name
            String fieldVar = symbolTable.newVariable(SymbolTable.FIELD_VALUE);

            // Emit the code
            String code = "";
            if (objectName != null) {
                if (kindObject == JavaKind.Object) {
                    if (loadFieldNode.getAnnotation() == KnownType.class) {
                        // code += "\n // not valid: access to object : DynamicObjectField";
                        if (inputAliasList.contains(objectName)) {
                            inputAliasList.add(fieldVar);
                        }
                    } else if (isLegalFromR(objectName, fieldName)) {
                        code = fieldType + " " + fieldVar + " = " + objectName + "." + fieldName + "; ";
                    }
                } else {
                    code = fieldType + " " + fieldVar + " = " + objectName + ";";
                }

                // Emit the final code for the LoadFieldNode
                buffer.emitCode(code);

                // Add the enw variable into the symbol table
                symbolTable.add(fieldVar, loadFieldNode, loadFieldNode.field().getJavaKind());
            }
        }
    }

    @Override
    public void visit(LoadFieldNode loadFieldNode) {
        buffer.emitComment("visited LoadFieldNode");
        buffer.emitComment(loadFieldNode.toString(Verbosity.All));

        ValueNode objectNode = loadFieldNode.getValue();

        if (!symbolTable.exists(objectNode)) {
            this.dispatch(objectNode);
        }

        if (scopedNodes.indexOf(loadFieldNode.field()) != -1 || scopedNodes.indexOf(loadFieldNode) != -1 || scopedNodes.indexOf(objectNode) != -1) {
            symbolTable.add(JavaKind.Illegal.getJavaName(), loadFieldNode, JavaKind.Illegal);
        } else {
            generateCodeForFieldNode(loadFieldNode, objectNode);
        }

        for (Node node : loadFieldNode.cfgSuccessors()) {
            this.dispatch(node);
        }
    }

    @SuppressWarnings("unused")
    private static boolean isPowerOfTwo(int n) {
        Double log2 = Math.log(n) / Math.log(2);
        if (log2 - log2.intValue() > 0) {
            return false;
        }
        return true;
    }

    private static boolean isPowerOfTwoBitWise(int n) {
        return ((n & (n - 1)) == 0);
    }

    @SuppressWarnings("rawtypes")
    private String generateOptimizationSequence(int indexTuple, PArray parray, String realOCLType, String indexVar, String arrayStart) {
        String codeOpenCL = "";
        if (parray.isSequence(indexTuple)) {
            // We previously check the type of the PArray to Tuple2
            Tuple2 t2 = (Tuple2) parray.get(indexTuple);
            String loopIndex = indexVar;
            if (indexTuple == 0) {
                Tuple2 nextTuple = (Tuple2) parray.get(1);
                Tuple2 t3 = (Tuple2) parray.get(2);

                if (parray.isFlag(indexTuple)) {
                    if (!isPowerOfTwoBitWise((int) t3._1)) {
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) / " + t3._1 + ";";
                    } else {
                        // i / n, if N is power of 2 then i >> log2(n)
                        int base2 = (int) (Math.log((int) t3._1) / Math.log(2));
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) >> " + base2 + ";";
                    }
                    buffer.emitCode(codeOpenCL);
                    loopIndex = "m" + indexTuple;
                    codeOpenCL = realOCLType + " " + "__auxVariable" + indexTuple + " = " + t2._1 + " + " + nextTuple._1 + " * " + loopIndex + " ;";
                } else if (parray.isCompass(indexTuple)) {

                    if (!isPowerOfTwoBitWise((int) t3._1)) {
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) % " + t3._1 + ";";
                    } else {
                        // Modulo
                        // i % n = i & (n - 1)
                        int n = (int) t3._1;
                        int nn = n - 1;
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) & " + nn + ";";
                    }

                    buffer.emitCode(codeOpenCL);
                    loopIndex = "m" + indexTuple;
                    codeOpenCL = realOCLType + " " + "__auxVariable" + indexTuple + " = " + t2._1 + " + " + nextTuple._1 + " * " + loopIndex + " ;";
                } else {
                    codeOpenCL = realOCLType + " " + "__auxVariable" + indexTuple + " = " + t2._1 + " + " + nextTuple._1 + " * (" + loopIndex + " <offsetDevice>);";
                }
            } else {
                Tuple2 tupleBefore = (Tuple2) parray.get(0);
                Tuple2 t3 = (Tuple2) parray.get(2);

                if (parray.isFlag(indexTuple)) {

                    if (!isPowerOfTwoBitWise((int) t3._2)) {
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) / " + t3._2 + ";";
                    } else {
                        // i / n, if N is power of 2 then i >> log2(n)
                        int base2 = (int) (Math.log((int) t3._2) / Math.log(2));
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) >> " + base2 + ";";
                    }
                    buffer.emitCode(codeOpenCL);
                    loopIndex = "m" + indexTuple;
                    codeOpenCL = realOCLType + " " + "__auxVariable" + indexTuple + " = " + tupleBefore._2 + " + " + t2._2 + " * " + loopIndex + ";";
                } else if (parray.isCompass(indexTuple)) {
                    if (!isPowerOfTwoBitWise((int) t3._2)) {
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) % " + t3._2 + ";";
                    } else {
                        // Modulo
                        // i % n = i & (n - 1)
                        int n = (int) t3._2;
                        int nn = n - 1;
                        codeOpenCL = "int m" + indexTuple + " = ( " + loopIndex + " <offsetDevice> ) & " + nn + ";";
                    }
                    buffer.emitCode(codeOpenCL);
                    loopIndex = "m" + indexTuple;
                    codeOpenCL = realOCLType + " " + "__auxVariable" + indexTuple + " = " + tupleBefore._2 + " + " + t2._2 + " * " + loopIndex + ";";
                } else {
                    codeOpenCL = realOCLType + " " + "__auxVariable" + indexTuple + " = " + tupleBefore._2 + " + " + t2._2 + " * (" + loopIndex + " <offsetDevice> );";
                }
            }

        } else {
            // Bailout to general case
            codeOpenCL = realOCLType + " " + arrayStart + " " + "__auxVariable" + indexTuple + " = " + tuplesManagement.getNameAt(indexTuple) + "[" + indexVar + "]; // Bailout general case";
        }
        return codeOpenCL;
    }

    @SuppressWarnings("rawtypes")
    private String handleOptimizationSequence(int i, String realOCLType, String indexVar, String arrayStart) {
        String codeOpenCL = null;
        PArray parray = (PArray) parametersDirection.get(Direction.INPUT)[0];

        if (parray.get(i) instanceof Tuple2) {
            // XXX: For now we allow two arrays in sequence format
            codeOpenCL = generateOptimizationSequence(i, parray, realOCLType, indexVar, arrayStart);
        } else {
            // Input is just one array
            if (parray.isFlag()) {
                if (!isPowerOfTwoBitWise((int) parray.get(2))) {
                    codeOpenCL = "int m = ( " + indexVar + " <offsetDevice> ) / " + parray.get(2) + ";\n";
                } else {
                    // i / n, if N is power of 2 then i >> log2(n)
                    int base2 = (int) (Math.log((int) parray.get(2)) / Math.log(2));
                    codeOpenCL = "int m = ( " + indexVar + " <offsetDevice> ) >> " + base2 + ";\n";
                }
                buffer.emitCode(codeOpenCL);
                codeOpenCL = realOCLType + " " + "__auxVariable" + i + " = " + parray.get(0) + " + " + parray.get(1) + " *  m ;";
            } else if (parray.isCompass()) {

                if (!isPowerOfTwoBitWise((int) parray.get(2))) {
                    codeOpenCL = "int m = ( " + indexVar + " <offsetDevice> ) % " + parray.get(2) + ";\n";
                } else {
                    // Modulo
                    // i % n = i & (n - 1)
                    int n = (int) parray.get(2);
                    int nn = n - 1;
                    codeOpenCL = "int m = ( " + indexVar + " <offsetDevice> ) & " + nn + ";\n";
                }

                buffer.emitCode(codeOpenCL);
                codeOpenCL = realOCLType + " " + "__auxVariable" + i + " = " + parray.get(0) + " + " + parray.get(1) + " *  m ;";
            } else {
                codeOpenCL = realOCLType + " " + "__auxVariable" + i + " = " + parray.get(0) + " + " + parray.get(1) + " * (" + indexVar + " <offsetDevice> );";
            }
        }
        return codeOpenCL;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void visit(LoadIndexedNode loadIndexedNode) {

        buffer.emitComment("visited LoadIndexedNode");
        buffer.emitComment(loadIndexedNode.toString(Verbosity.All));

        if (!symbolTable.exists(loadIndexedNode.array())) {
            this.dispatch(loadIndexedNode.array());
        }

        if (!symbolTable.exists(loadIndexedNode.index())) {
            this.dispatch(loadIndexedNode.index());
        }

        if (symbolTable.lookupArrayAccessInfo(loadIndexedNode) != null) {
            ArrayDepth arrayAccess = symbolTable.lookupArrayAccessInfo(loadIndexedNode);
            String variable = symbolTable.lookupName(arrayAccess.getNode());
            String indexVar = symbolTable.lookupName(loadIndexedNode.index());
            String arrayVar = symbolTable.lookupName(loadIndexedNode.array());
            String paramArray = symbolTable.lookupName(arrayAccess.getNode());
            int dim = arrayAccess.getDimensionAccessedAt();

            boolean completeAccess = (arrayAccess.getNode() == null) ? true : false;

            if ((arrayAccess.getNode() != null) && (dim == symbolTable.lookupArrayDimension(arrayAccess.getNode()))) {

                String arrayType = null;
                try {
                    arrayType = symbolTable.lookupType(arrayVar).getJavaName();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("ARRAY VAR NOT FOUND ");
                }

                if ((!simpleDataTypeLambdaParameter) && arrayType.equals("Object") && (isTupleEnabled)) {
                    // In this point we know perfectly that the variable is tuple
                    if (references.containsKey(arrayAccess.getNode())) {
                        arrayType = references.get(arrayAccess.getNode());

                        String codeOpenCL = "";
                        int numElements = tuplesManagement.getNumElements();
                        // int structNum = numElements;
                        // if (inputDataType.getNumAttributes() != -1) {
                        // structNum = inputDataType.getNumAttributes();
                        // }

                        ArrayList<String> singleVariableName = new ArrayList<>();

                        for (int i = 0; i < numElements; i++) {
                            String arrayStart = "";
                            String realOCLType = checkDataTypeinTuple(i, tuplesInputDataTypes);

                            if (i == 0) {
                                singleVariableName.add("__auxVariable" + i);
                            }

                            if (realOCLType == null) {
                                // It is an array in the tuple
                                realOCLType = tuplesInputDataTypes[i].getArrayDataType().getOCLName();
                                ArrayList<String> elemI = new ArrayList<>();
                                elemI.add("__auxVariable" + i);
                                elemI.add(new Integer(i).toString());
                                elemI.add(realOCLType);
                                ExtraArrayNamesManager.getInstance().addInput(elemI);

                                codeOpenCL = "__global " + realOCLType + " * " + "__auxVariable" + i + " = &" + tuplesManagement.getNameAt(i) + "[" + indexVar;
                                codeOpenCL += " * " + tuplesManagement.getNameAt(i) + "_index_data[2]";
                                codeOpenCL += "];";
                            } else if ((parametersDirection.get(Direction.INPUT)[0] instanceof PArray) && ((PArray) (parametersDirection.get(Direction.INPUT)[0])).isSequence()) {

                                codeOpenCL = handleOptimizationSequence(i, realOCLType, indexVar, arrayStart);

                            } else {
                                codeOpenCL = realOCLType + " " + arrayStart + " " + "__auxVariable" + i + " = " + tuplesManagement.getNameAt(i) + "[" + indexVar + "];  ";
                            }
                            buffer.emitCode(codeOpenCL);
                        }

                        // XXX: Assuming ArrayJPAI<Tuple> unroll so the number of parameters have to
                        // be more than 1.
                        if (numElements > 1) {
                            codeOpenCL = getTupleName(tuplesInputDataTypes) + " s;";
                            buffer.emitCode(codeOpenCL);
                            int numField = 1;
                            for (int i = 0; i < numElements; i++) {
                                // Only generates generate the struct with simple data types
                                if (tuplesInputDataTypes[i].isValid()) {
                                    codeOpenCL = "s._" + (numField) + " = __auxVariable" + (numField - 1) + ";";
                                    buffer.emitCode(codeOpenCL);
                                    numField++;
                                }
                            }
                            symbolTable.add("s", loadIndexedNode, JavaKind.Float);
                        } else {
                            symbolTable.add(singleVariableName.get(0), loadIndexedNode, JavaKind.Float);
                        }
                    }
                } else {
                    // Not a tuple, so a primitive, load the value
                    String valueVar = symbolTable.newVariable(SymbolTable.ARRAY_ELEMENT);
                    StringBuffer code = new StringBuffer();
                    JavaKind kind = symbolTable.lookupType(arrayVar);

                    int index = 0;
                    try {
                        index = Integer.parseInt(indexVar);
                    } catch (NumberFormatException e) {
                    }

                    if (isTruffleFrontEnd() && (index >= R_START_PARAM_INDEX)) {
                        int elem = index - R_START_PARAM_INDEX;
                        if (elem >= inputArgs) {
                            // This is a scope variable (most probably an array) after node
                            // rewriting in Truffle.
                            int scopeIndex = (elem - inputArgs) * 2;
                            ScopeTruffle scopeTruffle = scopeTruffleList.get(scopeIndex);
                            String name = scopeTruffle.getName();
                            String type = scopeTruffle.getGlobalType();
                            code.append(type + " " + valueVar + " = " + name + "; // SCOPE VAR DETECTED with Truffle Rewritting");
                        } else {
                            String structTypeName = structType.get(paramArray);
                            if (structTypeName != null) {
                                // This is a Tuple
                                int idx = Math.abs((R_START_PARAM_INDEX - index)) + 1;
                                String functionToCall = "_" + structTypeName + idx;
                                arrayType = globalObjectTable.get(structTypeName).get("_" + idx);
                                paramArray = functionToCall + "(" + paramArray + ")";
                            }
                            if (!arrayType.equals(JavaKind.Illegal.getJavaName())) {
                                code.append(arrayType + " " + valueVar + " = " + paramArray + "; ");
                            }
                        }
                    } else if (isTruffleFrontEnd() && (index == R_INDEX_IS_IRREGULAR)) {
                        code.append("// is Irregular: TODO: disable in the TruffleR interpreter");
                    } else if (isTruffleFrontEnd() && (index == R_INDEX_DEPTH)) {
                        code.append("// Check for depth disabled: TODO - disable in the interpreter level");
                    } else {
                        if (arrayType.equals(JavaKind.Illegal.getJavaName())) {
                            // This is the scope variable detected with the Truffle Scope Pattern
                            arrayType = scopeTruffleList.get(0).getType();
                            String valueVariable = scopeTruffleList.get(0).getName();
                            code.append(arrayType + " " + valueVar + " = " + valueVariable + "[" + indexVar + "]; ");
                        } else {
                            code.append(arrayType + " " + valueVar + " = " + paramArray + "[" + variable + "_index_data[" + variable);
                            if ((metadata.getType() == LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION) && (indexVar.startsWith("loop_"))) {
                                code.append("_dim_" + dim + " + 1] + " + " reductionIndex " + "];  ");
                            } else {
                                code.append("_dim_" + dim + " + 1] + " + indexVar + "]; ");
                            }
                        }
                    }
                    buffer.emitCode(code.toString());
                    symbolTable.add(valueVar, loadIndexedNode, kind);
                }

            } else if (completeAccess) {
                String arrayType = symbolTable.lookupType(arrayVar).getJavaName();
                if (arrayType.equals("Object") && references.containsKey(loadIndexedNode.array())) {
                    arrayType = references.get(loadIndexedNode.array());
                    references.put(loadIndexedNode, arrayType);
                }

                String valueVar = symbolTable.newVariable(SymbolTable.ARRAY_ELEMENT);
                JavaKind kind = symbolTable.lookupType(arrayVar);
                indexVar = symbolTable.lookupName(loadIndexedNode.index());
                arrayVar = symbolTable.lookupName(loadIndexedNode.array());
                variable = arrayVar;

                String code = "";

                if (isTruffleFrontEnd && (scopeTruffleList != null) && scopeTruffleList.size() > 0) {
                    ValueNode array = loadIndexedNode.array();

                    if (array instanceof ConstantNode) {
                        Integer position = arrayConstantIndex.get(array);
                        if (position == null) {
                            position = 0;
                        }
                        arrayType = scopeTruffleList.get(position).getType();
                        variable = scopeTruffleList.get(position).getName();
                        code = arrayType + " " + valueVar + " = " + variable + "[" + indexVar + "]; ";

                    } else {
                        code = "// We can not handle this case: " + loadIndexedNode.array();
                    }

                } else {

                    // XXX: RUBY SPECIFIC FOR THE FRAME LAYOUT : COME OUT WITH A GENERIC SOLUTION
                    if (isTruffleFrontEnd()) {
                        // Coming from the RUBY Truffle
                        valueVar = symbolTable.newVariable(SymbolTable.ARRAY_ELEMENT);
                        StringBuffer codeBuffer = new StringBuffer();
                        kind = symbolTable.lookupType(arrayVar);

                        int index = 0;
                        try {
                            index = Integer.parseInt(indexVar);
                        } catch (NumberFormatException e) {
                        }

                        if (isTruffleFrontEnd() && (index >= RUBY_INDEX_START)) {
                            String structTypeName = structType.get(paramArray);

                            // Preparation if it is a Tuple
                            if (structTypeName != null) {
                                // This is a Tuple
                                int idx = Math.abs((R_START_PARAM_INDEX - index)) + 1;
                                String functionToCall = "_" + structTypeName + idx;
                                arrayType = globalObjectTable.get(structTypeName).get("_" + idx);
                                paramArray = functionToCall + "(" + paramArray + ")";
                            }

                            // Generate the access
                            if (!arrayType.equals(JavaKind.Illegal.getJavaName())) {
                                codeBuffer.append(arrayType + " " + valueVar + " = " + variable + "; // RUBY !!!!!!!!!!");
                                code = codeBuffer.toString();

                                if (inputAliasList.contains(variable)) {
                                    // We do know is the input (inference)
                                    inputAliasList.add(valueVar);
                                }

                            } else {
                                code = "// Illegal Java Name";
                            }
                        } else {

                            // If the LoadIndexedNode has the input annotation => we generate code.
                            if (loadIndexedNode.getAnnotation() == KnownType.class) {
                                // This is a Tuple

                                if (inputAliasList.contains(variable)) {
                                    // We do know is the input (inference)
                                    paramArray = inputAliasList.get(0);
                                } else {
                                    throw new RuntimeException("Input Variable Not in the List (annotated with KnownType)");
                                }

                                String structTypeName = structType.get(paramArray);

                                int idx = Math.abs((index)) + 1;
                                String functionToCall = "_" + structTypeName + idx;
                                arrayType = globalObjectTable.get(structTypeName).get("_" + idx);
                                paramArray = functionToCall + "(" + paramArray + ")";
                                code += arrayType + " " + valueVar + " = " + paramArray + "; ";
                            }

                            // code += " // ignored : " + arrayType + " " + valueVar + " = " +
                            // variable + "; \n";
                            symbolTable.add(valueVar, loadIndexedNode, kind);
                        }
                    } else {
                        // Generic case, it does not come from Truffle languages
                        code = arrayType + " " + valueVar + " = " + variable + "[" + indexVar + "];";
                    }
                }

                if (!code.equals(" ") && !code.equals("")) {
                    buffer.emitCode(code);
                    symbolTable.add(valueVar, loadIndexedNode, kind);
                }

            } else {
                // Not at the innermost dimension yet, add the info for the next dimension
                String newVar = variable + "_dim_" + (dim + 1);
                JavaKind kind = symbolTable.lookupType(arrayVar);
                if (symbolTable.lookupNode(newVar) == null) {
                    String code = "int " + newVar + " = " + variable + "_index_data[" + variable + "_dim_" + (dim) + " + " + indexVar + " + 1];";
                    buffer.emitCode(code);
                }
                symbolTable.add(newVar, loadIndexedNode, kind);
            }

            for (Node node : loadIndexedNode.successors()) {
                this.dispatch(node);
            }
        } else {
            buffer.emitComment("LoadIndexedNode() - Object is not an array.");
        }
    }

    @Override
    public void visit(ParameterNode paramNode) {
        // LocalNodes get added to the symbol symbolTable earlier
        buffer.emitComment("visited ParameterNode");
        for (Node node : paramNode.cfgSuccessors()) {
            this.dispatch(node);
        }
    }

    @Override
    public void visit(LogicConstantNode logicConstantNode) {
        buffer.emitComment("visited LogicConstantNode");
        buffer.emitComment(logicConstantNode.toString(Verbosity.All));

        String varName = symbolTable.newVariable(SymbolTable.CONDITION_VAR);
        symbolTable.add(varName, logicConstantNode, JavaKind.Boolean);

        buffer.emitCode("const bool " + varName + " = " + logicConstantNode.getValue() + ";");
    }

    private static String getLoopExpressionForFunction() {
        return "for( ; ; )";
    }

    @Override
    public void visit(LoopBeginNode loopBeginNode) {
        buffer.emitComment("visited LoopBeginNode");
        buffer.emitComment(loopBeginNode.toString(Verbosity.All));

        // get current loop data for this loop.
        LoopEx thisLoop = loopsData.loop(loopBeginNode);

        // check if this loop has a parent if it has then we don't parallelise this one
        // if this loop doesn't have a parent then this is the parallelisable loop as
        // for now we only parallelise outermost loops.
        boolean isParallelLoop = (thisLoop.parent() == null);
        isParallelLoop = (this.metadata.getType() == LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION) || (isGeneratingFunction) ? false : isParallelLoop;

        for (PhiNode phi : loopBeginNode.phis()) {

            // now generate code for this node.
            String type = phi.getStackKind().getJavaName();
            String initialValue = symbolTable.lookupName(phi.firstValue());

            if (isParallelLoop && !isGeneratingFunction) {
                // don't use actual initial loop iteration variable value for now though.
                // use get_global_id(0) OpenCL function call.
                if (this.metadata.getType() != LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION) {
                    initialValue = "get_global_id(0)";
                }
                // } else {
                // // / XXX: it can be another value != 0
                // initialValue = "0";
                // }
                String localVarName = symbolTable.newVariable(SymbolTable.LOOP_VAR);

                symbolTable.add(localVarName, phi, phi.getStackKind());
                buffer.emitCode(type + " " + localVarName + " = " + initialValue + "; // Initial value");

            } else if (!isParallelLoop && !isGeneratingFunction) {

                String localVarName = symbolTable.newVariable(SymbolTable.LOOP_VAR);
                symbolTable.add(localVarName, phi, phi.getStackKind());
                buffer.emitCode(type + " " + localVarName + " = " + initialValue + "; ");
                symbolTable.add(localVarName, phi, phi.getStackKind());

            } else if (isGeneratingFunction) {

                String loopVariableName = symbolTable.newVariable(SymbolTable.LOOP_VAR);
                symbolTable.add(loopVariableName, phi, phi.getStackKind());

                if (initialValue.equals("null")) {
                    // XXX: throw an exception? We do need the initial value
                    initialValue = "0";
                }

                if (type.equals("Object")) {
                    // return type function
                    type = returnTypeFunction;
                }

                buffer.emitCode(type + " " + loopVariableName + " = " + initialValue + "; // LoopBegin");
                symbolTable.add(loopVariableName, phi, phi.getStackKind());

            }
        }

        boolean isExplicitLoop = true;
        if (isGeneratingFunction) {
            String code = getLoopExpressionForFunction();
            buffer.emitCode(code);
        } else {
            if (this.metadata.getType() == LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION) {
                // Sequential reduction loop ==> we know how is the reduction in the LambdaToOCL
                // class
                int max = this.metadata.getElements();
                buffer.emitStringNoNL("for (int reductionIndex = 1 ; reductionIndex < " + max + " ; reductionIndex++)");
            } else {
                if (isParallelLoop) {
                    // String iterVar = symbolTable.lookupName(inductionVariable.valueNode());
                    // then use the get_global_size stuff.
                    // buffer.emitStringNoNL("for ( ; ; " + iterVar + " += gs)");
                    isExplicitLoop = false;
                } else {
                    buffer.emitStringNoNL("for ( ; ; )");
                }
            }
        }
        buffer.emitCode("{");
        explicitLoop.push(isExplicitLoop);
        buffer.beginBlock();

        for (Node succ : loopBeginNode.cfgSuccessors()) {
            this.dispatch(succ);
        }

        buffer.endBlock();
        explicitLoop.pop();
        buffer.emitCode("}");
    }

    @Override
    public void visit(LoopEndNode loopEndNode) {
        buffer.emitComment("visited LoopEndNode");
        buffer.emitComment(loopEndNode.toString(Verbosity.All));

        LoopBeginNode loopBeginNode = loopEndNode.loopBegin();

        if ((isGeneratingFunction) || (loopsData.loop(loopBeginNode).parent() != null)) {
            for (PhiNode phiNode : loopEndNode.merge().phis()) {
                ValueNode valueAt = phiNode.valueAt(loopEndNode);
                if (!symbolTable.exists(valueAt)) {
                    this.dispatch(valueAt);
                }
            }

            // Emit Phi Values
            for (PhiNode phiNode : loopEndNode.merge().phis()) {
                String phiVarName = symbolTable.lookupName(phiNode);
                String resultVar = symbolTable.lookupName(phiNode.valueAt(loopEndNode));

                buffer.emitCode(phiVarName + " = " + resultVar + ";  ");
            }
        }

        for (Node node : loopEndNode.cfgSuccessors()) {
            this.dispatch(node);
        }
    }

    @Override
    public void visit(LoopExitNode loopExitNode) {
        buffer.emitComment("visited LoopExitNode");
        buffer.emitComment(loopExitNode.toString(Verbosity.All));

        for (Node node : loopExitNode.successors()) {
            this.dispatch(node);
        }

        if (explicitLoop.peek()) {
            buffer.compareNotReturnAndEmit("break;");
        }
    }

    @Override
    public void visit(MergeNode mergeNode) {
        buffer.emitComment("visited MergeNode");
        buffer.emitComment(mergeNode.toString(Verbosity.All));

        for (Node successor : mergeNode.successors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(NormalizeCompareNode normalizeCompareNode) {
        buffer.emitComment("visited NormalizeCompareNode");
        buffer.emitComment(normalizeCompareNode.toString(Verbosity.All));
        buffer.emitComment(normalizeCompareNode.getX().toString(Verbosity.All));
        buffer.emitComment(normalizeCompareNode.getY().toString(Verbosity.All));

        if (!symbolTable.exists(normalizeCompareNode.getX())) {
            this.dispatch(normalizeCompareNode.getX());
        }
        if (!symbolTable.exists(normalizeCompareNode.getY())) {
            this.dispatch(normalizeCompareNode.getY());
        }

        String xVar = symbolTable.lookupName(normalizeCompareNode.getX());
        String yVar = symbolTable.lookupName(normalizeCompareNode.getY());

        String conditionVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);
        symbolTable.add(conditionVar, normalizeCompareNode, JavaKind.Int);

        // Returns -1, 0, or 1 if either x < y, x == y, or x > y
        // emitted as result = x < y ? -1 : x == y ? 0 : 1
        String code = "int " + conditionVar + " = " + xVar + " < " + yVar + " ? " + " -1 : " + xVar + " == " + yVar;
        code += " ? 0 : 1;";
        buffer.emitCode(code);
    }

    @Override
    public void visit(ReturnNode returnNode) {
        buffer.emitComment("visited ReturnNode");
        buffer.emitComment(returnNode.toString(Verbosity.All));
        ValueNode valueNode = returnNode.result();

        if (valueNode != null) {
            // Generate code for the Lambda Function, not for the skeleton
            if (isGeneratingFunction) {
                // It only returns a value when the kernel in a normal function
                if ((!isTupleEnabledSignature) && (!returnNodes.contains(valueNode))) {
                    returnNodes.add(valueNode);
                    this.dispatch(valueNode);
                }

                if (valueNode instanceof AllocatedObjectNode) {
                    if (!symbolTable.exists(valueNode)) {
                        this.dispatch(valueNode);
                    }
                }

                StringBuffer returnValue = new StringBuffer();
                returnValue.append(symbolTable.lookupName(valueNode));
                StringBuffer statement = new StringBuffer("return ");
                statement.append(returnValue + ";");
                buffer.compareAndEmitString(statement.toString());
            }
        }

        for (Node node : returnNode.cfgSuccessors()) {
            this.dispatch(node);
        }
    }

    @Override
    public void visit(StartNode startNode) {
        for (Node successor : startNode.successors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(StoreIndexedNode storeIndexedNode) {
        buffer.emitComment("visited StoreIndexedNode");
        buffer.emitComment(storeIndexedNode.toString(Verbosity.All));

        if (!symbolTable.exists(storeIndexedNode.array())) {
            this.dispatch(storeIndexedNode.array());
        }
        if (!symbolTable.exists(storeIndexedNode.index())) {
            this.dispatch(storeIndexedNode.index());
        }

        if (symbolTable.lookupArrayAccessInfo(storeIndexedNode) != null) {
            ArrayDepth accessInfo = symbolTable.lookupArrayAccessInfo(storeIndexedNode);

            // Get parameter array name.
            String arrayName = symbolTable.lookupName(accessInfo.getNode());

            // get index in array to store value at.
            String indexVar = symbolTable.lookupName(storeIndexedNode.index());

            // int dim = accessInfo.getDimensionAccessedAt();

            // get value to be stored.
            if (!symbolTable.exists(storeIndexedNode.value())) {
                this.dispatch(storeIndexedNode.value());
            }
            String valueVar = "";
            String code = "";
            valueVar = symbolTable.lookupName(storeIndexedNode.value());

            ExtraArrayNamesManager manager = ExtraArrayNamesManager.getInstance();
            ArrayList<ArrayList<String>> outputList = manager.getArrayOutputList();

            boolean doNotGenerateOutput = false;
            if (!outputList.isEmpty()) {
                for (ArrayList<String> s : outputList) {
                    if (s.get(ArrayInfo.NAME.getIdx()).equals(arrayName)) {
                        doNotGenerateOutput = true;
                    }
                }
            }

            if (!doNotGenerateOutput) {
                if (arrayName != null) {
                    // code = arrayName + "[" + arrayName + "_index_data[" + arrayName + "_dim_" +
                    // dim + " + 1] + " + indexVar + "] = " + valueVar + "; ";
                    code = arrayName + "[" + indexVar + "] = " + valueVar + "; ";
                } else {
                    arrayName = symbolTable.lookupName(storeIndexedNode.array());
                    code = arrayName + "[" + indexVar + "] = " + valueVar + "; // Store 1D ";
                }
            }

            // XXX :WARNING: The access is for an array (It should be 2D -> lambda(1D) -> 2D)
            if (storeIndexedNode.value() instanceof InvokeNode) {
                if (valueVar.startsWith("lambda$")) {
                    InvokeNode invokeNode = (InvokeNode) storeIndexedNode.value();
                    String arg = symbolTable.lookupName(invokeNode.callTarget().arguments().last());
                    code = "__global short *aux; ";
                    buffer.emitCode(code);
                    code = "int aux_index[2];";
                    buffer.emitCode(code);
                    String outputVarIndex = this.outputVariableNames.get(1);
                    String totalElements = "int total = " + outputVarIndex + "[" + outputVarIndex + "[0] + 1];";
                    buffer.emitCode(totalElements);

                    String varOutput = this.outputVariableNames.get(0);
                    code = "aux =  &" + varOutput + "[" + indexVar + " * total];";
                    buffer.emitCode(code);
                    code = "aux_index[0] = total;";
                    buffer.emitCode(code);
                    code = "aux_index[1] = 0;";
                    buffer.emitCode(code);
                    code = valueVar + "(" + getExtraParametersForLambdaCall() + arg + ",aux,aux_index);";
                }
            }
            buffer.emitCode(code);

            for (Node succ : storeIndexedNode.cfgSuccessors()) {
                this.dispatch(succ);
            }
        } else {
            buffer.emitComment("StoreIndexedNode() - Array not in list.");
        }
    }

    @Override
    public void visit(IsNullNode isNullNode) {

        buffer.emitComment(isNullNode.toString(Verbosity.All));

        if (!symbolTable.exists(isNullNode.getValue())) {
            this.dispatch(isNullNode.getValue());
        }

        // checks if object is null.
        if (symbolTable.lookupArrayAccessInfo(isNullNode) != null) {
            ArrayDepth arrayAccess = symbolTable.lookupArrayAccessInfo(isNullNode);
            String variable = symbolTable.lookupName(arrayAccess.getNode());
            int dim = arrayAccess.getDimensionAccessedAt();
            String conditionVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);

            String code = "";

            if (arrayAccess.getNode() != null) {
                code = "bool " + conditionVar + " = (" + variable + "_index_data[" + variable + "_dim_" + dim + "] == -1);";
            } else {
                variable = symbolTable.lookupName(isNullNode.getValue());
                // code = "bool " + conditionVar + " = (" + variable +
                // " == -1); // Other condition";
            }

            symbolTable.add(conditionVar, isNullNode, JavaKind.Boolean);
            buffer.emitCode(code);
        } else {
            buffer.emitComment("IsNullNode() - Object is not an array.");
            // TODO: proper check, although marshaling should return a NullPointerException if it
            // is null
            // String conditionVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);
            // symbolTable.add(conditionVar, isNullNode, JavaKind.Boolean);

            // XXX: This line maybe is not totally true. It is commented since commit
            // changeset: 477:75c7bff41463
            // All the unittest in this moment are ok. It does not mean is needed for the future.

            // buffer.emitString("bool " + conditionVar + " = " + "false;");
        }

        for (Node successor : isNullNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(BoxNode boxNode) {
        buffer.emitComment("visited BoxNode");
        buffer.emitComment(boxNode.toString(Verbosity.All));

        ValueNode valueNode = boxNode.getValue();
        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);

        if (kind == JavaKind.Object) {
            kind = valueNode.stamp().getStackKind();
        }

        symbolTable.add(name, boxNode, kind);

        for (Node successor : boxNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(UnboxNode unboxNode) {

        buffer.emitComment("visited UnboxNode");
        buffer.emitComment(unboxNode.toString(Verbosity.All));

        ValueNode valueNode = unboxNode.getValue();
        if (!symbolTable.exists(valueNode)) {
            dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);

        if (kind == JavaKind.Object) {
            kind = valueNode.stamp().getStackKind();
        }

        symbolTable.add(name, unboxNode, kind);

        for (Node successor : unboxNode.cfgSuccessors()) {
            dispatch(successor);
        }
    }

    public void visit(FixedGuardNode fixedGuardNode) {
        buffer.emitComment("visited FixedGuardNode");
        buffer.emitComment(fixedGuardNode.toString(Verbosity.All));

        LogicNode condition = fixedGuardNode.condition();

        if (condition != null) {

            if (!symbolTable.exists(condition)) {
                this.dispatch(condition);
            }

            String nameCondition = symbolTable.lookupName(condition);
            JavaKind kind = symbolTable.lookupType(nameCondition);
            symbolTable.add(nameCondition, fixedGuardNode, kind);

            // We do not support object for now, apart from the known list of primitives and Tuples
            if ((nameCondition != null) && (kind != JavaKind.Object) && (kind != JavaKind.Illegal)) {
                String code = null;
                boolean negated = fixedGuardNode.isNegated();
                String threadResponsibleForDeopt = "deoptFlag[0] = get_global_id(0);";
                if (negated) {
                    // code = "if(" + nameCondition + ") { " + DEOPT_BUFFER_SET + " }";
                    code = "if(" + nameCondition + ") { " + threadResponsibleForDeopt + " }";
                } else {
                    // code = "if(!" + nameCondition + ") { " + DEOPT_BUFFER_SET + " }";
                    code = "if(!" + nameCondition + ") { " + threadResponsibleForDeopt + " }";
                }
                buffer.emitCode(code);
            }
        }

        for (Node successor : fixedGuardNode.successors()) {
            this.dispatch(successor);
        }
    }

    public void visit(PiNode piNode) {
        buffer.emitComment("visited PiNode");
        buffer.emitComment(piNode.toString(Verbosity.All));

        if (!symbolTable.exists(piNode.object())) {
            dispatch(piNode.object());
        }

        if (references.containsKey(piNode.object())) {
            references.put(piNode, references.get(piNode.object()));
        }

        ValueNode valueNode = piNode.object();
        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);

        if (kind == JavaKind.Object) {
            kind = valueNode.stamp().getStackKind();
        }

        symbolTable.add(name, piNode, kind);

        for (Node successor : piNode.cfgSuccessors()) {
            dispatch(successor);
        }
    }

    public void visit(CheckCastNode checkCastNode) {
        buffer.emitComment("visited CheckCastNode");
        buffer.emitComment(checkCastNode.toString(Verbosity.All));

        ValueNode valueNode = checkCastNode.object();
        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, checkCastNode, kind);

        if (references.containsKey(valueNode)) {
            references.put(checkCastNode, references.get(valueNode));
        }

        for (Node successor : checkCastNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    /**
     * If {@link DeoptimizeNode} found then just continue analysing the graph. TODO: write in the
     * deopt buffer.
     *
     * @param deoptimizeNode
     */
    public void visit(DeoptimizeNode deoptimizeNode) {
        buffer.emitComment("visited DeptoptimizeNode");
        buffer.emitComment(deoptimizeNode.toString(Verbosity.All));

        // XXX: Write into the deopt buffer

        for (Node successor : deoptimizeNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(NewInstanceNode newInstanceNode) {
        buffer.emitComment("visited NewInstanceNode");
        buffer.emitComment(newInstanceNode.toString(Verbosity.All));

        ResolvedJavaType instanceClass = newInstanceNode.instanceClass();
        String objectName = instanceClass.getName();

        String structVariableName = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        symbolTable.add(structVariableName, newInstanceNode, JavaKind.Object);

        if (objectSymbolTable.containsKey(objectName)) {
            objectName = getTupleName(tuplesOutputDataTypes);
        }

        if (objectName.equals("Ljava/lang/Float;")) {
            objectName = "float";
            symbolTable.add(structVariableName, newInstanceNode, JavaKind.Float);
        } else if (objectName.equals("Ljava/lang/Double;")) {
            objectName = "double";
            symbolTable.add(structVariableName, newInstanceNode, JavaKind.Double);
        } else if (objectName.equals("Ljava/lang/Integer;")) {
            objectName = "int";
            symbolTable.add(structVariableName, newInstanceNode, JavaKind.Int);
        } else if (objectName.equals("Ljava/lang/Short;")) {
            objectName = "short";
            symbolTable.add(structVariableName, newInstanceNode, JavaKind.Short);
        } else if (objectName.equals("Ljava/lang/Byte;")) {
            objectName = "byte";
            symbolTable.add(structVariableName, newInstanceNode, JavaKind.Byte);
        } else if (objectName.equals("Ljava/lang/Long;")) {
            objectName = "long";
            symbolTable.add(structVariableName, newInstanceNode, JavaKind.Long);
        } else if (objectName.equals("Ljava/lang/Character;")) {
            objectName = "char";
            symbolTable.add(structVariableName, newInstanceNode, JavaKind.Char);
        }

        String code = objectName + " " + structVariableName + ";";

        // We do not have to declare the struct if it is marked as hidden (because everything is
        // arrays)
        if ((structTohide != null) && (structTohide.equals(objectName))) {
            code = " // Struct to hide";
        }

        buffer.emitCode(code);

        for (Node successor : newInstanceNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(StoreFieldNode storeFieldNode) {
        buffer.emitComment("visited storeFieldNode");
        buffer.emitComment(storeFieldNode.toString(Verbosity.All));

        ValueNode objectNode = storeFieldNode.object();
        ValueNode valueNode = storeFieldNode.value();

        if (symbolTable.exists(objectNode)) {
            String objectName = symbolTable.lookupName(objectNode);
            String fieldName = storeFieldNode.field().getName();

            if (!symbolTable.exists(valueNode)) {
                this.dispatch(valueNode);
            }

            String name = symbolTable.lookupName(valueNode);
            JavaKind kind = symbolTable.lookupType(name);
            symbolTable.add(name, storeFieldNode, kind);

            String code = "";
            JavaKind kindObjectNode = symbolTable.lookupType(objectName);
            if (kindObjectNode == JavaKind.Object) {
                if (!objectName.startsWith("RContext")) {
                    code = objectName + "." + fieldName + " = " + name + ";  ";
                }
            } else {
                code = objectName + " = " + name + ";  ";
            }

            if (symbolTable.existsDynamicArrayVar(name)) {
                ArrayList<ArrayList<String>> output = ExtraArrayNamesManager.getInstance().getArrayOutputList();
                // Parse field number
                String fieldNumberStr = fieldName.substring(fieldName.length() - 1);
                int fieldNumber = Integer.parseInt(fieldNumberStr);

                HashMap<Integer, Integer> idx = ExtraArrayNamesManager.getInstance().getOutIndex();

                int index = idx.get(fieldNumber - 1);
                List<String> realOuput = output.get(index);

                String arraySource = realOuput.get(ArrayInfo.NAME.getIdx());

                Integer dim = symbolTable.lookupDynamicArrayDim(name);
                code = "for (int iii = 0; iii < " + dim + " ; iii++) { ";
                code += arraySource + "[iii] = " + name + "[iii]; }\n";
            }
            buffer.emitCode(code);
        } else {
            symbolTable.add("NULL", storeFieldNode, JavaKind.Object);
        }

        for (Node successor : storeFieldNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(OCLMathIntrinsicNode oclMathIntrinsicNode) {
        buffer.emitComment("visited oclMathIntrinsicNode");
        buffer.emitComment(oclMathIntrinsicNode.toString(Verbosity.All));

        ValueNode valueNode = oclMathIntrinsicNode.getValue();
        ValueNode valueNode2;
        OCLMathIntrinsicNode.Operation op = oclMathIntrinsicNode.operation();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        String intrinsic = null;
        if (op == OCLMathIntrinsicNode.Operation.POW2) {
            intrinsic = "pow(" + name + ", 2)";
        } else if (op == OCLMathIntrinsicNode.Operation.POW) {
            valueNode2 = oclMathIntrinsicNode.value2();
            if (!symbolTable.exists(valueNode2)) {
                this.dispatch(valueNode2);
            }
            String name2 = symbolTable.lookupName(valueNode2);
            intrinsic = op.toString() + "(" + name + "," + name2 + ")";
        } else if (op == OCLMathIntrinsicNode.Operation.HYPOT) {
            valueNode2 = oclMathIntrinsicNode.value2();
            if (!symbolTable.exists(valueNode2)) {
                this.dispatch(valueNode2);
            }
            String name2 = symbolTable.lookupName(valueNode2);
            intrinsic = op.toString() + "(" + name + "," + name2 + ")";

        } else {
            intrinsic = op.toString() + "(" + name + ")";
        }

        symbolTable.add(intrinsic, oclMathIntrinsicNode, kind);

        for (Node successor : oclMathIntrinsicNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(LeftShiftNode leftShiftNode) {
        buffer.emitComment("visited leftShiftNode");
        buffer.emitComment(leftShiftNode.toString(Verbosity.All));

        // XXX: It may be possible that here I have to write the operation in the code buffer
        ValueNode x = leftShiftNode.getX();
        ValueNode y = leftShiftNode.getY();

        if (!symbolTable.exists(x)) {
            this.dispatch(x);
        }

        if (!symbolTable.exists(y)) {
            this.dispatch(y);
        }

        String nameX = symbolTable.lookupName(x);
        JavaKind kindX = symbolTable.lookupType(nameX);
        symbolTable.add(nameX, x, kindX);

        String nameY = symbolTable.lookupName(y);
        JavaKind kindY = symbolTable.lookupType(nameY);
        symbolTable.add(nameY, y, kindY);

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        symbolTable.add(newVar, leftShiftNode, JavaKind.Int);

        String code = "int " + newVar + " = " + nameX + " << " + nameY + ";";
        buffer.emitCode(code);

        for (Node successor : leftShiftNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(ValueProxyNode valueProxyNode) {
        buffer.emitComment("visited valueProxyNode");
        buffer.emitComment(valueProxyNode.toString(Verbosity.All));

        ValueNode valueNode = valueProxyNode.value();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, valueProxyNode, kind);

        for (Node successor : valueNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(UnsignedRightShiftNode unsignedRightShiftNode) {
        buffer.emitComment("visited unisgnedRightShiftNode");
        buffer.emitComment(unsignedRightShiftNode.toString(Verbosity.All));

        ValueNode x = unsignedRightShiftNode.getX();
        ValueNode y = unsignedRightShiftNode.getY();

        if (!symbolTable.exists(x)) {
            this.dispatch(x);
        }
        if (!symbolTable.exists(y)) {
            this.dispatch(y);
        }

        String xVar = symbolTable.lookupName(x);
        String yVar = symbolTable.lookupName(y);

        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        symbolTable.add(resultVar, unsignedRightShiftNode, JavaKind.Int);

        String code = "int " + resultVar + " = " + xVar + " >> " + yVar + ";";
        buffer.emitCode(code);

        for (Node successor : unsignedRightShiftNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(SignExtendNode signExtendNode) {
        buffer.emitComment("visited signExtedNode");
        buffer.emitComment(signExtendNode.toString(Verbosity.All));

        ValueNode valueNode = signExtendNode.getValue();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, signExtendNode, kind);

        // XXX: Study what generate here for OpenCL if we need to
        for (Node successor : valueNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void visit(NarrowNode narrowNode) {
        buffer.emitComment("visited narrowNode");
        buffer.emitComment(narrowNode.toString(Verbosity.All));

        ValueNode valueNode = narrowNode.getValue();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, narrowNode, kind);

        for (Node successor : narrowNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(NewArrayNode newArrayNode) {

        buffer.emitComment("visited newArrayNode");
        buffer.emitComment(newArrayNode.toString(Verbosity.All));

        if (!symbolTable.exists(newArrayNode.length())) {
            this.dispatch(newArrayNode.length());
        }

        if (!symbolTable.exists(newArrayNode)) {

            ValueNode length = newArrayNode.length();
            ResolvedJavaType kind = newArrayNode.elementType();
            JavaKind javaType = kind.getJavaKind();
            String numElements = symbolTable.lookupName(length);
            String newVar = symbolTable.newVariable(SymbolTable.ARRAY_ELEMENT);
            symbolTable.add(newVar, newArrayNode, javaType);
            if (!javaType.toString().equals("Object")) {
                int intFormatElements = 1;
                try {
                    intFormatElements = Integer.parseInt(numElements);
                } catch (NumberFormatException e) {
                    buffer.emitCode("// Error in NewArrayNode with parse num element !!");
                }
                symbolTable.addDynamicArrayVar(newVar, javaType, intFormatElements);
                buffer.emitCode(javaType + " " + newVar + "[" + numElements + "]; ");
            } else {
                try {
                    Integer.parseInt(numElements);
                    symbolTable.addDynamicArrayVar(newVar, javaType, Integer.parseInt(numElements));
                    // Float by default
                    buffer.emitCode("float " + newVar + "[" + numElements + "]; ");
                } catch (NumberFormatException e) {
                    buffer.emitCode("// Error in NewArrayNode with parse num element");
                }
            }
        }

        for (Node successor : newArrayNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    @Override
    public void beginCodeGen(Node start, StructuredGraph sg) {
        throw new UnsupportedOperationException("Unsopported operation in Lambda class");
    }

    @Override
    public void visit(ValuePhiNode valuePhiNode) {
        buffer.emitComment("visited ValuePhiNode");
        buffer.emitComment(valuePhiNode.toString(Verbosity.All));

        NodeInputList<ValueNode> values = valuePhiNode.values();
        for (ValueNode value : values) {
            if (!symbolTable.exists(value)) {
                this.dispatch(value);
            }
        }

        if (values != null && !values.isEmpty()) {
            String lookupName = symbolTable.lookupName(values.first());
            String newVar = symbolTable.newVariable(SymbolTable.PHI_VAR);
            JavaKind kind = symbolTable.lookupType(lookupName);
            // String code = kind.getJavaName() + " " + newVar + " = " + lookupName + ";";
            String code = kind.getJavaName() + " " + newVar + ";";
            buffer.emitCode(code);
            symbolTable.add(newVar, valuePhiNode, kind);
        } else {
            String lookupName = symbolTable.lookupName(valuePhiNode);
            symbolTable.add(lookupName, valuePhiNode, valuePhiNode.getStackKind());
        }

        for (Node successor : valuePhiNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    private static String removeLastCharacter(String str) {
        String newStr = null;
        if (str.length() > 0) {
            newStr = str.substring(0, str.length() - 1);
        }
        return newStr;
    }

    public void visit(AndNode andNode) {
        buffer.emitComment("visited andNode");
        buffer.emitComment(andNode.toString(Verbosity.All));

        if (!symbolTable.exists(andNode.getX())) {
            this.dispatch(andNode.getX());
        }

        if (!symbolTable.exists(andNode.getY())) {
            this.dispatch(andNode.getY());
        }

        String xVar = symbolTable.lookupName(andNode.getX());
        String yVar = symbolTable.lookupName(andNode.getY());

        String resultVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        symbolTable.add(resultVar, andNode, JavaKind.Long);
        String type = andNode.getStackKind().getJavaName();
        buffer.emitCode(type + " " + resultVar + " = " + xVar + " & " + yVar + ";");

        for (Node succ : andNode.cfgSuccessors()) {
            this.dispatch(succ);
        }
    }

    public void visit(SqrtNode sqrtNode) {
        buffer.emitComment("visited SqrtNode");
        buffer.emitComment(sqrtNode.toString(Verbosity.All));

        ValueNode valueNode = sqrtNode.getValue();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        String intrinsic = "sqrt(" + name + ")";
        symbolTable.add(intrinsic, sqrtNode, kind);

        for (Node successor : sqrtNode.cfgSuccessors()) {
            this.dispatch(successor);
        }

    }

    public void visit(AbsNode absNode) {
        buffer.emitComment("visited SqrtNode");
        buffer.emitComment(absNode.toString(Verbosity.All));

        ValueNode valueNode = absNode.getValue();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        String intrinsic = "fabs(" + name + ")";
        symbolTable.add(intrinsic, absNode, kind);

        for (Node successor : absNode.cfgSuccessors()) {
            this.dispatch(successor);
        }

    }

    public void visit(ForeignCallNode foreignCallNode) {
        buffer.emitComment("visited foreignCallNode");
        buffer.emitComment(foreignCallNode.toString(Verbosity.All));

        ForeignCallDescriptor descriptor = foreignCallNode.getDescriptor();
        JavaKind stackKind = foreignCallNode.getStackKind();
        Class<?>[] argumentTypes = foreignCallNode.getDescriptor().getArgumentTypes();
        String name = descriptor.getName();
        String argumentType = argumentTypes[0].toString();

        NodeInputList<ValueNode> arguments = foreignCallNode.getArguments();
        if (!symbolTable.exists(arguments.first())) {
            this.dispatch(arguments.first());
        }

        if (name.equals("arithmeticExp")) {
            String intrinsic = symbolTable.lookupName(arguments.first());
            if (argumentType.equals("double") || argumentType.equals("float") || argumentType.equals("int")) {
                name = "exp(" + intrinsic + ") ";
            } else {
                name = "exp(" + argumentType + ")";
            }
        } else if (name.equals("arithmeticCos")) {
            String intrinsic = symbolTable.lookupName(arguments.first());
            if (argumentType.equals("double") || argumentType.equals("float") || argumentType.equals("int")) {
                name = "cos(" + intrinsic + ") ";
            } else {
                name = "cos(" + argumentType + ")";
            }
        } else if (name.equals("arithmeticSin")) {
            String intrinsic = symbolTable.lookupName(arguments.first());
            if (argumentType.equals("double") || argumentType.equals("float") || argumentType.equals("int")) {
                name = "sin(" + intrinsic + ") ";
            } else {
                name = "sin(" + argumentType + ")";
            }
        } else {
            System.err.println("# Operation not supported: " + name);
        }

        symbolTable.add(name, foreignCallNode, stackKind);

        for (Node successor : foreignCallNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(NegateNode negateNode) {
        buffer.emitComment("visited negateNode");
        buffer.emitComment(negateNode.toString(Verbosity.All));

        ValueNode valueNode = negateNode.getValue();

        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(newVar, negateNode, kind);
        buffer.emitCode(negateNode.getStackKind().getJavaName() + " " + newVar + " = -" + name + "; /// NEGATION");

        for (Node successor : negateNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(InstanceOfNode node) {

        // XXX: this method is not well implemented. We pass the information to the next dependency
        // and ignore what instanceof does only for GPU.
        // This is, in fact, not really supported. It was initial implementation for the R and Ruby
        // front-ends because of the PE.

        buffer.emitComment("visited InstanceOfNode");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode valueNode = node.getValue();
        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(newVar, node, kind);

        if (kind == JavaKind.Object) {
            kind = node.getStackKind();
        }

        if (kind != JavaKind.Void) {
            buffer.emitCode(kind.getJavaName() + " " + newVar + " = " + name + "; // InstanceNode");
        }

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(ReinterpretNode node) {
        // XXX: this method is not well implemented. We pass the information to the next dependency
        buffer.emitComment("visited ReinterpretNode");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode valueNode = node.getValue();
        if (!symbolTable.exists(valueNode)) {
            this.dispatch(valueNode);
        }

        // FIXME: not sure about to pass the condition or not.
        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, node, kind);

        // String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        // String name = symbolTable.lookupName(valueNode);
        // JavaKind kind = symbolTable.lookupType(name);
        // symbolTable.add(newVar, node, kind);
        // buffer.emitCode(kind.getJavaName() + " " + newVar + " = ( " + kind.getJavaName() + ") " +
        // name +
        // "; /// REINTERPRETERNODE:?????");

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }

    }

    private void handleVirtualArrayNode(VirtualObjectNode virtual, CommitAllocationNode node, Object[] object) {

        VirtualArrayNode arrayNode = (VirtualArrayNode) virtual;
        Constant value = ((ConstantNode) arrayNode.length()).getValue();
        int parseInt = Integer.parseInt(value.toValueString());

        // Tuple 2 contains 2 elements: <x, y>
        if (parseInt >= 2) {
            // Dispatch the values of the Tuple (they have to be in the symbol table)
            for (int i = 0; i < object.length; i++) {
                if (!symbolTable.exists((Node) object[i])) {
                    this.dispatch((Node) object[i]);
                }
            }

            // Here, add in the symbol table the tuple
            String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);

            // Infer data type
            StringBuffer tupleType = new StringBuffer("Tuple");
            for (int i = 0; i < object.length; i++) {
                if (object[i] instanceof ValueNode) {
                    String name = symbolTable.lookupName((Node) object[i]);
                    JavaKind kind = symbolTable.lookupType(name);
                    tupleType.append("_" + kind.toString());
                }
            }

            // OpenCL Code generation
            buffer.emitCode(tupleType.toString() + " " + newVar + "; // COMMIT AllocationNode");
            for (int i = 0; i < object.length; i++) {
                if (object[i] instanceof ValueNode) {
                    String name = symbolTable.lookupName((Node) object[i]);
                    buffer.emitCode(newVar + "._" + (i + 1) + "  = " + name + ";");
                }
            }

            // Add to object Tuples table
            symbolTable.add(newVar, node, JavaKind.Object);
        }
    }

    public void visit(CommitAllocationNode node) {
        buffer.emitComment("visited CommitAllocationNode");
        buffer.emitComment(node.toString(Verbosity.All));

        for (ValueNode value : node.getVirtualObjects()) {
            if (!symbolTable.exists(value)) {
                this.dispatch(value);
            }
        }

        HashMap<VirtualObjectNode, Object[]> valuesArrays = node.getValuesArrays();

        for (VirtualObjectNode virtual : valuesArrays.keySet()) {
            // Handle VirtualArrayNodes
            if (virtual instanceof VirtualArrayNode) {
                handleVirtualArrayNode(virtual, node, valuesArrays.get(virtual));
            }
        }

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(AllocatedObjectNode node) {
        buffer.emitComment("visited AllocatedObjectNode");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getCommit())) {
            dispatch(node.getCommit());
        }

        String name = symbolTable.lookupName(node.getCommit());
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, node, kind);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }

    }

    public void visit(NullCheckNode node) {
        buffer.emitComment("visited NullCheckNode");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getObject())) {
            dispatch(node.getObject());
        }

        String name = symbolTable.lookupName(node.getObject());
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, node, kind);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(FloatingReadNode node) {
        buffer.emitComment("visited FloatingReadNode");
        buffer.emitComment(node.toString(Verbosity.All));

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    // Ignore compression for OpenCL
    public void visit(CompressionNode node) {
        buffer.emitComment("visited CompressionNode");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getValue())) {
            dispatch(node.getValue());
        }

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    // Ignore node in OpenCL
    public void visit(ReadNode node) {
        buffer.emitComment("visited ReadNode ");
        buffer.emitComment(node.toString(Verbosity.All));
    }

    // Ignore node in OpenCL
    public void visit(DynamicDeoptimizeNode node) {
        buffer.emitComment("visited DynamicDeoptimizeNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        // TODO: get action and reason -

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(ObjectEqualsNode node) {
        buffer.emitComment("visited ObjectEqualsNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getX())) {
            dispatch(node.getX());
        }
        if (!symbolTable.exists(node.getY())) {
            dispatch(node.getY());
        }

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        String name = symbolTable.lookupName(node.getX());
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(newVar, node.getY(), kind);

        String newVarY = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        String nameY = symbolTable.lookupName(node.getY());
        JavaKind kindY = symbolTable.lookupType(nameY);
        symbolTable.add(newVarY, node.getX(), kindY);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(VirtualInstanceNode node) {
        buffer.emitComment("visited VirtualInstanceNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(VirtualArrayNode node) {
        buffer.emitComment("visited VirtualArrayNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode n = node.length();
        if (!symbolTable.exists(n)) {
            this.dispatch(n);
        }

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    private enum MathOpenCLOperation {
        LOG("log"),
        LOG10("log10"),
        COS("cos"),
        SIN("sin"),
        TAN("tan");

        MathOpenCLOperation(String openclOperation) {
            this.openCLOperation = openclOperation;
        }

        private String openCLOperation;

        public String getOpenCLMathOperation() {
            return openCLOperation;
        }
    }

    public void visit(AMD64MathIntrinsicNode node) {
        buffer.emitComment("visited AMD64MathIntrinsicNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getValue())) {
            this.dispatch(node.getValue());
        }

        StringBuffer code = new StringBuffer();
        Operation operation = node.operation();

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        String name = symbolTable.lookupName(node.getValue());
        JavaKind kind = symbolTable.lookupType(name);

        code.append(kind.toString() + " " + newVar + " = ");

        MathOpenCLOperation op = null;

        switch (operation) {
            case LOG:
                op = MathOpenCLOperation.LOG;
                break;
            case LOG10:
                op = MathOpenCLOperation.LOG10;
                break;
            case COS:
                op = MathOpenCLOperation.COS;
                break;
            case SIN:
                op = MathOpenCLOperation.SIN;
                break;
            case TAN:
                op = MathOpenCLOperation.TAN;
                break;
        }

        code.append(op.getOpenCLMathOperation() + "(" + name + ");");
        buffer.emitCode(code.toString());
        symbolTable.add(newVar, node, kind);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(ZeroExtendNode node) {
        buffer.emitComment("visited ZeroExtendNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node.getValue())) {
            this.dispatch(node.getValue());
        }

        String newVar = symbolTable.newVariable(SymbolTable.CAST_VAR);
        String name = symbolTable.lookupName(node.getValue());
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(newVar, node, kind);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(XorNode node) {
        buffer.emitComment("visited XorNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode x = node.getX();
        ValueNode y = node.getY();

        if (!symbolTable.exists(x)) {
            this.dispatch(x);
        }

        if (!symbolTable.exists(y)) {
            this.dispatch(y);
        }

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        String nameX = symbolTable.lookupName(node.getX());
        String nameY = symbolTable.lookupName(node.getY());

        String code = "int " + newVar + " = " + nameX + " ^^ " + nameY + " ; // XorNode";

        buffer.emitCode(code);
        symbolTable.add(newVar, node, JavaKind.Int);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(OrNode node) {
        buffer.emitComment("visited OrNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode x = node.getX();
        ValueNode y = node.getY();

        if (!symbolTable.exists(x)) {
            this.dispatch(x);
        }

        if (!symbolTable.exists(y)) {
            this.dispatch(y);
        }

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        String nameX = symbolTable.lookupName(node.getX());
        String nameY = symbolTable.lookupName(node.getY());

        String code = "int " + newVar + " = " + nameX + " | " + nameY + " ; // OrNode";

        buffer.emitCode(code);
        symbolTable.add(newVar, node, JavaKind.Int);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(RightShiftNode node) {
        buffer.emitComment("visited RightShiftNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode varX = node.getX();
        ValueNode varY = node.getY();

        if (!symbolTable.exists(varX)) {
            this.dispatch(varX);
        }

        if (!symbolTable.exists(varY)) {
            this.dispatch(varY);
        }

        String nameX = symbolTable.lookupName(varX);
        JavaKind kindX = symbolTable.lookupType(nameX);
        symbolTable.add(nameX, varX, kindX);

        String nameY = symbolTable.lookupName(varY);
        JavaKind kindY = symbolTable.lookupType(nameY);
        symbolTable.add(nameY, varY, kindY);

        String newVar = symbolTable.newVariable(SymbolTable.RESULT_VAR);
        symbolTable.add(newVar, node, JavaKind.Int);

        String code = "int " + newVar + " = " + nameX + " >> " + nameY + ";";
        buffer.emitCode(code);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(UnsafeLoadNode node) {
        buffer.emitComment("visited UnsafeLoadNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        LogicNode guardingCondition = node.getGuardingCondition();

        if (!symbolTable.exists(guardingCondition)) {
            this.dispatch(guardingCondition);
        }

        if (!symbolTable.exists(node.object())) {
            this.dispatch(node.object());
        }

        if (!symbolTable.exists(node.offset())) {
            this.dispatch(node.offset());
        }

        String name = symbolTable.lookupName(node.object());
        JavaKind kind = symbolTable.lookupType(name);
        symbolTable.add(name, node, kind);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(IntegerTestNode node) {
        buffer.emitComment("visited IntegerTestNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode x = node.getX();
        ValueNode y = node.getY();

        if (!symbolTable.exists(x)) {
            this.dispatch(x);
        }
        if (!symbolTable.exists(y)) {
            this.dispatch(y);
        }

        String newVar = symbolTable.newVariable(SymbolTable.CONDITION_VAR);
        // String nameX = symbolTable.lookupName(x);
        String nameY = symbolTable.lookupName(y);

        // String code = "bool " + newVar + " = ((" + nameX + " & " + nameY +
        // ") == 0)? true: false;";
        String code = "bool " + newVar + " = " + nameY + ";";
        buffer.emitCode(code);
        symbolTable.add(newVar, node, JavaKind.Boolean);

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(ArrayCopyNode node) {
        buffer.emitComment("visited ArrayCopyNode ");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode source = node.getSource();
        ValueNode sourcePosition = node.getSourcePosition();
        ValueNode destination = node.getDestination();
        ValueNode destinationPosition = node.getDestinationPosition();
        ValueNode length = node.getLength();

        if (!symbolTable.exists(source)) {
            this.dispatch(source);
        }

        if (!symbolTable.exists(sourcePosition)) {
            this.dispatch(sourcePosition);
        }

        if (!symbolTable.exists(destination)) {
            this.dispatch(destination);
        }

        if (!symbolTable.exists(destinationPosition)) {
            this.dispatch(destinationPosition);
        }

        if (!symbolTable.exists(length)) {
            this.dispatch(length);
        }

        String init = symbolTable.lookupName(sourcePosition);
        String size = symbolTable.lookupName(length);

        String sourceS = symbolTable.lookupName(source);
        String destinations = symbolTable.lookupName(destination);
        String destinationPositionS = symbolTable.lookupName(destinationPosition);

        StringBuffer code = new StringBuffer();
        code.append("for (int i = " + init + "; i < " + size + "; i++) {");
        code.append(destinations + "[i] = " + sourceS + "[ i + " + destinationPositionS + "];}\n");

        buffer.emitCode(code.toString());

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    /**
     * Experimental support for this node: It is basically a PiNode with the length of the array.
     *
     * @param piArrayNode
     */
    public void visit(PiArrayNode piArrayNode) {
        buffer.emitComment("visited piArrayNode ");
        buffer.emitComment(piArrayNode.toString(Verbosity.All));

        if (!symbolTable.exists(piArrayNode.object())) {
            dispatch(piArrayNode.object());
        }

        if (references.containsKey(piArrayNode.object())) {
            references.put(piArrayNode, references.get(piArrayNode.object()));
        }

        ValueNode valueNode = piArrayNode.object();
        String name = symbolTable.lookupName(valueNode);
        JavaKind kind = symbolTable.lookupType(name);

        if (kind == JavaKind.Object) {
            kind = valueNode.stamp().getStackKind();
        }

        symbolTable.add(name, piArrayNode, kind);

        for (Node successor : piArrayNode.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    // =================================================================
    // Set of nodes not specially needed for now. This is just to check
    // as a consequence of the ruby instruction rand()
    // =================================================================

    public void visit(CurrentJavaThreadNode node) {
        buffer.emitComment("visited CurrentJavaThreadNode");
        buffer.emitComment(node.toString(Verbosity.All));

        if (!symbolTable.exists(node)) {
            String newVariable = symbolTable.newVariable(SymbolTable.THREAD_VALUE);
            symbolTable.add(newVariable, node, node.getStackKind());
        }

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(OffsetAddressNode node) {
        buffer.emitComment("visited OffsetAddressNode");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode base = node.getBase();

        if (!symbolTable.exists(base)) {
            dispatch(base);
        }

        symbolTable.add("address", node, node.getStackKind());

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(JavaReadNode node) {
        buffer.emitComment("visited JavaReadNode");
        buffer.emitComment(node.toString(Verbosity.All));

        AddressNode address = node.getAddress();

        if (!symbolTable.exists(address)) {
            dispatch(address);
        }

        for (Node successor : node.cfgSuccessors()) {
            this.dispatch(successor);
        }
    }

    public void visit(IntegerSwitchNode node) {
        buffer.emitComment("visited JavaReadNode");
        buffer.emitComment(node.toString(Verbosity.All));

        ValueNode value = node.value();
        if (!symbolTable.exists(value)) {
            this.dispatch(value);
        }

        String name = symbolTable.lookupName(value);

        int keyCount = node.keyCount();

        Iterator<? extends Node> iterator = node.cfgSuccessors().iterator();
        for (int i = 0; i < keyCount; i++) {
            JavaConstant keyAt = node.keyAt(i);
            String code = i == 0 ? "if (" + name + " == " + keyAt.asInt() + " ) {" : "else if (" + name + " == " + keyAt.asInt() + " ) {";
            buffer.emitCode(code);
            buffer.beginBlock();
            this.dispatch(iterator.next());
            buffer.endBlock();
            buffer.emitCode("}");
        }

        if (iterator.hasNext()) {
            // this is the default case
            buffer.emitCode("else {");
            buffer.beginBlock();
            this.dispatch(iterator.next());
            buffer.endBlock();
            buffer.emitCode("}");
        }
    }

    // =================================================================

    @Override
    public boolean isSimpleDataTypeLambdaParameter() {
        return this.simpleDataTypeLambdaParameter;
    }

    @Override
    public void setSimpleDataTypeLambdaParameter(boolean c) {
        this.simpleDataTypeLambdaParameter = c;
    }

    public void setScopedNodes(ArrayList<Node> scopedNodes) {
        if (scopedNodes == null) {
            this.scopedNodes = new ArrayList<>();
        } else {
            this.scopedNodes = scopedNodes;
        }
    }

    public void setInputArgs(int inputArgs) {
        this.inputArgs = inputArgs;
    }

    @Override
    public void visit(Node node) {
        String clazzName = node.getClass().getName();
        String errorMessage = "#ERROR: Visit method for class " + clazzName + " has not been implemented.";
        buffer.emitComment(errorMessage);
        buffer.emitCode("// " + errorMessage);
        System.err.println(errorMessage);
    }
}
