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

import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.BeginNode;
import com.oracle.graal.nodes.ConstantNode;
import com.oracle.graal.nodes.EndNode;
import com.oracle.graal.nodes.IfNode;
import com.oracle.graal.nodes.InvokeWithExceptionNode;
import com.oracle.graal.nodes.KillingBeginNode;
import com.oracle.graal.nodes.LogicConstantNode;
import com.oracle.graal.nodes.LoopBeginNode;
import com.oracle.graal.nodes.LoopEndNode;
import com.oracle.graal.nodes.LoopExitNode;
import com.oracle.graal.nodes.MergeNode;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.ReturnNode;
import com.oracle.graal.nodes.StartNode;
import com.oracle.graal.nodes.ValuePhiNode;
import com.oracle.graal.nodes.calc.AddNode;
import com.oracle.graal.nodes.calc.ConditionalNode;
import com.oracle.graal.nodes.calc.DivNode;
import com.oracle.graal.nodes.calc.FloatConvertNode;
import com.oracle.graal.nodes.calc.IntegerBelowNode;
import com.oracle.graal.nodes.calc.IntegerDivNode;
import com.oracle.graal.nodes.calc.IntegerEqualsNode;
import com.oracle.graal.nodes.calc.IntegerLessThanNode;
import com.oracle.graal.nodes.calc.IntegerRemNode;
import com.oracle.graal.nodes.calc.IsNullNode;
import com.oracle.graal.nodes.calc.MulNode;
import com.oracle.graal.nodes.calc.NarrowNode;
import com.oracle.graal.nodes.calc.NormalizeCompareNode;
import com.oracle.graal.nodes.calc.SignExtendNode;
import com.oracle.graal.nodes.calc.SubNode;
import com.oracle.graal.nodes.calc.UnsignedRightShiftNode;
import com.oracle.graal.nodes.java.ArrayLengthNode;
import com.oracle.graal.nodes.java.LoadFieldNode;
import com.oracle.graal.nodes.java.LoadIndexedNode;
import com.oracle.graal.nodes.java.StoreIndexedNode;

public interface NodeVisitor {

    void visit(ArrayLengthNode arrayLengthNode);

    void visit(BeginNode beginNode);

    void visit(ConditionalNode conditionalNode);

    void visit(ConstantNode constantNode);

    void visit(FloatConvertNode convertNode);

    void visit(EndNode endNode);

    void visit(AddNode floatAddNode);

    void visit(DivNode floatDivNode);

    void visit(MulNode floatMulNode);

    void visit(SubNode floatSubNode);

    void visit(IfNode ifNode);

    void visit(IntegerBelowNode integerBelowThanNode);

    void visit(IntegerDivNode integerDivNode);

    void visit(IntegerEqualsNode integerEqualsNode);

    void visit(IntegerLessThanNode integerLessThanNode);

    void visit(IntegerRemNode integerRemNode);

    void visit(IsNullNode isNullNode);

    void visit(InvokeWithExceptionNode invokeWithExceptionNode);

    void visit(KillingBeginNode killingBeginNode);

    void visit(LoadFieldNode loadFieldNode);

    void visit(LoadIndexedNode loadIndexedNode);

    void visit(ParameterNode localNode);

    void visit(LogicConstantNode logicConstantNode);

    void visit(LoopBeginNode loopBeginNode);

    void visit(LoopEndNode loopEndNode);

    void visit(LoopExitNode loopExitNode);

    void visit(MergeNode mergeNode);

    void visit(NormalizeCompareNode normalizeCompareNode);

    void visit(ReturnNode returnNode);

    void visit(StartNode startNode);

    void visit(StoreIndexedNode storeIndexedNode);

    void visit(SignExtendNode signExtendNode);

    void visit(ValuePhiNode valuePhiNode);

    void visit(NarrowNode narrowNode);

    void visit(UnsignedRightShiftNode unsignedRightShiftNode);

    void visit(Node n);

    void dispatch(Node n);
}
