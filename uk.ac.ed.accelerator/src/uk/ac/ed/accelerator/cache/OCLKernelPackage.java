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
package uk.ac.ed.accelerator.cache;

import org.jocl.cl_kernel;
import org.jocl.cl_program;

/**
 * Cache for storing the kernel Information.
 *
 */
public class OCLKernelPackage {

    private String oclCode;
    private String kernelName;

    // OpenCL specific data
    private cl_kernel clkernel;
    private cl_program program;
    private long[] binaryDataSizes;
    private byte[][] binaryDatas;

    // flags
    private boolean isCreatingProgram;
    private boolean isBuildingProgram;
    private boolean isCreatingKernel;

    private boolean binaryCreated;

    public OCLKernelPackage() {
        this.oclCode = null;
        this.program = null;
    }

    public void setKernelCode(String oclCode) {
        this.oclCode = oclCode;
    }

    public void setKernelBinary(cl_kernel kernel) {
        this.clkernel = kernel;
    }

    public void setKernelProgram(cl_program program) {
        this.program = program;
    }

    public String getKernelCode() {
        return this.oclCode;
    }

    public cl_kernel getKernelBinary() {
        return clkernel;
    }

    public cl_program getKernelProgram() {
        return this.program;
    }

    public boolean isCreatingProgram() {
        return isCreatingProgram;
    }

    public void setCreatingProgram(boolean isCreatingProgram) {
        this.isCreatingProgram = isCreatingProgram;
    }

    public boolean isBuildingProgram() {
        return isBuildingProgram;
    }

    public void setBuildingProgram(boolean isBuildingProgram) {
        this.isBuildingProgram = isBuildingProgram;
    }

    public boolean isCreatingKernel() {
        return isCreatingKernel;
    }

    public void setCreatingKernel(boolean isCreatingKernel) {
        this.isCreatingKernel = isCreatingKernel;
    }

    public void setBinaryDataSize(long[] binaryDataSizes) {
        this.binaryDataSizes = binaryDataSizes;
    }

    public long[] getBinaryDataSize() {
        return this.binaryDataSizes;
    }

    public void setBinaryData(byte[][] binaryData) {
        this.binaryDatas = binaryData;
    }

    public byte[][] getBinaryData() {
        return this.binaryDatas;
    }

    public void setBinaryCreated() {
        this.binaryCreated = true;
    }

    public boolean isBinaryCreated() {
        return this.binaryCreated;
    }

    public void setKernelName(String kernelName) {
        this.kernelName = kernelName;
    }

    public String getKernelName() {
        return this.kernelName;
    }
}
