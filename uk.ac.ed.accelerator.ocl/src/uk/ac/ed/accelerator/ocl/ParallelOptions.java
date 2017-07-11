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

    @Option(help = "See comments during compilation") public static final OptionValue<Boolean> EnableComments = new OptionValue<>(false);
    @Option(help = "Use CPU or GPU") public static final OptionValue<Boolean> UseCPU = new OptionValue<>(false);
    @Option(help = "Set global work size") public static final OptionValue<Integer> WorkSize = new OptionValue<>(1000);
    @Option(help = "Verbose of internal timers and code generation") public static final OptionValue<Boolean> Verbose = new OptionValue<>(false);
    @Option(help = "Generate Kernel from Lambda expression using new interface (JDK8 required)") public static final OptionValue<Boolean> UseFunctionalJPAIGPU = new OptionValue<>(true);
    @Option(help = "Explicit code generation for Structure of Arrays (SoA)") public static final OptionValue<Integer> UseSoAWithValue = new OptionValue<>(0);
    @Option(help = "Explicit code generation for Structure of Arrays (SoA)") public static final OptionValue<Integer> UseSoAWithValueforOutput = new OptionValue<>(0);
    @Option(help = "Using Pipelining segmentation") public static final OptionValue<Boolean> UsePipeline = new OptionValue<>(false);
    @Option(help = "Using Pipelining marshalling") public static final OptionValue<Boolean> PartialMarshalling = new OptionValue<>(false);
    @Option(help = "Using Pipelining marshalling") public static final OptionValue<Boolean> UseSimplePipeline = new OptionValue<>(false);
    @Option(help = "Using Pipelining copy") public static final OptionValue<Boolean> UseSubCopy = new OptionValue<>(false);
    @Option(help = "Time breakdown for each stage in the pipeline") public static final OptionValue<Boolean> PrintTimeStages = new OptionValue<>(false);

    // Data Structures
    @Option(help = "Will contain the kernel execution time") public static final OptionValue<Double> KernelTime = new OptionValue<>(-1.0);
    @Option(help = "Will contain the kernel generation time") public static final OptionValue<Double> KernelGenTime = new OptionValue<>(-1.0);
    @Option(help = "Will contain the overall execution time") public static final OptionValue<Double> OverallTime = new OptionValue<>(-1.0);
    @Option(help = "Will contain the marshalling time") public static final OptionValue<Double> MarshallTime = new OptionValue<>(-1.0);
    @Option(help = "Will contain the unmarshalling time") public static final OptionValue<Double> UnmarshallTime = new OptionValue<>(-1.0);
    @Option(help = "Will contain the transfer data time") public static final OptionValue<Double> TransferTime = new OptionValue<>(-1.0);
    @Option(help = "Will contain the read data time") public static final OptionValue<Double> ReadTime = new OptionValue<>(-1.0);
    @Option(help = "Will contain the build time") public static final OptionValue<Double> BuildTime = new OptionValue<>(-1.0);

}
