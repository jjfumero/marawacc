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

package uk.ac.ed.compiler.utils;

import static jdk.vm.ci.code.CodeUtil.getCallingConvention;

import java.lang.reflect.Method;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.CompilationResult;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCI;

import com.oracle.graal.api.runtime.GraalJVMCICompiler;
import com.oracle.graal.compiler.GraalCompiler;
import com.oracle.graal.compiler.target.Backend;
import com.oracle.graal.lir.asm.CompilationResultBuilderFactory;
import com.oracle.graal.lir.phases.LIRSuites;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.StructuredGraph.AllowAssumptions;
import com.oracle.graal.phases.OptimisticOptimizations;
import com.oracle.graal.phases.PhaseSuite;
import com.oracle.graal.phases.tiers.HighTierContext;
import com.oracle.graal.phases.tiers.Suites;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.runtime.RuntimeProvider;

/**
 * Just In Time method compilation. Class to be called when server compiler is running. The
 * compilation is done via GRAAL.
 *
 */
public final class JITGraalCompilerUtil implements CompilationUtils {

    // Keep the cache per JITMethodCompiler object
    private final CodeCacheProvider codeCache;

    private ResolvedJavaMethod resolvedJavaMethod;

    public JITGraalCompilerUtil() {
        codeCache = GraalOCLBackendConnector.getProviders().getCodeCache();
    }

    public ResolvedJavaMethod getCompiledLambda(Class<?> classFunction, String nameMethod) {
        Method applyMethod = getMethodFromName(classFunction, nameMethod);
        resolvedJavaMethod = getResolvedApplyJavaMethod(applyMethod);
        return resolvedJavaMethod;
    }

    public static Backend getBackend() {
        return GraalOCLBackendConnector.getHostBackend();
    }

    public static PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite() {
        return getBackend().getSuites().getDefaultGraphBuilderSuite().copy();
    }

    public static CodeCacheProvider getCodeCache() {
        return GraalOCLBackendConnector.getProviders().getCodeCache();
    }

    public static Providers getProviders() {
        return GraalOCLBackendConnector.getProviders();
    }

    public static Suites createSuites(Backend backend) {
        Suites suite = backend.getSuites().createSuites();
        return suite;
    }

    /**
     * Compile method and install the machine code in the hotspot.
     *
     * @param method
     * @return {@link InstalledCode}
     */
    public InstalledCode compile(ResolvedJavaMethod method) {

        StructuredGraph graphToCompile = new StructuredGraph(method, AllowAssumptions.YES, null);

        GraalJVMCICompiler c = (GraalJVMCICompiler) JVMCI.getRuntime().getCompiler();
        RuntimeProvider runtimeProvider = c.getGraalRuntime().getCapability(RuntimeProvider.class);

        CallingConvention cc = getCallingConvention(codeCache, Type.JavaCallee, graphToCompile.method(), false);
        PhaseSuite<HighTierContext> copy = runtimeProvider.getHostBackend().getSuites().getDefaultGraphBuilderSuite().copy();

        LIRSuites createLIRSuites = runtimeProvider.getHostBackend().getSuites().getDefaultLIRSuites();

        ProfilingInfo profilingInfo = graphToCompile.method().getProfilingInfo();
        CompilationResult compilationResult = new CompilationResult();
        GraalCompiler.compileGraph(graphToCompile, cc, graphToCompile.method(), runtimeProvider.getHostBackend().getProviders(), runtimeProvider.getHostBackend(), copy, OptimisticOptimizations.ALL,
                        profilingInfo, runtimeProvider.getHostBackend().getSuites().getDefaultSuites(), createLIRSuites, compilationResult, CompilationResultBuilderFactory.Default);

        InstalledCode installedCode = codeCache.addCode(graphToCompile.method(), compilationResult, null, null);

        return installedCode;
    }
}
