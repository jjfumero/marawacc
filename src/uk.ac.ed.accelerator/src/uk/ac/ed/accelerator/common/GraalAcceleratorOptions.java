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
 * Graal-OpenCL runtime options.
 *
 */
public final class GraalAcceleratorOptions {

    /**
     * Worksize on the accelerator.
     */
    @Deprecated public static int workSize = 0;

    /**
     * Flag to indicate if computation has to be enforced on CPU.
     */
    public static boolean useCPU = false;

    /**
     * Use accelerator. First GPU by default.
     */
    public static boolean useACC = true;

    /**
     * Print references to cache for debugging.
     */
    public static boolean debugCacheGPUCode = false;

    /**
     * Apply profiling in the Accelerator
     */
    public static boolean profileOffload = true;

    /**
     * Warming up of the accelerator (Useful on AMD GPUs).
     */
    public static boolean offloadWarmingUp = false;

    /**
     * Print OCL Information such as Device and Platform.
     */
    public static boolean printOCLInfo = getBoolean("marawacc.printOCLInfo", false);

    /**
     * Print Graal-OCL IR. -XX:+PrintGralIR
     */
    public static boolean printGraalIR = getBoolean("marawacc.printGraalIR", false);

    /**
     * Print C OpenCL offload kernel. The kernel is automatically generated by the VM.
     *
     */
    public static boolean printOffloadKernel = getBoolean("marawacc.printOCLKernel", false);

    /**
     * Compile OpenCL kernel with --cl-fast-relaxed-math"
     */
    public static boolean relaxMathKernel = getBoolean("marawacc.relaxMathKernel", false);

    /**
     * Set true to set blocking calls in the OpenCL backend. -XX:+OffloadSync
     */
    public static boolean offloadSync = true;

    public static boolean useSubCopy = false;

    /**
     * Experimental: to include OpenCL multiple-devices.
     */
    public static boolean multiOpenCLDevice = getBoolean("marawacc.multidevice", false);

    /**
     * Experimental: batch processing when data does not fit on GPU memory.
     */
    public static boolean bachProcess = getBoolean("marawacc.batch", true);

    /**
     * Dump into file GraalIR for debugging.
     */
    public static boolean dumpGraalIRToFile = false;

    /**
     * Dump GraalIR into IGV for debugging.
     */
    public static boolean dumpGraalIR = getBoolean("marawacc.dumpGraph", false);

    /**
     * Report of each OpenCL Timer with its iteration.
     */
    public static boolean reportOCLTimers = false;

    /**
     * Print info that come from R front-end for debugging.
     */
    public static boolean printMessagesFromFastR = false;

    /**
     * Enable Guard deoptimization in the OpenCL Kernel.
     */
    public static boolean deoptGuardsEnabled = getBoolean("marawacc.guards", true);

    /**
     * It enables new strategy for PArrays and primitives
     */
    public static boolean newPArraysPrimitive = getBoolean("marawacc.primitives", false);

    /**
     * It enables debug info for OpenCL Multidevice
     */
    public static boolean debugMultiDevice = getBoolean("marawacc.debugMultidevice", false);

    /**
     * It enables OpenCL vector types. This is an experimental branch
     */
    public static boolean useVectorTypes = getBoolean("marawacc.useVectorTypes", false);

    /**
     * Debug the OpenCL kernel. It prints the IR nodes and its values as well as the corresponding
     * OpenCL code in the same output source code.
     */
    public static boolean debugOCLKernel = getBoolean("marawacc.debugOCLKernel", false);

    /**
     * Old option to print the template code generation.
     */
    public static boolean templateVerbose = false;

    /**
     * When running with multiple threads if this value is true the JIT compilation is performed via
     * Graal.
     */
    public static boolean threadsGraalCompilation = getBoolean("marawacc.threadGraalCompilatoin", false);

    /**
     * Set default value for Graal compilation.
     */
    public static int threadsGraalCompilationThreshold = getIntValue("marawacc.threadGraalCompilation", 10);

    /**
     * Platforms supported
     */
    public enum AcceleratorPlatformKind {

        OPENCL("OCL"),
        CUDA("CUDA");  // Future support for PTX.

        private final String kind;

        private AcceleratorPlatformKind(String kind) {
            this.kind = kind;
        }

        @Override
        public String toString() {
            return kind;
        }
    }

    @SuppressWarnings("unused")
    private static boolean getBoolean(String property) {
        if (System.getProperty(property) == null) {
            return false;
        } else if (System.getProperty(property).toLowerCase().equals("true")) {
            return true;
        }
        return false;
    }

    private static boolean getBoolean(String property, boolean defaultValue) {
        if (System.getProperty(property) == null) {
            return defaultValue;
        } else if (System.getProperty(property).toLowerCase().equals("true")) {
            return true;
        } else if (System.getProperty(property).toLowerCase().equals("false")) {
            return false;
        }
        return defaultValue;
    }

    private static int getIntValue(String property, int defaultValue) {
        if (System.getProperty(property) == null) {
            return defaultValue;
        } else {
            int value = Integer.parseInt(System.getProperty(property).toLowerCase());
            return value;
        }
    }

    /**
     * Default Accelerator Platform (Marawacc) is OpenCL.
     */
    public final static AcceleratorPlatformKind DEFAULT_ACCELERATOR_PLATFORM = AcceleratorPlatformKind.OPENCL;

}
