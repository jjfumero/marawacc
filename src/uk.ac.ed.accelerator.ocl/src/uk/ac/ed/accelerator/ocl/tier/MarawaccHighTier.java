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
package uk.ac.ed.accelerator.ocl.tier;

import com.oracle.graal.compiler.target.Backend;
import com.oracle.graal.graph.Node;
import com.oracle.graal.hotspot.meta.HotSpotProviders;
import com.oracle.graal.loop.DefaultLoopPolicies;
import com.oracle.graal.loop.phases.LoopFullUnrollPhase;
import com.oracle.graal.nodes.InvokeNode;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.phases.OptimisticOptimizations;
import com.oracle.graal.phases.Phase;
import com.oracle.graal.phases.PhaseSuite;
import com.oracle.graal.phases.common.CanonicalizerPhase;
import com.oracle.graal.phases.common.ConditionalEliminationPhase;
import com.oracle.graal.phases.common.DeadCodeEliminationPhase;
import com.oracle.graal.phases.common.inlining.InliningPhase;
import com.oracle.graal.phases.tiers.HighTierContext;
import com.oracle.graal.phases.util.Providers;

import jdk.vm.ci.meta.MetaAccessProvider;
import uk.ac.ed.accelerator.math.ocl.OCLMath;
import uk.ac.ed.compiler.utils.GraalOCLBackendConnector;
import uk.ac.ed.replacements.ocl.OCLHotSpotReplacementsImpl;
import uk.ac.ed.replacements.ocl.OCLMathSubstitutions;

public class MarawaccHighTier {

    public static class MarawaccGPUCompilerPhase extends Phase {

        private HighTierContext context;

        public MarawaccGPUCompilerPhase(HighTierContext context) {
            this.context = context;
        }

        @Override
        protected void run(StructuredGraph graph) {
            new CanonicalizerPhase().apply(graph, context);
            new InliningPhase(new CanonicalizerPhase()).apply(graph, context);
            new CanonicalizerPhase().apply(graph, context);
            new DeadCodeEliminationPhase().apply(graph);
            new ConditionalEliminationPhase().apply(graph);
            new LoopFullUnrollPhase(new CanonicalizerPhase(), new DefaultLoopPolicies()).apply(graph, context);
            // new PartialEscapePhase(true, new CanonicalizerPhase()).apply(graph, context);
        }

    }

    public static Backend getBackend() {
        return GraalOCLBackendConnector.getHostBackend();
    }

    public static MetaAccessProvider getMetaAccess() {
        return GraalOCLBackendConnector.getHostBackend().getProviders().getMetaAccess();
    }

    protected static PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite(Backend backend) {
        return backend.getSuites().getDefaultGraphBuilderSuite().copy();
    }

    public static HighTierContext applyMathReplacements(boolean mathReplacements, StructuredGraph graph) {

        Backend backend = getBackend();
        Providers providers = backend.getProviders();
        HotSpotProviders hotspotProviders = (HotSpotProviders) backend.getProviders();
        PhaseSuite<HighTierContext> graphBuilderSuite = getDefaultGraphBuilderSuite(backend);
        HighTierContext context = null;

        if (!mathReplacements) {
            context = new HighTierContext(providers, graphBuilderSuite, OptimisticOptimizations.ALL);
        } else {
            OCLHotSpotReplacementsImpl replacements = new OCLHotSpotReplacementsImpl(providers, hotspotProviders.getSnippetReflection(), providers.getCodeCache().getTarget());
            replacements.setGraphBuilderPlugins(hotspotProviders.getGraphBuilderPlugins());
            replacements.registerSubstitutions(OCLMath.class, OCLMathSubstitutions.class);
            Providers providersWithMathReplacements = providers.copyWith(replacements);
            context = new HighTierContext(providersWithMathReplacements, graphBuilderSuite, OptimisticOptimizations.ALL);
        }

        for (Node node : graph.getNodes()) {
            if (node instanceof InvokeNode) {
                InvokeNode invokeNode = (InvokeNode) node;
                if (invokeNode.callTarget().targetName().startsWith("AF")) {
                    invokeNode.setUseForInlining(false);
                }
            }
        }
        return context;
    }

}
