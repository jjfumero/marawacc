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
package uk.ac.ed.jpai.jit;

import java.util.function.Function;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.ed.accelerator.ocl.runtime.GraalIRConversion;
import uk.ac.ed.compiler.utils.JITGraalCompilerUtil;

public class JPAICompileFunctionThread<inT, outT> extends Thread {

    private Function<inT, outT> function = null;
    private InstalledCode compiledCode = null;

    public JPAICompileFunctionThread(Function<inT, outT> function) {
        this.function = function;
    }

    public Function<inT, outT> getFunction() {
        return this.function;
    }

    public InstalledCode getInstalledCode() {
        return this.compiledCode;
    }

    @SuppressWarnings("unchecked")
    public outT executeCompiledCode(inT input) throws InvalidInstalledCodeException {
        return (outT) compiledCode.executeVarargs(input);
    }

    @Override
    public void run() {
        JITGraalCompilerUtil compiler = new JITGraalCompilerUtil();
        ResolvedJavaMethod resolvedJavaMethodForUserFunction = GraalIRConversion.getResolvedJavaMethodForUserFunction(function.getClass());
        this.compiledCode = compiler.compile(resolvedJavaMethodForUserFunction);
    }

}
