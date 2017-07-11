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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

// XXX: Change the name
public final class PipelineTimeDescritor {

    public enum Stage {

        // Main Stages in the pipeline
        MARSHAL("Marshall", 1),
        HOSTTODEVICE("HostToDevice", 2),
        EXECUTION("Execution", 3),
        DEVICETOHOST("DeviceToHost", 4),
        UNMARSHAL("Unmarshall", 5),

        // Internal OpenCL operations (they are
        // controlled by the event)
        OCLHOSTTODEVICE("OCL_HtDevice", 6),
        OCLEXECUTION("OCL_Execution", 7),
        OCLDEVICETOHOST("OCL_DtHost", 8),

        // Exclusive the Java Method to do the
        // OpenCL operation
        JAVAHTD("JavaSanity h->D", 9),
        JAVAEX("JavaOCL Execution", 10),
        JAVADTH("JavaSanity D->H", 11),

        // Main Stages in the pipeline (raw data)
        MARSHALRAW("MarshallRAW", 12),
        HOSTTODEVICERAW("HostToDeviceRAW", 13),
        EXECUTIONRAW("ExecutionRAW", 14),
        DEVICETOHOSTRAW("DeviceToHostRAW", 15),
        UNMARSHALRAW("UnmarshallRAW", 16),

        // OCL Start Stop events
        OCLHOSTTODEVICESTART("OpenCL_COPY_IN Start", 17),
        OCLHOSTTODEVICESTOP("OpenCL_COPY_IN Stop", 18),
        OCLEXECUTIONSTART("OpenCLExecution Start", 19),
        OCLEXECUTIONSTOP("OpenCLExecution Stop", 20),
        OCLDEVICETOHOSTSTART("OpenCL_COPY_OUT Start", 21),
        OCLDEVICETOHOSTSTOP("OpenCL_COPY_OUT Stop", 22),

        // Java Start-Stop timers
        MARSHALSTART("Marshall Start", 23),
        MARSHALSTOP("Marshall Stop", 24),
        HOSTTODEVICESTART("HostToDevice Start", 25),
        HOSTTODEVICESTOP("HostToDevice Stop", 26),
        EXECUTIONSTART("Execution Start", 27),
        EXECUTIONSTOP("Execution Stop", 28),
        DEVICETOHOSTART("DeviceToHost Start", 29),
        DEVICETOHOSTOP("DeviceToHost Stop", 30),
        UNMARSHALSTART("Unmarshall Start", 31),
        UNMARSHALSTOP("Unmarshall Stop", 32),

        // Additional
        // TEMPORALCOPYIN("Temporal Copy in", 33),
        TEMPORALPOINTER("Temporal PointerTo", 34),
        TEMPORALGLOBAL("Temporal Global", 35),

        // Additional timers for marshal and unmarshal analysis
        FROMJAVATOOCL("fromJavaToOCL", 36),
        PROCESSINPUT("processInput", 37),
        SERIALIZEOUTPUT("serializeOutput", 38),
        FROMOCLTOJAVA("fromOCLToJava", 39),
        PROCESSPARAMETERSIMPLE("processParametersSimpleDataType", 40),

        OCL_SUBMIT_WRITE("OCL_SUBMIT WRITE", 41),
        OCL_QUEUE_WRITE("OCL_QUEUE WRITE", 42),

        OCL_SUBMIT_READ("OCL_SUBMIT READ", 43),
        OCL_QUEUE_READ("OCL_QUEUE READ", 44),

        OCL_SUBMIT_KERNEL("OCL_SUBMIT KERNEL", 45),
        OCL_QUEUE_KERNEL("OCL_QUEUE KERNEL", 46),

        FINE_TUNE_COPY_OUT("FINE_TUNE COPY OUT", 47),
        FINE_TUNE_COPY_START("FINE_TUNE COPY START", 48),
        FINE_TUNE_COPY_STOP("FINE_TUNE COPY STOP", 49),

