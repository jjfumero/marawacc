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

/**
 * Class for storage Input/Output GPU parameters. It also storages the size associated with the
 * number (estimation) of threads to be launched on GPU.
 *
 * @author Juan Fumero
 *
 */
public class GPUParameters {

    HashMap<Direction, Object[]> parameters;

    public void putInput(Object[] input) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(Direction.INPUT, input);
    }

    public void setParameters(HashMap<Direction, Object[]> parameters) {
        this.parameters = parameters;
    }

    public HashMap<Direction, Object[]> getParameters() {
        return parameters;
    }

    public <T> void setOutput(T output) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(Direction.OUTPUT, new Object[]{output});
    }

    public Object[] getOutput() {
        return parameters.get(Direction.OUTPUT);
    }
}
