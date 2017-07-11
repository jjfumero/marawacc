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

import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.common.GraalMetaAccelerator;

import com.oracle.graal.nodes.ParameterNode;
import com.oracle.graal.nodes.StructuredGraph;

public interface OCLRuntimeUtils {

    static ArrayList<ParameterNode> addIOParams(StructuredGraph graph) {
        int iParam = 0;
        ArrayList<ParameterNode> ioParams = new ArrayList<>();
        for (ParameterNode node : graph.getNodes(ParameterNode.TYPE)) {
            if (iParam != 0) {
                ioParams.add(node);
            }
            iParam++;
        }
        return ioParams;
    }

    static ArrayList<Integer[]> pipelineDataPartitionStandard(int numPartitions, int total) {
        ArrayList<Integer[]> indexPipeline = new ArrayList<>();
        int totalSize = total;
        int subSize = ((totalSize / numPartitions) == 0) ? 1 : totalSize / numPartitions;
        int rest = ((totalSize / numPartitions) == 0) ? 0 : totalSize % numPartitions;
        int from = 0;
        int to = 0;
        int tmpRest = rest;
        int accRest = 0;

        int baseID = 0;
        int virtualID = baseID * numPartitions;

        for (virtualID = 0; virtualID < (baseID + numPartitions); virtualID++) {
            from = virtualID * subSize + accRest;
            accRest = (tmpRest > 0) ? accRest + 1 : accRest;
            to = (tmpRest > 0) ? from + subSize + 1 : from + subSize;
            indexPipeline.add(new Integer[]{from, to});
            tmpRest = (tmpRest > 0) ? tmpRest - 1 : tmpRest;
        }
        return indexPipeline;
    }

    static void waitForTheOpenCLInitialization() throws InterruptedException {

        // Join the thread was launched in the initialisation process (Started by Graal HotspotVM)
        if (GraalMetaAccelerator.running) {
            GraalMetaAccelerator.graalInitThread.join();
            GraalMetaAccelerator.running = false;
        }

        while (!GraalAcceleratorSystem.getInstance().isSystemInitialized()) {
            // wait
        }
        while (GraalAcceleratorSystem.getInstance().getPlatform() == null) {
            // wait
        }
        while (!GraalAcceleratorSystem.getInstance().getPlatform().isDevicesDiscovered()) {
            // wait
        }
    }

}