        // Call to Read
        C_FINE_SINGLETON("[CtoHOST] Singleton ", 50),
        C_FINE_START("[CtoHost] Start", 51),
        C_FINE_STOP("[CtoHost] Stop", 52),
        C_FINE_START_CALL_TO_READ("[CtoHost] Call_To_Read Start:  ", 53),
        C_FINE_START_JUST_READING("[CtoHOST] READING start: ", 54);

        private Stage(final String text, int index) {
            this.text = text;
            this.index = index;
        }

        private final String text;
        private final int index;

        @Override
        public String toString() {
            return text;
        }

        public int index() {
            return index;
        }
    }

    private static PipelineTimeDescritor instance = null;
    private TreeMap<Stage, List<Long>> desc;
    private TreeMap<Stage, Long> median;
    private Set<Stage> nonMedians;

    public static synchronized PipelineTimeDescritor getInstance() {
        if (instance == null) {
            instance = new PipelineTimeDescritor();
        }
        return instance;
    }

    private PipelineTimeDescritor() {
        desc = new TreeMap<>();
        nonMedians = new HashSet<>();
        inicializeNonMedians();
    }

    private void inicializeNonMedians() {
        // OpenCL Start-Stop
        nonMedians.add(Stage.OCLHOSTTODEVICESTART);
        nonMedians.add(Stage.OCLHOSTTODEVICESTOP);
        nonMedians.add(Stage.OCLEXECUTIONSTART);
        nonMedians.add(Stage.OCLEXECUTIONSTOP);
        nonMedians.add(Stage.OCLDEVICETOHOSTSTART);
        nonMedians.add(Stage.OCLDEVICETOHOSTSTOP);

        // Java Start-Stop
        nonMedians.add(Stage.MARSHALSTART);
        nonMedians.add(Stage.MARSHALSTOP);
        nonMedians.add(Stage.HOSTTODEVICESTART);
        nonMedians.add(Stage.HOSTTODEVICESTOP);
        nonMedians.add(Stage.EXECUTIONSTART);
        nonMedians.add(Stage.EXECUTIONSTOP);
        nonMedians.add(Stage.DEVICETOHOSTART);
        nonMedians.add(Stage.DEVICETOHOSTOP);
        nonMedians.add(Stage.UNMARSHALSTART);
        nonMedians.add(Stage.UNMARSHALSTOP);

    }

    public void put(Stage description, Long time) {
        // Time is in nanoseconds
        List<Long> timerList;
        if (!desc.containsKey(description)) {
            timerList = new LinkedList<>();
        } else {
            timerList = desc.get(description);
        }
        timerList.add(time);
        desc.put(description, timerList);
    }

    public void printTimers() {
        for (Stage event : desc.keySet()) {
            for (Long time : desc.get(event)) {
                System.out.println(event.toString() + "\t:" + time);
            }
        }
    }

    public void printMedians() {
        computeMedian();
        for (Stage event : median.keySet()) {
            System.out.println("Median time of " + event.toString() + ": " + median.get(event));
        }
    }

    public void printRaw() {
        for (Stage event : desc.keySet()) {
            List<Long> timers = desc.get(event);

            for (Long l : timers) {
                System.out.println("Raw " + event.toString() + " : " + l);
            }
        }
    }

    public void computeMedian() {

        // Compute ahead of computation time
        if (median == null) {
            median = new TreeMap<>();
        }

        // printRaw();

        for (Stage event : desc.keySet()) {

            // Do not compute medians if the event is in the set nonMedians
            if (nonMedians.contains(event)) {
                continue;
            }

            List<Long> timers = desc.get(event);

            Collections.sort(timers);
            int middle = timers.size() / 2;
            long medianEvent = 0;
            if (timers.size() % 2 == 1) {
                medianEvent = timers.get(middle);
            } else {
                medianEvent = (timers.get(middle - 1) + timers.get(middle)) / 2;
            }
            median.put(event, medianEvent);
        }
    }

    public void clear() {
        desc.clear();
        median.clear();
    }
}
