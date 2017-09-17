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

package uk.ac.ed.accelerator.ocl.runtime;

import java.util.HashMap;

import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.utils.ArrayPackage;

/**
 * Class to store references to the global data for each pipelines.
 *
 * @author Juan Fumero
 *
 */
public final class GlobalDataPipeline {

    public static GlobalDataPipeline instance = null;
    private boolean dataOnShelf;
    private boolean isGPUCodeGenerated;
    private HashMap<Direction, Object[]> parameters;
    private ArrayPackage arrayPackage;

    public static GlobalDataPipeline getInstance() {
        if (instance == null) {
            instance = new GlobalDataPipeline();
        }
        return instance;
    }

    private GlobalDataPipeline() {
        dataOnShelf = false;
    }

    public void setGPUCodeGenerated() {
        isGPUCodeGenerated = true;
    }

    public boolean isCodeGenerated() {
        return isGPUCodeGenerated;
    }

    public void setParameters(HashMap<Direction, Object[]> parameters) {
        this.parameters = parameters;
        dataOnShelf = true;
    }

    public HashMap<Direction, Object[]> getParameters() {
        return parameters;
    }

    public Object[] getInput() {
        return parameters.get(Direction.INPUT);
    }

    public Object[] getOutput() {
        return parameters.get(Direction.OUTPUT);
    }

    public boolean dataOnShelf() {
        return dataOnShelf;
    }

    public void setArrayContainer(ArrayPackage arrayPackage) {
        this.arrayPackage = arrayPackage;
    }

    public ArrayPackage getArrayContainer() {
        return this.arrayPackage;
    }

    public void clear() {
        parameters = null;
        isGPUCodeGenerated = false;
        instance = null;
    }
}
