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

package uk.ac.ed.jpai.test.base;

import org.junit.Before;

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.jpai.MarawaccOptions;

/**
 * Base class for all the OpenCL tests.
 *
 */
public class MarawaccOpenCLTestBase {

    @Before
    public void setUp() {
        MarawaccOptions.DEOPTIMIZE = false;
        GraalAcceleratorOptions.printOCLInfo = true;
        GraalAcceleratorOptions.printGraalIR = true;
        GraalAcceleratorOptions.printOffloadKernel = true;

        try {
            // This is a mechanism for waiting a random number of milliseconds between 0 and 999 for
            // the next test.
            int milliseconds = (int) (Math.random() * 1000);
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
