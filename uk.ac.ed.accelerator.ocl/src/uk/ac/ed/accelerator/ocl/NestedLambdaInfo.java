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
import java.util.List;

import jdk.vm.ci.meta.ResolvedJavaType;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata.TypeOfFunction;

import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.ValueNode;

class NestedLambdaInfo {

    StructuredGraph lambdaGraph;
    ResolvedJavaType lambdaType;
    LambdaFunctionMetadata meta;
    List<ValueNode> externalArgs;
    AcceleratorOCLInfo typeInfo;

    public NestedLambdaInfo(StructuredGraph lambdaGraph, ResolvedJavaType lambdaType, AcceleratorOCLInfo typeInfo) {
        this.lambdaGraph = lambdaGraph;
        this.lambdaType = lambdaType;
        this.typeInfo = typeInfo;
        this.externalArgs = new ArrayList<>();

        String interfaceName = lambdaType.getInterfaces()[0].toJavaName(false);
        TypeOfFunction lambdaFunctionType = interfaceName.endsWith("BiFunction") ? TypeOfFunction.BIFUNCTION : TypeOfFunction.FUNCTION;
        this.meta = new LambdaFunctionMetadata(lambdaFunctionType);
    }
}
