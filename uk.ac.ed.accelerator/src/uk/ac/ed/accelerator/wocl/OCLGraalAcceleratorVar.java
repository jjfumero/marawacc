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
package uk.ac.ed.accelerator.wocl;

import java.nio.ByteBuffer;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_event;
import org.jocl.cl_mem;

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorVar;
import uk.ac.ed.accelerator.utils.LoggerMarawacc;
import uk.ac.ed.accelerator.utils.StageInfo;
import uk.ac.ed.accelerator.wocl.PipelineTimeDescritor.Stage;

public class OCLGraalAcceleratorVar extends GraalAcceleratorVar {

    // Pointers
    private Pointer pToObj;
    ByteBuffer structBuffer;

    // OpenCL specific (execution and platform)
    private cl_context context;
    private cl_command_queue queue;
    // private cl_event event;

    // OpenCL specific (memory specific)
    private cl_mem memObject;

    // flags

    public OCLGraalAcceleratorVar(Object javaParam, Object flatParam, Pointer p, int type, boolean isArray, boolean isReadOnly, int length) {
        this.javaParam = javaParam;
        this.flatParam = flatParam;

        this.type = type;

        this.isArray = isArray;
        this.isReadOnly = isReadOnly;
        this.length = length;
        this.pToObj = p;
        this.klass = null;
        this.structBuffer = null;
        this.tupleNumber = 0;
        this.extraArray = false;
        init();
    }

    public OCLGraalAcceleratorVar(Object javaParam, Object flatParam, Pointer p, int type, boolean isArray, boolean isReadOnly, int length, Class<?> klass, int dim) {
        this.javaParam = javaParam;
        this.flatParam = flatParam;
        this.type = type;
        this.isArray = isArray;
        this.isReadOnly = isReadOnly;
        this.length = length;
        this.pToObj = p;
        this.klass = klass;
        this.structBuffer = null;
        this.tupleNumber = dim;
        this.extraArray = false;
        init();
    }

    private void init() {
        structPointers = false;
    }

    @Override
    public Pointer getPointer() {
        return this.pToObj;
    }

    @Override
    public void setStructBuffer(ByteBuffer byteBuffer) {
        this.structBuffer = byteBuffer;
    }

    @Override
    public ByteBuffer getStructBuffer() {
        return this.structBuffer;
    }

    private long resolveFlags() {
        // Determine the flags
        long flags = 0;
        if (this.isArray()) {
            if (this.isReadOnly()) {
                flags = CL.CL_MEM_READ_ONLY;
            } else {
                flags = CL.CL_MEM_READ_WRITE;
            }
        } else {
            flags = CL.CL_MEM_READ_ONLY;
        }
        return flags;
    }

    @Override
    public void createDeviceBuffers(int chunkPipeline) {

        long flags = resolveFlags();

        // Allocate memory
        memObject = CL.clCreateBuffer(context, flags, this.getType() * this.getArrayLength(), null, null);

        if (GraalAcceleratorOptions.printOCLInfo) {
            LoggerMarawacc.info("[OCL clCreateBuffer] Creating Buffer of size = " + this.getArrayLength());
        }
    }

    private static int getBytesForDataType(int datatType) {
        if (datatType == Sizeof.cl_float) {
            return 4;
        } else if (datatType == Sizeof.cl_int) {
            return 4;
        } else if (datatType == Sizeof.cl_double) {
            return 8;
        } else if (datatType == Sizeof.cl_long) {
            return 8;
        } else if (datatType == Sizeof.cl_short) {
            return 2;
        } else if (datatType == Sizeof.cl_char) {
            return 1;
        }
        return 0;
    }

