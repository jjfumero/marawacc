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

import java.util.ArrayList;

/**
 * GraalAccelerator Platform. This is an abstract class to represent the device and platform
 * information of specific-technology.
 *
 * @author juan.fumero
 *
 */
public abstract class GraalAcceleratorPlatform {

    /**
     * ID to identify the platform (not vendor or implementation dependent).
     */
    protected int platformID;

    protected ArrayList<GraalAcceleratorDevice> graalDevices;

    protected boolean devicesDiscovered = false;

    /**
     * Represents the next device available (tasks parallelism and multikernel).
     */
    protected int[] deviceNextContext;

    public GraalAcceleratorPlatform() {
        // It Can be moved to Region (Upper section)
        deviceNextContext = new int[GraalAcceleratorInternalConstants.MAXCONTEXTS];
        for (int i = 0; i < GraalAcceleratorInternalConstants.MAXCONTEXTS; i++) {
            deviceNextContext[i] = 0;
        }
        graalDevices = new ArrayList<>();
    }

    public void setDevice(GraalAcceleratorDevice dev) {
        graalDevices.add(dev);
    }

    public GraalAcceleratorDevice getDevice() {
        return graalDevices.get(0);
    }

    public GraalAcceleratorDevice getDevice(int idx) {
        return graalDevices.get(idx);
    }

    public ArrayList<GraalAcceleratorDevice> getDevices() {
        return graalDevices;
    }

    public boolean isDevicesDiscovered() {
        return devicesDiscovered;
    }

    public abstract boolean isMultiDeviceAvailable();

    public abstract ArrayList<GraalAcceleratorDevice> getDevicesForMultiGPU(int idx);

    public abstract int getNumCurrentCurrentDevices();

    public abstract int getNumTotalDevices();

    public abstract Object getPlatformID();

    public abstract void clean();

    @Override
    public String toString() {
        return (new Integer(platformID)).toString();
    }
}
