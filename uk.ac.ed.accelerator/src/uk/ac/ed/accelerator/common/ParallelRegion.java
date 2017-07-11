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

import java.util.HashMap;
import java.util.Vector;

/*
 * -----------------------------------
 * 1 region - single context - 1 queue
 *                           - 1 stream
 * -----------------------------------
 */
public class ParallelRegion {

    // Thread-safe
    private Vector<GraalAcceleratorVar> deviceVars;

    private HashMap<Object, GraalAcceleratorVar> hostVars;
    private HashMap<GraalAcceleratorVar, GraalAcceleratorVar> mapVars;

    private Vector<GraalAcceleratorDevice> devices;

    public ParallelRegion() {
        devices = new Vector<>();
        deviceVars = new Vector<>();
        mapVars = new HashMap<>();
        hostVars = new HashMap<>();
    }

    public void addAcceleratorDeviceVar(GraalAcceleratorVar hostVar, GraalAcceleratorVar deviceVar) {
        deviceVars.add(deviceVar);
        mapVars.put(hostVar, deviceVar);
    }

    public void addAcceleratorHostVar(Object cpuReference, GraalAcceleratorVar hostVar) {
        hostVars.put(cpuReference, hostVar);
    }

    public void addDeviceToRegion(GraalAcceleratorDevice device) {
        devices.addElement(device);
    }

    public GraalAcceleratorDevice getDevice(int index) {
        return devices.get(index);
    }

    public GraalAcceleratorVar getVar(int index) {
        return deviceVars.get(index);
    }

    @SuppressWarnings("unused")
    public void setContext(Object context) {

    }

    public void clean() {

    }

}