    @Override
    public StageInfo writeBufferByJavaThreadNew(Object clMemJavaObjects, int[] fromTo) {

        cl_mem buffer = (cl_mem) clMemJavaObjects;

        if (context == null) {
            throw new RuntimeException("[Context is null]");
        }

        if (this.direction == GraalOCLConstants.COPY_IN) {
            cl_event writeEvent = new cl_event();
            boolean block = (GraalAcceleratorOptions.offloadSync == true) ? CL.CL_TRUE : CL.CL_FALSE;
            int fromHotsOffset = (this.extraArray) ? 0 : fromTo[0];
            if (GraalAcceleratorOptions.printOCLInfo) {
                LoggerMarawacc.info("[OCL EnqueueWriteBuffer] writing variable Host -> Device  -- from " + fromHotsOffset);
            }
            int bytesForDataType = getBytesForDataType(this.getType());

            Pointer ptr = getPointer();
            ptr = ptr.withByteOffset(fromHotsOffset * bytesForDataType);
            int totalSize = getType() * getArrayLength();
            CL.clEnqueueWriteBuffer(queue, buffer, block, 0, totalSize, ptr, 0, null, writeEvent);

            // CL.clEnqueueWriteBufferWithHostOffset(queue, buffer, block, 0, totalSize,
            // this.getPointer(), 0, null, event, fromHotsOffset * bytesForDataType);

            this.memObject = buffer;
            return (new StageInfo(buffer, writeEvent, this.getPointer()));
        }
        return (new StageInfo(null, null, this.getPointer()));
    }

    @Override
    public cl_event readParameterByJavaThread(Object pointerJavaObject, Object kernelEventObject, int[] fromTo, Object pipelineMemObject) throws Exception {

        if (queue == null) {
            throw new Exception("Queue variable is null");
        }

        if (!this.isReadOnly() && (this.direction == GraalOCLConstants.COPY_OUT)) {
            cl_event eventReader = new cl_event();
            cl_event[] eventKernel = (cl_event[]) kernelEventObject;
            cl_mem pipelineMem = (cl_mem) pipelineMemObject;

            if (GraalAcceleratorOptions.printOCLInfo) {
                LoggerMarawacc.info("[OCL EnqueueReaderBuffer] reading variable from Device -> Host  >>> FROM : " + fromTo[0] + "  to" + (fromTo[0] + (this.getArrayLength())));
            }

            int bytesForDataType = getBytesForDataType(this.getType());
            int fromHotsOffset = (this.extraArray) ? 0 : fromTo[0];

            int totalSize = this.getType() * this.getArrayLength();
            Pointer basePointer = (Pointer) pointerJavaObject;
            Pointer pointer = basePointer.withByteOffset(fromHotsOffset * bytesForDataType);

            long start = System.nanoTime();
            CL.clEnqueueReadBuffer(queue, pipelineMem, CL.CL_TRUE, 0, totalSize, pointer, eventKernel.length, eventKernel, eventReader);
            // CL.clEnqueueReadBufferWithHostOffset(queue, pipelineMem, CL.CL_TRUE, 0,
            // this.getType() * this.getArrayLength(), pointer, eventKernel.length, eventKernel,
            // eventReader, fromHotsOffset *
            // bytesForDataType);
            long end = System.nanoTime();

            PipelineTimeDescritor.getInstance().put(Stage.FINE_TUNE_COPY_OUT, (end - start));
            PipelineTimeDescritor.getInstance().put(Stage.FINE_TUNE_COPY_START, start);
            PipelineTimeDescritor.getInstance().put(Stage.FINE_TUNE_COPY_STOP, end);

            return eventReader;
        }
        return null;
    }

    @Override
    public void setContext(Object context) {
        this.context = (cl_context) context;
    }

    @Override
    public cl_context getContext() {
        return this.context;
    }

    @Override
    public void setCommandQueue(Object queue) {
        this.queue = (cl_command_queue) queue;
    }

    @Override
    public cl_command_queue getCommandQueue() {
        return this.queue;
    }

    @Override
    public cl_mem getMemObject() {
        return this.memObject;
    }

    @Override
    public String toString() {
        String result = "Array: " + this.isArray;
        result += "\nLength: " + this.length;
        result += "\nRead Only: " + this.isReadOnly;
        return result;
    }

    @Override
    public void clean() {

    }
}
