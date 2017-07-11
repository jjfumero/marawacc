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

package uk.ac.ed.accelerator.profiler;

/**
 * Profiler timers for the whole back-end.
 */
public enum ProfilerType {

    // General timers
    TOTAL_TIME("TotalTime"),
    SEQUENTIAL_TIME("Sequential"),

    //
    COPY_TO_DEVICE("CopyToDevice"),
    COMPUTE_MAP("ComputeMap"),
    COPY_TO_HOST("CopyToHost"),

    // TRUFFLE R
    TRUFFLE_MARSHAL("ASTMarshal"),
    TRUFFLE_EXECUTE("ASTExecute"),
    TRUFFLE_UNMARSHAL("ASTUnmarshal"),
    TRUFFLE_RLIST_CONVERSION("RLISTConversion"),
    TRUFFLE_COMPILATION_TIME("CompilationTime"),

    GENERAL_LOG_MESSAGE("LOG"),

    DEOPTTRACE("DEOPTTRACE"),

    MARSHAL("Marshal"),
    UNMARSHAL("Unmarshal"),

    COMPUTE_MAP_PREPARE("ComputeMapPrepare"),

    OCL_WRITE_BUFFER("OCLWriteBuffer"),
    OCL_WRITE_BUFFER_METADATA("OCLWriteBufferMetadata"),
    OCL_READ_BUFFER("OCLReadBuffer"),

    OCL_KERNEL("OCLKernel"),

    // OpenCL counters
    KERNEL_GENERATION_TIME("OpenCL_KernelGeneration_VISITOR"),
    SERIAL_OUTPUT("SerialOutput"),
    BUILD_TIME("BuildKernel"),
    COPY_IN("CopyIN"),
    KERNEL_EXECUTION("KernelExecution"),
    OCLKERNEL_EXECUTION("OCLKernelExecution"),
    COPY_OUT("CopyOut"),
    MARSHALLING_TIME("Marshalling"),
    UNMARSHALL_TIME("Unmarshalling"),
    OCL_GRAAL_DRIVER_COMMAND_TO_CREATE_KERNEL("OCL_GRAAL_DRIVER_COMMAND_TO_CREATE_KERNEL"),
    OCL_GRAAL_DRIVER_COMPILE_KERNEL("OCL_GRAAL_DRIVER_COMPILE_KERNEL"),
    OCL_DRIVER_COMPILE_KERNEL("OCL_DRIVER_COMPILE_KERNEL"),
    Graal_OpenCL_Code_Generation("Graal_OpenCL_Code_Generation"),

    // Section counters
    WARMING_UP("WarmingUP"),
    TRANSFORMATION_MAP("TransformationMAP"),
    CREATE_DATA_TYPES("CreateDataTypes"),
    DECOMPACT("CreateDataTypes"),
    PATTERN_APPLY_JAVA("PatternApplyJava"),
    PATTERN_APPLY_OCL("PatternApplyOCL"),

    // Marshall step
    M_REAL_MARSHAL("RealMarshal"),
    M_PROCESS_INPUT("MProcessInput"),
    M_UNROLL_OUTPUT("MUnrollOutput"),
    M_ARRAY_COPY("MUnrollCopy"),
    M_PROCESS_OUTPUT("MProcessOutput"),
    M_GET_POINTER("MGETPointer"),
    M_TYPEFROMARRAY("MGetTypeFromArrayNonPrimitive"),
    M_OUTPUTJAVAALLOCATION("M_OutputJavaAllocation"),

    // Fine tupe, copy IN
    FINE_COPY_IN("FineCopyIN"),

    // Pipeline
    FINE_PIPELINE("Pipeline_exclusive"),
    MERGE_PIPELINE("Merge_pipeline"),
    CLEAN_PIPELINE("CleanPipeline"),

    FINE_TUNE_COPY_OUT("FineTune COPYOUT"),

    M_PROCESS_GET_OUT_REFERENCES("M_PROCESS_GET_OUT_REFERENCES");

    private String description;

    ProfilerType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return this.description;
    }
}
