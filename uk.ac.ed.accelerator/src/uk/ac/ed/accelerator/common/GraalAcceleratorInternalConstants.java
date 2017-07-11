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
package uk.ac.ed.accelerator.common;

/**
 * Global constants for Accelerators.
 *
 */
public interface GraalAcceleratorInternalConstants {

    /**
     * Max number of iterations in the kernel benchmarks.
     */
    int MAX_KERNEL_ITERATIONS = 100;

    /**
     * Max number of concurrent OpenCL states.
     */
    int MAX_CONCURRENT_OCL_STAGES = 3;

    /**
     * ID of final states.
     */
    int FINAL_STAGE = 4;

    /**
     * Max number of contexts.
     */
    int MAXCONTEXTS = 10;
}
