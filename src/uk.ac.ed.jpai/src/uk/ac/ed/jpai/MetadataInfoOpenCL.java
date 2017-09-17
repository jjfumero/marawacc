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

import java.util.HashMap;
import java.util.UUID;

import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;

public class MetadataInfoOpenCL {

    private HashMap<Direction, Object[]> parameters;
    private int[] fromTo;
    private AcceleratorOCLInfo acceleratorOCLInfo;
    private UUID uuidData;
    private Object input;
    private Object output;
    private int sizeInput;

    public MetadataInfoOpenCL(int sizeInput) {
        this.sizeInput = sizeInput;
    }

    public int getInputSize() {
        return this.sizeInput;
    }

    public HashMap<Direction, Object[]> getParameters() {
        return parameters;
    }

    public void setParameters(HashMap<Direction, Object[]> parameters) {
        this.parameters = parameters;
    }

    public int[] getFromTo() {
        return fromTo;
    }

    public void setFromTo(int[] fromTo) {
        this.fromTo = fromTo;
    }

    public AcceleratorOCLInfo getAcceleratorOCLInfo() {
        return acceleratorOCLInfo;
    }

    public void setAcceleratorOCLInfo(AcceleratorOCLInfo acceleratorOCLInfo) {
        this.acceleratorOCLInfo = acceleratorOCLInfo;
    }

    public UUID getUUIDData() {
        return uuidData;
    }

    public void setUUIDData(UUID uuidData) {
        this.uuidData = uuidData;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getInput() {
        return this.input;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public Object getOutput() {
        return this.output;
    }
}
