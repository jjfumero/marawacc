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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class PArraySequencesTest extends MarawaccOpenCLTestBase {

    @Test
    public void basicSequence() {
        PArray<Integer> sequence = new PArray<>(3, TypeFactory.Integer(), StorageMode.OPENCL_BYTE_BUFFER, true);
        sequence.enableSequence(1, 1, 0);
        sequence.setTotalSize(100);

        ArrayFunction<Integer, Integer> function = new MapAccelerator<>(x -> x + 100);
        PArray<Integer> result = function.apply(sequence);
        assertNotNull(result);

        for (int i = 0; i < result.size(); i++) {
            assertNotEquals(result.get(i), i + 100, 0.001);
        }
    }
}
