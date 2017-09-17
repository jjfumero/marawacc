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

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorPlatform;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.KernelOffloadException;
import uk.ac.ed.accelerator.wocl.LambdaFunctionMetadata;
import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorDevice;

import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;

/**
 * Runtime system for OpenCL and Graal execution.
 *
 */
public final class GraalOpenCLRuntime {

    private static GraalAcceleratorPlatform getPlatform() throws InterruptedException {
        OCLRuntimeUtils.waitForTheOpenCLInitialization();
        GraalAcceleratorSystem system = GraalAcceleratorSystem.getInstance();
        GraalAcceleratorPlatform platform = system.getPlatform();
        return platform;
    }

    private static OCLGraalAcceleratorDevice getDevice(int idx) throws InterruptedException {
        GraalAcceleratorPlatform platform = getPlatform();
        OCLGraalAcceleratorDevice device = (OCLGraalAcceleratorDevice) platform.getDevice(idx);
        return device;
    }

    /**
     * This operation requires OpenCL Runtime communication. It compiles the source code and install
     * it in binary format in the Marawacc cache.
     *
     * @param uuidKernel
     * @throws Exception
     */
    public static void compileOpenCLKernelAndInstallBinaryWithDriver(UUID uuidKernel) throws Exception {

        int numDev = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            numDev = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }

        for (int idx = 0; idx < numDev; idx++) {
            OCLGraalAcceleratorDevice device = getDevice(idx);
            device.createCommandQueue();
            device.createProgram(uuidKernel, idx);
            device.buildProgram(uuidKernel, idx);
            device.createKernel(uuidKernel, idx);

            // if (GraalAcceleratorOptions.profileOffload) {
            // Profiler.getInstance().writeInBuffer(ProfilerType.OCL_GRAAL_DRIVER_COMMAND_TO_CREATE_KERNEL,
            // "total", (end - start));
            // Profiler.getInstance().writeInBuffer(ProfilerType.OCL_GRAAL_DRIVER_COMPILE_KERNEL,
            // "total",
            // (endBuildProgram - startBuildProgram));
            // }
        }
    }

    private static ArrayList<ParameterNode> buildParameterListNode(StructuredGraph graphTemplate) {
        ArrayList<ParameterNode> ioParamters = null;
        int iParam = 0;
        ioParamters = new ArrayList<>();
        for (ParameterNode node : graphTemplate.getNodes(ParameterNode.TYPE)) {
            if (iParam != 0) {
                ioParamters.add(node);
            }
            iParam++;
        }
        return ioParamters;
    }

    /**
     * Generate the OpenCL kernel from the {@link StructuredGraph} for the parallel skeleton and the
     * lambda expression.
     *
     * @param graphTemplate
     * @param graphLambda
     * @param klass
     * @param oclmetadata
     * @param typeInfoOCL
     * @param uuidKernel
     * @param parameters
     * @throws KernelOffloadException
     */
    public static void generateOffloadKernel(StructuredGraph graphTemplate, StructuredGraph graphLambda, Class<?> klass, LambdaFunctionMetadata oclmetadata, AcceleratorOCLInfo typeInfoOCL,
                    UUID uuidKernel, HashMap<Direction, Object[]> parameters, boolean isTruffleCode, ArrayList<Node> scopeNodes, int inputArgs) throws KernelOffloadException {

        ArrayList<ParameterNode> ioParameters = buildParameterListNode(graphTemplate);
        KernelRequest kernelRequest = new KernelRequest(graphTemplate, klass, oclmetadata, parameters, ioParameters, graphLambda, parameters.get(Direction.INPUT), typeInfoOCL, isTruffleCode,
                        uuidKernel, scopeNodes, inputArgs);
        OpenCLOffloadKernel.generateOpenCLKernel(kernelRequest);
    }

}
