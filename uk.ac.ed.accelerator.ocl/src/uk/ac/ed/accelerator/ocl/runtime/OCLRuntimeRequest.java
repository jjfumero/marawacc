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

package uk.ac.ed.accelerator.ocl.runtime;

import java.lang.reflect.Method;
import java.util.UUID;

import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.runtime.JVMCI;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.ParallelSkeleton;
import uk.ac.ed.accelerator.ocl.ParallelOptions;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;

import com.oracle.graal.api.runtime.GraalJVMCICompiler;
import com.oracle.graal.compiler.target.Backend;
import com.oracle.graal.graphbuilderconf.GraphBuilderConfiguration;
import com.oracle.graal.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import com.oracle.graal.graphbuilderconf.InvocationPlugins;
import com.oracle.graal.java.GraphBuilderPhase;
import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.nodes.StructuredGraph.AllowAssumptions;
import com.oracle.graal.phases.OptimisticOptimizations;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.runtime.RuntimeProvider;

public class OCLRuntimeRequest {
    private int jobSize;
    private Object[] args;
    private Object[] output;
    private UUID uuidKernel;
    private UUID uuidData;
    private ParallelSkeleton type;
    private LambdaFunctionMetadata oclmetadata;
    private GPUParameters paramsGPU;
    private StructuredGraph graphKernel;
    private AcceleratorOCLInfo typeInfoOcl;

    public OCLRuntimeRequest(int jobSize, Object[] args, Object[] output, UUID uuid, UUID uuiData, AcceleratorOCLInfo typeInfoOcl, ParallelSkeleton type) {
        this.jobSize = jobSize;
        this.args = args;
        this.output = output;
        this.uuidKernel = uuid;
        this.uuidData = uuiData;
        this.typeInfoOcl = typeInfoOcl;
        this.type = type;
        this.prepareForDispatch();
    }

    public LambdaFunctionMetadata getOclmetadata() {
        return oclmetadata;
    }

    public GPUParameters getParamsGPU() {
        return paramsGPU;
    }

    private static Backend getBackend() {
        GraalJVMCICompiler c = (GraalJVMCICompiler) JVMCI.getRuntime().getCompiler();
        RuntimeProvider runtimeProvider = c.getGraalRuntime().getCapability(RuntimeProvider.class);
        Backend hostBackend = runtimeProvider.getHostBackend();
        return hostBackend;
    }

    /*
     * @param Number of arguments in the map function. It will be deprecated if Function is always
     * assumed.
     */
    private StructuredGraph setTemplateType(ParallelSkeleton type) {
        return getGraalIRForParallelSkeleton(type);
    }

    private StructuredGraph getGraalIRForParallelSkeleton(ParallelSkeleton template) {
        Method method = FunctionalPatternTemplate.getMethod(template);
        Backend backend = getBackend();
        Providers providers = backend.getProviders();
        StructuredGraph graph = new StructuredGraph(((HotSpotMetaAccessProvider) providers.getMetaAccess()).lookupJavaMethod(method), AllowAssumptions.YES, null);
        Plugins plugins = new Plugins(new InvocationPlugins(providers.getMetaAccess()));
        new GraphBuilderPhase.Instance(providers.getMetaAccess(), providers.getStampProvider(), null, GraphBuilderConfiguration.getEagerDefault(plugins), OptimisticOptimizations.ALL, null).apply(graph);
        this.graphKernel = graph;
        return graph;
    }

    private static LambdaFunctionMetadata createMetadata(ParallelSkeleton operation) {
        LambdaFunctionMetadata metadata = null;
        if (operation == ParallelSkeleton.MAP) {
            metadata = new LambdaFunctionMetadata(LambdaFunctionMetadata.TypeOfFunction.FUNCTION);
        } else if (operation == ParallelSkeleton.REDUCE) {
            metadata = new LambdaFunctionMetadata(LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION);
        }
        return metadata;
    }

    private void prepareForDispatch() {
        int convertionSoA = typeInfoOcl.getClassInput().getType().getNumAttributes();
        setTemplateType(type);

        oclmetadata = createMetadata(type);

        // LambdaGraphReplacements replacements = new LambdaGraphReplacements();
        // graphLambda = replacements.decorateGraph(graphLambda);

        paramsGPU = new GPUParameters();
        paramsGPU.putInput(args);

        // If the operation is a reduction (computed as a map) => jobsize = 1
        if (oclmetadata.getType() == LambdaFunctionMetadata.TypeOfFunction.BIFUNCTION) {
            oclmetadata.setElements(jobSize);
            jobSize = 1;
        }
        paramsGPU.setOutput(output);

        // lastParam = replacements.getLastParameterNode();
        GraalAcceleratorOptions.workSize = jobSize;
        ParallelOptions.UseSoAWithValue.setValue(convertionSoA);

    }

    public UUID getUUIDKernel() {
        return this.uuidKernel;
    }

    public UUID getUUIDData() {
        return this.uuidData;
    }

    public StructuredGraph getSGKernel() {
        return this.graphKernel;
    }
}
