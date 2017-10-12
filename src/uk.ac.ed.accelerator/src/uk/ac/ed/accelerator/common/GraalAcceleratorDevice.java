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
import java.util.UUID;
import java.util.Vector;

public abstract class GraalAcceleratorDevice {

    protected int id;
    protected String name;
    protected String vendorName;
    protected String version;
    protected long type;

    protected Object deviceReference;

    protected Vector<GraalAcceleratorVar> deviceVar;

    protected boolean kernelTimeBenchmarking;

    public GraalAcceleratorDevice(int id) {
        this.id = id;
    }

    public void enableForBechmarking() {
        this.kernelTimeBenchmarking = true;
    }

    public void disableForBechmarking() {
        this.kernelTimeBenchmarking = false;
    }

    public void addDeviceVar(GraalAcceleratorVar var) {
        if (deviceVar == null) {
            deviceVar = new Vector<>();
        }
        deviceVar.add(var);
    }

    public GraalAcceleratorVar getDeviceVar(int index) {
        return this.deviceVar.get(index);
    }

    public int getID() {
        return id;
    }

    public abstract String getDeviceType();

    public abstract void setDeviceReference(Object deviceReference);

    public abstract String getName();

    public abstract String getVendorName();

    public abstract String getVersion();

    public abstract OCLVendor getVendor();

    public abstract Object getPlatform();

    public abstract Object getDevice();

    public abstract Object getDeviceInfo();

    public abstract long getType();

    public abstract void buildProgram(UUID uuidKernel, int cacheIndex) throws Exception;

    public abstract Object createProgram(UUID uuidKernel, int cacheIndex) throws Exception;

    public abstract Object createKernel(UUID uuidKernel, int cacheIndex);

    protected abstract Object createContext();

    public abstract Object createCommandQueue();

    public abstract GraalAcceleratorVar registerOffloadVariable(GraalAcceleratorVar oclVar);

    public abstract Object allocateBufferAndWriteBuffer(ArrayList<?> clParams, int[] fromTo, UUID uuidData, ParallelSkeleton functionOperation);

    public abstract void clean();

}
