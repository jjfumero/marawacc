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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;

import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;

/**
 * Meta-data to pass to the code generator.
 */
public class KernelRequest {

    private StructuredGraph graphTemplate;
    private StructuredGraph graphLambda;
    private Class<?> klass;
    private LambdaFunctionMetadata oclmetadata;
    private AcceleratorType[] datatypeList;
    private HashMap<Direction, Object[]> parametersDirection;
    private ArrayList<ParameterNode> ioParams;
    private AcceleratorType inputDataType;
    private AcceleratorType outputDataType;
    private UUID uuid;
    private ArrayList<Node> scopeNodes;
    private Object[] paramInput;
    private AcceleratorOCLInfo typeInfoOCL;
    private boolean isTruffleCode;
    private int inputArgs;

    public KernelRequest(StructuredGraph graph, Class<?> klass, LambdaFunctionMetadata oclmetadata, HashMap<Direction, Object[]> parametersDirection, ArrayList<ParameterNode> ioParams,
                    StructuredGraph graphLambda, Object[] paramInput, AcceleratorOCLInfo typeInfoOCL, boolean isTruffleCode, UUID uuid, ArrayList<Node> scopeNodes, int inputArgs) {
        this.graphTemplate = graph;
        this.klass = klass;
        this.oclmetadata = oclmetadata;
        this.parametersDirection = parametersDirection;
        this.ioParams = ioParams;
        this.graphLambda = graphLambda;
        this.paramInput = paramInput;
        this.typeInfoOCL = typeInfoOCL;
        this.isTruffleCode = isTruffleCode;
        this.uuid = uuid;
        this.scopeNodes = scopeNodes;
        this.inputArgs = inputArgs;
    }

    public int getInputArgs() {
        return this.inputArgs;
    }

    public boolean isTruffleCode() {
        return this.isTruffleCode;
    }

    public StructuredGraph getGraph() {
        return graphTemplate;
    }

    public StructuredGraph getGraphLambda() {
        return graphLambda;
    }

    public AcceleratorOCLInfo getTypeInfoOCL() {
        return typeInfoOCL;
    }

    public void setGraphTemplate(StructuredGraph graph) {
        this.graphTemplate = graph;
    }

    public void setGraphLambda(StructuredGraph graph) {
        this.graphLambda = graph;
    }

    public Class<?> getReferenceKlass() {
        return klass;
    }

    public void setReferenceKlass(Class<?> klass) {
        this.klass = klass;
    }

    public LambdaFunctionMetadata getOclmetadata() {
        return oclmetadata;
    }

    public void setOclmetadata(LambdaFunctionMetadata oclmetadata) {
        this.oclmetadata = oclmetadata;
    }

    public AcceleratorType[] getDatatypeList() {
        return datatypeList;
    }

    public void setDatatypeList(AcceleratorType[] datatypeList) {
        this.datatypeList = datatypeList;
    }

    public HashMap<Direction, Object[]> getParametersDirection() {
        return parametersDirection;
    }

    public void setParametersDirection(HashMap<Direction, Object[]> parametersDirection) {
        this.parametersDirection = parametersDirection;
    }

    public ArrayList<ParameterNode> getIoParams() {
        return ioParams;
    }

    public void setIoParams(ArrayList<ParameterNode> ioParams) {
        this.ioParams = ioParams;
    }

    public AcceleratorType getInputDataType() {
        return inputDataType;
    }

    public void setInputDataType(AcceleratorType inputDataType) {
        this.inputDataType = inputDataType;
    }

    public AcceleratorType getOutputDataType() {
        return outputDataType;
    }

    public void setOutputDataType(AcceleratorType outputDataType) {
        this.outputDataType = outputDataType;
    }

    public Object[] getParamInput() {
        return paramInput;
    }

    public void setUUIDKernel(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUIDKernel() {
        return this.uuid;
    }

    public ArrayList<Node> getScopedNodes() {
        return scopeNodes;
    }
}
