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

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.runtime.JVMCI;

import com.oracle.graal.api.runtime.GraalJVMCICompiler;
import com.oracle.graal.compiler.target.Backend;
import com.oracle.graal.graphbuilderconf.GraphBuilderConfiguration;
import com.oracle.graal.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import com.oracle.graal.graphbuilderconf.InvocationPlugins;
import com.oracle.graal.java.GraphBuilderPhase;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.phases.OptimisticOptimizations;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.runtime.RuntimeProvider;

public final class GraalOCLBackendConnector {

    private static Providers providers;
    private static MetaAccessProvider metaAccess;
    private static Backend hostBackend;
    private static RuntimeProvider runtimeProvider;

    public static RuntimeProvider getRuntimeProvider() {
        return runtimeProvider;
    }

    public static Backend getHostBackend() {
        return hostBackend;
    }

    public static Providers getProviders() {
        return providers;
    }

    public static MetaAccessProvider getMetaAccessProvider() {
        return metaAccess;
    }

    public static void apply(StructuredGraph graph) {
        Plugins plugins = new Plugins(new InvocationPlugins(metaAccess));
        ConstantReflectionProvider constantReflection = providers.getConstantReflection();
        new GraphBuilderPhase.Instance(metaAccess, providers.getStampProvider(), constantReflection, GraphBuilderConfiguration.getDefault(plugins), OptimisticOptimizations.ALL, null).apply(graph);
    }

    static {
        GraalJVMCICompiler runtimeCompiler = (GraalJVMCICompiler) JVMCI.getRuntime().getCompiler();
        runtimeProvider = runtimeCompiler.getGraalRuntime().getCapability(RuntimeProvider.class);
        hostBackend = runtimeProvider.getHostBackend();
        providers = hostBackend.getProviders();
        metaAccess = providers.getMetaAccess();
    }
}
