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

import jdk.vm.ci.options.Option;
import jdk.vm.ci.options.OptionValue;

public class ParallelOptions {
    @Option(help = "Verbose of internal timers and code generation") public static final OptionValue<Boolean> Verbose = new OptionValue<>(false);
    @Option(help = "Generate Kernel from Lambda expression using new interface (JDK8 required)") public static final OptionValue<Boolean> UseFunctionalJPAIGPU = new OptionValue<>(true);
    @Option(help = "Explicit code generation for Structure of Arrays (SoA)") public static final OptionValue<Integer> UseSoAWithValue = new OptionValue<>(0);
    @Option(help = "Using Pipelining marshalling") public static final OptionValue<Boolean> UseSimplePipeline = new OptionValue<>(false);
}
