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

import java.nio.ByteBuffer;

public abstract class GraalAcceleratorVar {

    protected boolean isArray;
    protected boolean isReadOnly;
    protected Object javaParam;
    protected Object flatParam;
    protected int type;
    protected int length;
    protected Class<?> klass;
    protected int tupleNumber;

    protected int offset;
    protected int from; // Original Java Array
    protected int to;   // Original Java Array

    protected boolean structPointers;

    protected GraalAcceleratorDevice device;

    protected float[] ficticious;

    protected boolean isFlatten;

    protected int direction;

    // Extra array flag
    protected boolean extraArray;

    public void setExtraArray(boolean extraArray) {
        this.extraArray = extraArray;
    }

    public boolean isExtraArray() {
        return this.extraArray;
    }

    public Object getFlatArray() {
        return this.flatParam;
    }

    public int getArrayLength() {
        return this.length;
    }

    public int getType() {
        return this.type;
    }

    public Object getJavaParam() {
        return this.javaParam;
    }

    public boolean isArray() {
        return this.isArray;
    }

    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    public boolean setReadOnly(boolean read) {
        return isReadOnly = read;
    }

    public Class<?> getObjectClass() {
        return this.klass;
    }

    public int getTupleNumber() {
        return this.tupleNumber;
    }

    public boolean getStructPointer() {
        return this.structPointers;
    }

    // Multi-device support
    public void setFrom(int from) {
        this.from = from;
    }

    public int getFrom() {
        return this.from;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getTo() {
        return this.to;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setDevice(GraalAcceleratorDevice device) {
        this.device = device;
    }

    public GraalAcceleratorDevice getDevice() {
        return this.device;
    }

    public float[] getFicticious() {
        return this.ficticious;
    }

    public void setFlatten(boolean flatten) {
        this.isFlatten = flatten;
    }

    public boolean isFlatten() {
        return this.isFlatten;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return this.direction;
    }

    public abstract Object getPointer();

    public abstract void setStructBuffer(ByteBuffer byteBuffer);

    public abstract ByteBuffer getStructBuffer();

    public abstract void setContext(Object context);

    public abstract Object getContext();

    public abstract void setCommandQueue(Object queue);

    public abstract Object getCommandQueue();

    // Allocation
    public abstract void createDeviceBuffers(int chunkPipeline);

    public abstract Object writeBufferByJavaThreadNew(Object clMemJavaObjects, int[] fromTo);

    public abstract Object readParameterByJavaThread(Object pointer, Object kernel, int[] fromTo, Object pipelineMemObject) throws Exception;

    public abstract Object getMemObject();

    public abstract void clean();

}
