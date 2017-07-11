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

package uk.ac.ed.replacements.ocl;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;
import uk.ac.ed.accelerator.hotspot.AcceleratorHotSpotLIRGenerator;
import uk.ac.ed.accelerator.math.ocl.OCLMath;

import com.oracle.graal.compiler.common.type.FloatStamp;
import com.oracle.graal.compiler.common.type.PrimitiveStamp;
import com.oracle.graal.compiler.common.type.StampFactory;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.graph.spi.CanonicalizerTool;
import com.oracle.graal.lir.gen.ArithmeticLIRGeneratorTool;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.ConstantNode;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.calc.UnaryNode;
import com.oracle.graal.nodes.spi.ArithmeticLIRLowerable;
import com.oracle.graal.nodes.spi.NodeLIRBuilderTool;

@NodeInfo
public class OCLMathIntrinsicNode extends UnaryNode implements ArithmeticLIRLowerable {

    public static final NodeClass<OCLMathIntrinsicNode> TYPE = NodeClass.create(OCLMathIntrinsicNode.class);
    protected final Operation operation;

    public enum Operation {
        FABS("fabs"),
        LOG("log"),
        SQRT("sqrt"),
        EXP("exp"),
        POW2("pow2"),
        POW("pow"),
        HYPOT("hypot");

        private Operation(String oclFunctionName) {
            this.oclFunctionName = oclFunctionName;
        }

        private final String oclFunctionName;

        @Override
        public String toString() {
            return oclFunctionName;
        }
    }

    @Input protected ValueNode param1;
    @Input protected ValueNode param2;

    public ValueNode value2() {
        return this.param2;
    }

    public Operation operation() {
        return operation;
    }

    public static ValueNode create(ValueNode value, Operation op) {
        ValueNode c = tryConstantFold(value, op);
        if (c != null) {
            return c;
        }
        return new OCLMathIntrinsicNode(value, op);
    }

    public static ValueNode create(ValueNode value, ValueNode value2, Operation op) {
        ValueNode c = tryConstantFold(value, op);
        if (c != null) {
            return c;
        }
        return new OCLMathIntrinsicNode(value, value2, op);
    }

    protected static ValueNode tryConstantFold(ValueNode value, Operation op) {
        if (value.isConstant()) {
            double ret = doCompute(value.asJavaConstant().asDouble(), op);
            return ConstantNode.forDouble(ret);
        }
        return null;
    }

    protected OCLMathIntrinsicNode(ValueNode value, Operation op) {
        super(TYPE, StampFactory.forKind(JavaKind.Float), value);
        assert value.stamp() instanceof FloatStamp && PrimitiveStamp.getBits(value.stamp()) == 64;
        this.operation = op;
    }

    protected OCLMathIntrinsicNode(ValueNode value, ValueNode value2, Operation op) {
        super(TYPE, StampFactory.forKind(JavaKind.Float), value);
        assert value.stamp() instanceof FloatStamp && PrimitiveStamp.getBits(value.stamp()) == 64;
        this.operation = op;
        this.param2 = value2;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool lirGen) {
        AcceleratorHotSpotLIRGenerator gen = (AcceleratorHotSpotLIRGenerator) lirGen;
        Value input = nodeValueMap.operand(getValue());
        Value result;
        switch (operation()) {
            case FABS:
                result = gen.emitMathAbs(input);
            case SQRT:
                result = gen.emitMathSqrt(input);
            case EXP:
                result = gen.emitMathExp(input);
            case LOG:
                result = gen.emitMathLog(input, false);
            case POW2:
                result = gen.emitMathPow(input);
            case HYPOT:
                result = gen.emitMathHypot(input);
                break;
            default:
                throw JVMCIError.shouldNotReachHere();
        }
        nodeValueMap.setResult(this, result);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        ValueNode c = tryConstantFold(forValue, operation());
        if (c != null) {
            return c;
        }
        return this;
    }

    @NodeIntrinsic
    public static native float compute(float value, @ConstantNodeParameter Operation op);

    @NodeIntrinsic
    public static native float compute(float valuex, float valuey, @ConstantNodeParameter Operation op);

    @NodeIntrinsic
    public static native double compute(double value, @ConstantNodeParameter Operation op);

    @NodeIntrinsic
    public static native double compute(double valuex, double valuey, @ConstantNodeParameter Operation op);

    private static double doCompute(double value, Operation op) {
        switch (op) {
            case LOG:
                return OCLMath.log(value);
            case FABS:
                return OCLMath.fabs(value);
            case SQRT:
                return OCLMath.sqrt(value);
            case EXP:
                return OCLMath.exp(value);
            case POW2:
                return OCLMath.pow2(value);
            default:
                throw new JVMCIError("unknown op %s", op);
        }
    }
}
