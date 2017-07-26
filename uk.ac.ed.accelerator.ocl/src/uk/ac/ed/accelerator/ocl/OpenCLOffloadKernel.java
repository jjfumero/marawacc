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
import java.util.HashMap;
import java.util.UUID;

import uk.ac.ed.accelerator.cache.OCLKernelCache;
import uk.ac.ed.accelerator.cache.OCLKernelPackage;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType.DataType;
import uk.ac.ed.accelerator.ocl.runtime.KernelOffloadException;
import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;
import uk.ac.ed.datastructures.common.PArray;

import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;

public final class OpenCLOffloadKernel {

    public static boolean isKernelInCache(UUID uuidKernel) {
        // CacheSystem for Functional GPU Programming
        if (OCLKernelCache.getInstance().isInCache(uuidKernel)) {
            if (GraalAcceleratorOptions.debugCacheGPUCode) {
                System.out.println("[Debug] Kernel found in cache: " + uuidKernel);
            }
            return true;
        }
        ExtraArrayNamesManager.getInstance().clean();
        return false;
    }

    public static synchronized void generateOpenCLKernel(KernelRequest kernelPackage) throws KernelOffloadException {

        StructuredGraph graphLambda = kernelPackage.getGraphLambda();
        UUID uuidKernel = kernelPackage.getUUIDKernel();

        boolean isGenerated = isKernelInCache(uuidKernel);
        long startTime = System.nanoTime();

        // Generate kernel only if it has not been generated yet
        if (!isGenerated) {
            // Get Parameters from Kernel package
            StructuredGraph graph = kernelPackage.getGraph();

            Class<?> klass = kernelPackage.getReferenceKlass();
            LambdaFunctionMetadata oclmetadata = kernelPackage.getOclmetadata();
            AcceleratorOCLInfo typeInfoOCL = kernelPackage.getTypeInfoOCL();
            HashMap<Direction, Object[]> parametersDirection = kernelPackage.getParametersDirection();
            ArrayList<ParameterNode> ioParams = kernelPackage.getIoParams();

            // Build runtime symbol tables for arrays - meta-information for the code generator
            SymbolPhases symbolPhases = new SymbolPhases();
            symbolPhases.applyPhases(graph);

            StringBuffer openCLCode = new StringBuffer();
            SymbolTable table = new SymbolTable(symbolPhases.getArrayAccessDim(), symbolPhases.getArrayDimensions());

            GraalOpenCLGenerator codeSkeleton = new GraalOpenCLGenerator(GraalAcceleratorOptions.debugOCLKernel, table);
            codeSkeleton.setTruffleFrontEnd(kernelPackage.isTruffleCode());
            codeSkeleton.setParametersDirection(parametersDirection);
            codeSkeleton.setLambdaGraph(graphLambda);
            codeSkeleton.setLastParamLambda(null);

            if (typeInfoOCL.getClassOutput().getType() == DataType.ARRAY) {
                codeSkeleton.setExtraArrayforFunction(true); // The function call is different -
                                                             // takes more
                                                             // arguments that is not in the CFG
            }

            // Generate the Main Kernel - Skeleton - (__kernel void mainKernel() { ... } )
            codeSkeleton.generateMainSkeletonKernel(graph, klass, oclmetadata, typeInfoOCL, ioParams, uuidKernel);

            boolean simpleLambdaParameter = codeSkeleton.isSimpleDataTypeLambdaParameter();

            // Generate the second kernel, it is the kernel for the lambda expression
            SymbolPhases symbolPhasesFunction = new SymbolPhases();
            symbolPhasesFunction.applyPhases(graphLambda);

            // Preparation
            table = new SymbolTable(symbolPhasesFunction.getArrayAccessDim(), symbolPhasesFunction.getArrayDimensions());
            GraalOpenCLGenerator codeLambda = new GraalOpenCLGenerator(GraalAcceleratorOptions.debugOCLKernel, table, uuidKernel);
            codeLambda.setParametersDirection(parametersDirection);
            codeLambda.setLambdaGraph(graphLambda);
            codeLambda.setSimpleDataTypeLambdaParameter(simpleLambdaParameter);

            codeLambda.setTruffleFrontEnd(kernelPackage.isTruffleCode());
            codeLambda.setScopedNodes(kernelPackage.getScopedNodes());
            codeLambda.setScopeTruffleList(codeSkeleton.getScopeTruffleList());
            codeLambda.setInputArgs(kernelPackage.getInputArgs());

            // Generate the Lambda Kernel - (<type> lambda$1(...) { ... } )
            codeLambda.generateOpenCLForLambdaFunction(graphLambda, oclmetadata, typeInfoOCL);

            codeSkeleton.setExtraArrayforFunction(codeLambda.isExtraArrayforFunction());

            // Compose the OpenCL Kernel
            String pragmas = codeSkeleton.includeKhronosPragmas();
            openCLCode.append(pragmas + "\n" + codeLambda.getCode());
            openCLCode.append(codeSkeleton.getCode());
            String kernelName = codeSkeleton.getKernelName();

            String kernelSourceCode = openCLCode.toString();

            // Insert the OpenCL kernel into a cache

            ArrayList<String> kernelsSource = new ArrayList<>();

            // Here it is the first point to insert kernel for multidevice
            int numDevices = 1;
            if (GraalAcceleratorOptions.multiOpenCLDevice) {
                numDevices = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();

                // Add first kernel
                kernelsSource.add(kernelSourceCode.replace("<offsetDevice>", ""));
                PArray<?> parray = (PArray<?>) parametersDirection.get(Direction.INPUT)[0];

                // Add the rest of the kernels
                for (int i = 1; i < numDevices; i++) {
                    int size = parray.getTotalSizeWhenSequence() / 2;
                    kernelsSource.add(kernelSourceCode.replace("<offsetDevice>", " + " + size));
                }
            } else {
                kernelSourceCode = kernelSourceCode.replace("<offsetDevice>", "");
                kernelsSource.add(kernelSourceCode);
            }

            long endTime = System.nanoTime();
            TimeDescriptor.getInstance().put(TimeDescriptor.Time.KERNEL_GENERATION_TIME, (endTime - startTime));
            Profiler.getInstance().put(ProfilerType.KERNEL_GENERATION_TIME, (endTime - startTime));

            for (int deviceIndex = 0; deviceIndex < numDevices; deviceIndex++) {
                OCLKernelPackage oclCache = new OCLKernelPackage();
                oclCache.setKernelCode(kernelsSource.get(deviceIndex));
                oclCache.setKernelName(kernelName);
                OCLKernelCache.getInstance().insert(uuidKernel, deviceIndex, oclCache);
                Profiler.getInstance().writeInBuffer(ProfilerType.KERNEL_GENERATION_TIME, "total", (endTime - startTime), deviceIndex);
            }

            if (GraalAcceleratorOptions.printOffloadKernel) {
                for (int i = 0; i < kernelsSource.size(); i++) {
                    System.out.println(kernelsSource.get(i));
                }
            }
        }
    }

    public static boolean wasGenerated(UUID uuidKernel) {
        return OCLKernelCache.getInstance().isInCache(uuidKernel);
    }

}
