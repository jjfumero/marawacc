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

package uk.ac.ed.jpai.test.parrays;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

/**
 * This is currently not supported. This test is to see how the Graal IR looks like in order to
 * implement the object creation of the PArray in the OpenCL code generator.
 *
 * Execute: mx igv & with the VM option -Dmarawacc.dumpGraph=true to visualize the graph
 *
 */
public class PArrayAllocationTest extends MarawaccOpenCLTestBase {

    @Test
    @Ignore
    public void allocation01() {
        PArray<Integer> sequence = new PArray<>(10, TypeFactory.Integer(), StorageMode.OPENCL_BYTE_BUFFER, true);

        ArrayFunction<Integer, Integer> function = new MapAccelerator<>(x -> {

            PArray<Integer> p = new PArray<>(5, TypeFactory.Integer());
            p.put(0, 10);
            p.put(1, 11);

            return x + 100;
        });
        PArray<Integer> result = function.apply(sequence);
        assertNotNull(result);

    }
}