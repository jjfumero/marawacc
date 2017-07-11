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
import java.util.Map;
import java.util.UUID;

import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.KernelOffloadException;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;

import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;

public abstract class AbstractOpenCLGenerator implements NodeVisitor {

    protected boolean isTruffleFrontEnd = false;

    public abstract boolean isExtraArrayforFunction();

    public abstract void setExtraArrayforFunction(boolean condition);

    public abstract void setLastParamLambda(ParameterNode paramNode);

    public abstract void setLambdaGraph(StructuredGraph graph);

    public abstract void setParametersDirection(Map<Direction, Object[]> parameters);

    public abstract void addInitCode(StructuredGraph graph);

    public abstract void addLocalVariables(StructuredGraph graph);

    public abstract String generateKernelSignature(StructuredGraph graph, Class<?> klass, ArrayList<ParameterNode> ioParams) throws KernelOffloadException;

    public abstract String generateParametersSignature(StructuredGraph graph, Class<?> klass, ArrayList<ParameterNode> ioParams) throws KernelOffloadException;

    public abstract void generateBodyKernel(StructuredGraph graph);

    public abstract void updateSymbolTable(StructuredGraph graph);

    public abstract void generateMainSkeletonKernel(StructuredGraph graph, Class<?> klass, LambdaFunctionMetadata oclmetadata, AcceleratorOCLInfo typeInfoOCL, ArrayList<ParameterNode> ioParams,
                    UUID uuid)
                    throws KernelOffloadException;

    public abstract void generateOpenCLForLambdaFunction(StructuredGraph graph, LambdaFunctionMetadata oclmetadata, AcceleratorOCLInfo typeInfoOCL) throws KernelOffloadException;

    public abstract String getCode();

    public abstract String getKernelName();

    public abstract void beginCodeGen(Node start, StructuredGraph sg);

    public abstract boolean isSimpleDataTypeLambdaParameter();

    public abstract void setSimpleDataTypeLambdaParameter(boolean c);

    public abstract void dispatch(Node n);

    public abstract String includeKhronosPragmas();

    /**
     * If the Graal-IR comes from Truffle Interpreter, we need more information about the scope. In
     * that case, we generate code slightly different. If it is a Truffle language, then set a this
     * variable to true.
     *
     * @param isTruffleFrontEnd
     */
    public void setTruffleFrontEnd(boolean isTruffleFrontEnd) {
        this.isTruffleFrontEnd = isTruffleFrontEnd;
    }

    public boolean isTruffleFrontEnd() {
        return this.isTruffleFrontEnd;
    }
}
