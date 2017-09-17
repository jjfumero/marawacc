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
package uk.ac.ed.compiler.utils.test;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.junit.Test;

import uk.ac.ed.compiler.utils.JITGraalCompilerUtil;

import com.oracle.graal.java.BytecodeDisassembler;
import com.oracle.graal.phases.util.Providers;

public class CustomCompilationTest {

    // Test method to compile.
    public static int methodToCompile() {
        int acc = 0;
        for (int i = 0; i < 100; i++) {
            acc += i;
        }
        return acc;
    }

    public static int methodToCompile2(int x) {
        int acc = x;
        for (int i = 0; i < 100; i++) {
            acc += i;
        }
        return acc;
    }

    @Test
    public void testGraalJIT01() throws InvalidInstalledCodeException {

        // This test will compile a method and it will execute the binary installed into the
        // hotspot. It will check if the result is correct compared to the Java execution.

        String nameMethod = "methodToCompile";
        Class<?> klass = CustomCompilationTest.class;

        JITGraalCompilerUtil compiler = new JITGraalCompilerUtil();

        Method method = compiler.getMethodFromName(klass, nameMethod);
        Providers providers = JITGraalCompilerUtil.getProviders();
        MetaAccessProvider metaAccess = providers.getMetaAccess();
        ResolvedJavaMethod resolvedJavaMethod = metaAccess.lookupJavaMethod(method);

        InstalledCode compile = compiler.compile(resolvedJavaMethod);

        assertEquals(nameMethod, resolvedJavaMethod.getName());

        Object executeVarargs = compile.executeVarargs();

        System.out.println(new BytecodeDisassembler().disassemble(resolvedJavaMethod));

        int result = methodToCompile();

        assertEquals(result, executeVarargs);
    }

    @Test
    public void testGraalJIT02() throws InvalidInstalledCodeException {

        // This test will compile a method and it will execute the binary installed into the
        // hotspot. It will check if the result is correct compared to the Java execution.

        String nameMethod = "methodToCompile2";
        Class<?> klass = CustomCompilationTest.class;

        JITGraalCompilerUtil compiler = new JITGraalCompilerUtil();

        Method method = compiler.getMethodFromName(klass, nameMethod);
        Providers providers = JITGraalCompilerUtil.getProviders();
        MetaAccessProvider metaAccess = providers.getMetaAccess();
        ResolvedJavaMethod resolvedJavaMethod = metaAccess.lookupJavaMethod(method);

        InstalledCode compile = compiler.compile(resolvedJavaMethod);

        assertEquals(nameMethod, resolvedJavaMethod.getName());

        Object executeVarargs = compile.executeVarargs(10);

        int result = methodToCompile2(10);
        System.out.println(new BytecodeDisassembler().disassemble(resolvedJavaMethod));

        assertEquals(result, executeVarargs);
    }
}
