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

import java.util.ArrayList;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.EventCallbackFunction;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_device_id;
import org.jocl.cl_event;

import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.accelerator.wocl.PipelineTimeDescritor;
import uk.ac.ed.accelerator.wocl.PipelineTimeDescritor.Stage;

public interface OpenCLUtils {

    static String getType(long deviceType) {
        if (deviceType == CL.CL_DEVICE_TYPE_CPU) {
            return "CL_DEVICE_TYPE_CPU";
        } else if (deviceType == CL.CL_DEVICE_TYPE_GPU) {
            return "CL_DEVICE_TYPE_GPU";
        } else if (deviceType == CL.CL_DEVICE_TYPE_ACCELERATOR) {
            return "CL_DEVICE_TYPE_ACCELERATOR";
        } else {
            return "UNKNOW";
        }
    }

    static long getLong(cl_device_id device, int paramName) {
        return getLongs(device, paramName, 1)[0];
    }

    static long[] getLongs(cl_device_id device, int paramName, int numValues) {
        long[] values = new long[numValues];
        CL.clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues, Pointer.to(values), null);
        return values;
    }

    static String getString(cl_device_id device, int paramName) {
        long[] size = new long[1];
        CL.clGetDeviceInfo(device, paramName, 0, null, size);
        byte[] buffer = new byte[(int) size[0]];
        CL.clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, buffer.length - 1);
    }

    static void waitForEvents(cl_event[] events) {
        CL.clWaitForEvents(events.length, events);
    }

    static void waitForEvents(ArrayList<ArrayList<cl_event>> events) {
        for (ArrayList<cl_event> eventsInContext : events) {
            if (eventsInContext.size() > 0) {
                cl_event[] array = eventsInContext.toArray(new cl_event[eventsInContext.size()]);
                CL.clWaitForEvents(array.length, array);
            }
        }
    }

    static long getTimeEvent(cl_event event) {
        waitForEvents(new cl_event[]{event});
        long[] start = new long[1];
        long[] end = new long[1];
        CL.clGetEventProfilingInfo(event, CL.CL_PROFILING_COMMAND_START, Sizeof.cl_long, Pointer.to(start), null);
        CL.clGetEventProfilingInfo(event, CL.CL_PROFILING_COMMAND_END, Sizeof.cl_long, Pointer.to(end), null);
        return (end[0] - start[0]);
    }

    static long getTimeSubmit(cl_event event) {
        waitForEvents(new cl_event[]{event});
        long[] submit = new long[1];
        CL.clGetEventProfilingInfo(event, CL.CL_PROFILING_COMMAND_SUBMIT, Sizeof.cl_long, Pointer.to(submit), null);
        return submit[0];
    }

    static long getTimeQueue(cl_event event) {
        waitForEvents(new cl_event[]{event});
        long[] queue = new long[1];
        CL.clGetEventProfilingInfo(event, CL.CL_PROFILING_COMMAND_QUEUED, Sizeof.cl_long, Pointer.to(queue), null);
        return queue[0];
    }

    static long getTimeEvent(Stage stage, cl_event event) {
        long[] start = new long[1];
        long[] end = new long[1];

        CL.clGetEventProfilingInfo(event, CL.CL_PROFILING_COMMAND_START, Sizeof.cl_long, Pointer.to(start), null);
        CL.clGetEventProfilingInfo(event, CL.CL_PROFILING_COMMAND_END, Sizeof.cl_long, Pointer.to(end), null);

        switch (stage) {
            case OCLHOSTTODEVICE:
                PipelineTimeDescritor.getInstance().put(Stage.OCLHOSTTODEVICESTART, start[0]);
                PipelineTimeDescritor.getInstance().put(Stage.OCLHOSTTODEVICESTOP, end[0]);
                break;
            case OCLEXECUTION:
                PipelineTimeDescritor.getInstance().put(Stage.OCLEXECUTIONSTART, start[0]);
                PipelineTimeDescritor.getInstance().put(Stage.OCLEXECUTIONSTOP, end[0]);
                break;
            case OCLDEVICETOHOST:
                PipelineTimeDescritor.getInstance().put(Stage.OCLDEVICETOHOSTSTART, start[0]);
                PipelineTimeDescritor.getInstance().put(Stage.OCLDEVICETOHOSTSTOP, end[0]);
                break;
            default:
                break;
        }

        return (end[0] - start[0]);
    }

    static void oclTimer(cl_event[] events, Stage stage) throws CLException {
        OpenCLUtils.waitForEvents(events);
        long totalTime = 0;
        for (cl_event e : events) {
            totalTime += OpenCLUtils.getTimeEvent(stage, e);
        }
        PipelineTimeDescritor.getInstance().put(stage, totalTime);
    }

    static void oclSubmit(cl_event[] events, Stage stage) throws CLException {
        OpenCLUtils.waitForEvents(events);
        for (cl_event e : events) {
            long totalTime = OpenCLUtils.getTimeSubmit(e);
            PipelineTimeDescritor.getInstance().put(stage, totalTime);
        }
    }

    static void oclQueue(cl_event[] events, Stage stage) throws CLException {
        OpenCLUtils.waitForEvents(events);
        for (cl_event e : events) {
            long totalTime = OpenCLUtils.getTimeQueue(e);
            PipelineTimeDescritor.getInstance().put(stage, totalTime);
        }
    }

    static void oclTimer(cl_event event, Stage stage) throws CLException {
        long time = OpenCLUtils.getTimeEvent(stage, event);
        PipelineTimeDescritor.getInstance().put(stage, time);
    }

    static EventCallbackFunction makeCallBackFunction(ProfilerType metric, int idx, int deviceIndex) {
        return (e, t, d) -> {
            if (t == CL.CL_COMPLETE) {
                long[] queuedTime = new long[1];
                CL.clGetEventProfilingInfo(e, CL.CL_PROFILING_COMMAND_QUEUED, Sizeof.cl_ulong, Pointer.to(queuedTime), null);
                Profiler.getInstance().writeInBuffer(metric, "queued", queuedTime[0], deviceIndex);

                long[] submitTime = new long[1];
                CL.clGetEventProfilingInfo(e, CL.CL_PROFILING_COMMAND_SUBMIT, Sizeof.cl_ulong, Pointer.to(submitTime), null);
                Profiler.getInstance().writeInBuffer(metric, "submit", submitTime[0], deviceIndex);

                long[] startTime = new long[1];
                CL.clGetEventProfilingInfo(e, CL.CL_PROFILING_COMMAND_START, Sizeof.cl_ulong, Pointer.to(startTime), null);
                Profiler.getInstance().writeInBuffer(metric, "start", startTime[0], deviceIndex);

                long[] endTime = new long[1];
                CL.clGetEventProfilingInfo(e, CL.CL_PROFILING_COMMAND_END, Sizeof.cl_ulong, Pointer.to(endTime), null);
                Profiler.getInstance().writeInBuffer(metric, "end", endTime[0], deviceIndex);

                if (metric == ProfilerType.OCL_WRITE_BUFFER || metric == ProfilerType.OCL_WRITE_BUFFER_METADATA) {
                    Profiler.getInstance().putWrite(endTime[0] - startTime[0], idx, deviceIndex);
                } else if (metric == ProfilerType.OCL_READ_BUFFER) {
                    Profiler.getInstance().putRead(endTime[0] - startTime[0], idx, deviceIndex);
                } else if (metric == ProfilerType.OCL_KERNEL) {
                    Profiler.getInstance().putKernel(endTime[0] - startTime[0], idx, deviceIndex);
                }
            } else {
                System.out.println("Unable to print time of event");
            }
        };
    }

}
