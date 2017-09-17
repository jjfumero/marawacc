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

package uk.ac.ed.jpai;

import java.util.function.Function;

import uk.ac.ed.datastructures.common.PArray;

public class MapAccelerator<inT, outT> extends MapJavaThreads<inT, outT> {

    protected ArrayFunction<inT, outT> decomposition;

    /**
     *
     * @param function
     */
    public MapAccelerator(Function<inT, outT> function) {
        super(function);

        CopyToDevice<inT> copyToDevice = new CopyToDevice<>();
        OpenCLMap<inT, outT> openclMap = new OpenCLMap<>(function);
        CopyToHost<outT> copyToHost = new CopyToHost<>();

        decomposition = copyToDevice.andThen(openclMap).andThen(copyToHost);
    }

    @Override
    public PArray<outT> prepareExecution(PArray<inT> input) {
        PArray<outT> out = decomposition.prepareExecution(input);
        preparedExecutionFinish = true;
        return out;
    }

    private PArray<outT> deoptimize(PArray<inT> input, Exception e) {
        // An exception in the Graal-OpenCL is launched.
        // In this case try the Java multiple-thread map
        if (MarawaccOptions.DEOPTIMIZE) {
            preparedExecutionFinish = false;
            System.err.println("[Deptimisation] Using MapJavaThreads");
            MapJavaThreads<inT, outT> threadsMap = new MapJavaThreads<>(function);
            decomposition = threadsMap;
            return threadsMap.apply(input);
            // return super.apply(input);
        } else {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    @Override
    public PArray<outT> apply(PArray<inT> input) {

        if (!preparedExecutionFinish) {
            prepareExecution(input);
        }

        try {
            return decomposition.apply(input);
        } catch (Exception e) {
            return deoptimize(input, e);
        }
    }
}
