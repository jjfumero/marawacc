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
package uk.ac.ed.accelerator.utils;

import java.util.Arrays;
import java.util.UUID;

import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorDevice;

public final class OCLRuntimeMetadata {

    private Object[] finalParameters;
    private Object clParams;
    private LocalStageOCLInfo oclStageLocalInfo;
    private int[] fromTo;
    private boolean[] ioMask;
    private boolean[] eventsIOMask;
    private long start;
    private ArrayPackage arrayPackage;
    private int[] finalDirection;
    private int outputIDXStart;
    private OCLGraalAcceleratorDevice device;
    private UUID uuidData;

    public OCLRuntimeMetadata() {

    }

    public void setDevice(OCLGraalAcceleratorDevice device) {
        this.device = device;
    }

    public OCLGraalAcceleratorDevice getDevice() {
        return this.device;
    }

    public void setUUIDData(UUID uuidData) {
        this.uuidData = uuidData;
    }

    public UUID getUUIDData() {
        return this.uuidData;
    }

    public Object getOCLGraalAcceleratorVar() {
        return clParams;
    }

    public void setOCLGraalAcceleratorVar(Object clParams) {
        this.clParams = clParams;
    }

    public void setParameters(Object[] parameters) {
        this.finalParameters = parameters;
    }

    public Object[] getFinalParameters() {
        return this.finalParameters;
    }

    public void setOCLStageLocalInfo(LocalStageOCLInfo localInfo) {
        this.oclStageLocalInfo = localInfo;
    }

    public LocalStageOCLInfo getOCLStageLocalInfo() {
        return this.oclStageLocalInfo;
    }

    public void setSize(int[] fromTo) {
        this.fromTo = fromTo;
    }

    public void setFromTo(int[] fromTo) {
        setSize(fromTo);
    }

    public int[] getFromTo() {
        return this.fromTo;
    }

    public int getSize() {
        return (this.fromTo[1] - this.fromTo[0]);
    }

    public void setIOMask(boolean[] ioMask) {
        this.ioMask = ioMask;
    }

    public boolean[] getIOMask() {
        return this.ioMask;
    }

    public void setEventsIOMask(boolean[] eventIOMask) {
        this.eventsIOMask = eventIOMask;
    }

    public boolean[] getEventsIOMask() {
        return this.eventsIOMask;
    }

    @Override
    public String toString() {
        return Arrays.toString(finalParameters);
    }

    public void setStartTransferTime(long start) {
        this.start = start;
    }

    public long getStartTransferTime() {
        return this.start;
    }

    public ArrayPackage getArrayContainer() {
        return arrayPackage;
    }

    public void setArrayContainer(ArrayPackage arrayPackage) {
        this.arrayPackage = arrayPackage;
    }

    public void setFinalDirection(int[] finalDirection) {
        this.finalDirection = finalDirection;
    }

    public int[] getFinalDirection() {
        return this.finalDirection;
    }

    public void setOutputIDXStart(int idx) {
        this.outputIDXStart = idx;
    }

    public int getOutputIDXStart() {
        return this.outputIDXStart;
    }
}
