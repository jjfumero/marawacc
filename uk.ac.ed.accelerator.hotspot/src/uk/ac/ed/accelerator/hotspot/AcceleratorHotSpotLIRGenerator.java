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
package uk.ac.ed.accelerator.hotspot;

import jdk.vm.ci.meta.LIRKind;
import jdk.vm.ci.meta.Value;

import com.oracle.graal.compiler.amd64.AMD64ArithmeticLIRGenerator;
import com.oracle.graal.lir.Variable;

public class AcceleratorHotSpotLIRGenerator extends AMD64ArithmeticLIRGenerator {

    protected AcceleratorHotSpotLIRGenerator() {
        super();
    }

    public Value emitMathExp(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        return result;
    }

    public Value emitMathPow(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        return result;
    }

    public Value emitMathHypot(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        return result;
    }
}
