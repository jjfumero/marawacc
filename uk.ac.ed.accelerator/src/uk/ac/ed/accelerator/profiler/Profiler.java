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
package uk.ac.ed.accelerator.profiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;

public final class Profiler {

    private TreeMap<ProfilerType, LinkedList<Long>> tableTimers;
    private StringBuffer buffer;

    private StringBuffer[] logTimers;
    private Vector<Vector<Long>> writeTimers;
    private Vector<Vector<Long>> readTimers;
    private Vector<Vector<Long>> kernelTimers;

    private final int MAX_NUM_DEVICES = 4;

    private static Profiler instance = null;

    public static Profiler getInstance() {
        if (instance == null) {
            instance = new Profiler();
        }
        return instance;
    }

    private Profiler() {
        tableTimers = new TreeMap<>();
        logTimers = new StringBuffer[MAX_NUM_DEVICES];
        for (int i = 0; i < MAX_NUM_DEVICES; i++) {
            logTimers[i] = new StringBuffer();
        }
        initOCLLists();

    }

    public void clean() {
        buffer = new StringBuffer("");
        logTimers = null;
        writeTimers = null;
        readTimers = null;
        kernelTimers = null;
        tableTimers = null;
        System.gc();
        tableTimers = new TreeMap<>();
        initOCLLists();
        logTimers = new StringBuffer[MAX_NUM_DEVICES];
        for (int i = 0; i < MAX_NUM_DEVICES; i++) {
            logTimers[i] = new StringBuffer();
        }
    }

    public void put(ProfilerType metric, long time) {
        if (tableTimers.containsKey(metric)) {
            LinkedList<Long> timerList = tableTimers.get(metric);
            timerList.add(time);
            tableTimers.put(metric, timerList);
        } else {
            LinkedList<Long> timerList = new LinkedList<>();
            timerList.add(time);
            tableTimers.put(metric, timerList);
        }
    }

    public void putWrite(long time, int idx, int deviceIndex) {
        if ((writeTimers.get(deviceIndex).size() - 1) < idx) {
            writeTimers.get(deviceIndex).add(time);
        } else {
            writeTimers.get(deviceIndex).set(idx, writeTimers.get(deviceIndex).get(idx) + time);
        }
    }

    public void putASTWrite(long time, int idx, int deviceIndex) {
        if ((writeTimers.get(deviceIndex).size() - 1) < idx) {
            writeTimers.get(deviceIndex).add(time);
        } else {
            writeTimers.get(deviceIndex).set(idx, writeTimers.get(deviceIndex).get(idx) + time);
        }
    }

    public void putRead(long time, int idx, int deviceIndex) {
        if ((readTimers.get(deviceIndex).size() - 1) < idx) {
            readTimers.get(deviceIndex).add(time);
        } else {
            readTimers.get(deviceIndex).set(idx, readTimers.get(deviceIndex).get(idx) + time);
        }
    }

    public void putKernel(long time, int idx, int deviceIndex) {
        if ((kernelTimers.get(deviceIndex).size() - 1) < idx) {
            kernelTimers.get(deviceIndex).add(time);
        } else {
            kernelTimers.get(deviceIndex).set(idx, kernelTimers.get(deviceIndex).get(idx) + time);
        }
    }

    public void printWritesTimers(int deviceIndex) {
        for (Long t : writeTimers.get(deviceIndex)) {
            System.out.println("[WRITE]: " + t);
        }
    }

    public void printReadsTimers(int deviceIndex) {
        for (Long t : readTimers.get(deviceIndex)) {
            System.out.println("[READ]: " + t);
        }
    }

    public void printKernelTimers(int deviceIndex) {
        for (Long t : kernelTimers.get(deviceIndex)) {
            System.out.println("[Kernel]: " + t);
        }
    }

    private void initOCLLists() {
        writeTimers = new Vector<>();
        readTimers = new Vector<>();
        kernelTimers = new Vector<>();
        for (int i = 0; i < MAX_NUM_DEVICES; i++) {
            writeTimers.add(new Vector<Long>());
            readTimers.add(new Vector<Long>());
            kernelTimers.add(new Vector<Long>());
        }
    }

    public void printReportFullOCLTimers(int deviceIndex) {
        for (int i = 0; i < writeTimers.get(deviceIndex).size(); i++) {
            System.out.println("\n Iteration " + i);
            System.out.println("W : " + writeTimers.get(deviceIndex).get(i));
            System.out.println("X : " + kernelTimers.get(deviceIndex).get(i));
            System.out.println("R : " + readTimers.get(deviceIndex).get(i));
        }
    }

    public StringBuffer getReportFullOCLTimers(int deviceIndex) {
        StringBuffer fullReport = new StringBuffer();
        for (int i = 0; i < writeTimers.get(deviceIndex).size(); i++) {
            fullReport.append(("\n Iteration " + i));
            fullReport.append("W : " + writeTimers.get(deviceIndex).get(i));
            fullReport.append("X : " + kernelTimers.get(deviceIndex).get(i));
            fullReport.append("R : " + readTimers.get(deviceIndex).get(i));
        }
        return fullReport;
    }

    public void printMediansOCLEvents() {

        int maxDevs = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            maxDevs = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }

        for (int i = 0; i < maxDevs; i++) {
            printMediansOCLEvents(i);
        }
    }

    public void printMediansOCLEvents(int deviceIndex) {

        if (writeTimers.get(deviceIndex).size() > 0) {

            if (GraalAcceleratorOptions.reportOCLTimers) {
                printReportFullOCLTimers(deviceIndex);
            }

            long medianCopyIn = StatsUtils.computeMedian(writeTimers.get(deviceIndex));
            long medianCopyOut = StatsUtils.computeMedian(readTimers.get(deviceIndex));
            long medianKernel = StatsUtils.computeMedian(kernelTimers.get(deviceIndex));

            System.out.println("\nDevice: " + deviceIndex);
            System.out.println("Median KernelTime: " + medianKernel + " (ns)");
            System.out.println("Median CopyInTime: " + medianCopyIn + " (ns)");
            System.out.println("Median CopyOutTime: " + medianCopyOut + " (ns)");
        }
    }

    public boolean isContentInBuffers(int deviceIndex) {
        if (writeTimers.get(deviceIndex).size() > 0 && readTimers.get(deviceIndex).size() > 0 && kernelTimers.get(deviceIndex).size() > 0) {
            return true;
        }
        return false;
    }

    public StringBuffer getMediansOCLEvents() {

        int maxDevs = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            maxDevs = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }
        StringBuffer fullReport = new StringBuffer();
        for (int i = 0; i < maxDevs; i++) {
            fullReport.append(getMediansOCLEvents(i));
        }
        return fullReport;
    }

    public StringBuffer getMediansOCLEvents(int deviceIndex) {
        StringBuffer fullReport = new StringBuffer();

        if (isContentInBuffers(deviceIndex)) {

            if (GraalAcceleratorOptions.reportOCLTimers) {
                getReportFullOCLTimers(deviceIndex);
            }

            long medianCopyIn = StatsUtils.computeMedian(writeTimers.get(deviceIndex));
            long medianCopyOut = StatsUtils.computeMedian(readTimers.get(deviceIndex));
            long medianKernel = StatsUtils.computeMedian(kernelTimers.get(deviceIndex));

            fullReport.append("\nDevice: " + deviceIndex + "\n");
            fullReport.append("Median KernelTime: " + medianKernel + " (ns)\n");
            fullReport.append("Median CopyInTime: " + medianCopyIn + " (ns)\n");
            fullReport.append("Median CopyOutTime: " + medianCopyOut + " (ns)\n");
        }
        return fullReport;
    }

    public void writeInBuffer(ProfilerType metric, String desc, long time) {
        print(metric + " " + desc + " " + time, 0);
    }

    public void writeInBuffer(ProfilerType metric, String desc, long time, int deviceIndex) {
        print(metric + " " + desc + " " + time, deviceIndex);
    }

    public void writeInBuffer(String desc, long time, int deviceIndex) {
        print(desc + " " + time, deviceIndex);
    }

    public void writeInBuffer(String desc, long time) {
        print(desc + " " + time, 0);
    }

    public void print(String s, int deviceIndex) {
        logTimers[deviceIndex].append(s + "\n");
    }

    public void print(String s) {
        logTimers[0].append(s + "\n");
    }

    public void printLogBuffers() {
        int numDevs = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            numDevs = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }
        for (int i = 0; i < numDevs; i++) {
            if (logTimers[i].length() > 0) {
                System.out.println("Device: " + i);
                System.out.println(logTimers[i]);
            }
        }
    }

    public StringBuffer getLogBuffers() {
        int numDevs = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            numDevs = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }
        StringBuffer content = new StringBuffer();
        for (int i = 0; i < numDevs; i++) {
            if (logTimers[i].length() > 0) {
                content.append("\nDevice: " + i + "\n");
                content.append(logTimers[i]);
                content.append("\n");
            }
        }
        return content;
    }

    public StringBuffer getLogBuffers(int deviceIndex) {
        return logTimers[deviceIndex];
    }

    /**
     * Write plain text (log file) with all the raw metrics.
     *
     * @param sb
     * @return StringBuffer
     */
    public StringBuffer writeRawMetrics(StringBuffer sb) {
        for (ProfilerType type : tableTimers.keySet()) {
            List<Long> timers = tableTimers.get(type);
            for (Long time : timers) {
                sb.append(type.toString() + " time : " + time + "\n");
            }
        }
        return sb;
    }

    /**
     * Write into StringBuffer the csv table for all the raw metrics.
     *
     * @param sb
     * @return StringBuffer
     */
    public StringBuffer writeTableRawMetrics(StringBuffer sb) {
        for (ProfilerType type : tableTimers.keySet()) {
            sb.append(type.toString() + ",");
            List<Long> timers = tableTimers.get(type);
            for (Long time : timers) {
                sb.append(time + ",");
            }
            sb.append("\n");
        }
        return sb;
    }

    public StringBuffer writeMedianMetrics(StringBuffer sb) {
        for (ProfilerType type : tableTimers.keySet()) {
            LinkedList<Long> timers = tableTimers.get(type);
            long median = StatsUtils.computeMedian(timers);
            sb.append("Median " + type.toString() + " time : " + median + "\n");
        }
        return sb;
    }

    public StringBuffer writeAvarageMetrics(StringBuffer sb) {
        for (ProfilerType type : tableTimers.keySet()) {
            LinkedList<Long> timers = tableTimers.get(type);
            double avarage = StatsUtils.computeAvarage(timers);
            sb.append("Median " + type.toString() + " time : " + avarage + "\n");
        }
        return sb;
    }

    private static void writePlainOutputFile(String fileName, StringBuffer sb) {
        File file = new File(fileName);
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            System.out.println("");
            System.out.println("+-----------------------------------------------+");
            System.out.println("|             Profiler Information              |");
            System.out.println("+-----------------------------------------------+");
            System.out.println("| Time in nanoseconds                           |");
            System.out.println("+-----------------------------------------------+");
            System.out.println(sb.toString());
            output.write(sb.toString());
            System.out.println("+-----------------------------------------------+");
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeTableFile(String fileName, StringBuffer sb) {
        File file = new File(fileName);
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            System.out.println("");
            System.out.println("#-----------------------------------------------+");
            System.out.println("#             Profiler Information              |");
            System.out.println("#-----------------------------------------------+");
            System.out.println("# Time in nanoseconds                           |");
            System.out.println("#-----------------------------------------------+");
            System.out.println(sb.toString());
            output.write(sb.toString());
            System.out.println("#-----------------------------------------------+");
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StringBuffer writeMetricsIntoBuffer() {
        StringBuffer sb = new StringBuffer();
        sb = writeRawMetrics(sb);
        sb = writeMedianMetrics(sb);
        return sb;
    }

    public void writePlain(String fileName) {
        this.buffer = writeMetricsIntoBuffer();
        writePlainOutputFile(fileName, buffer);
    }

    public void writeTable(String fileName) {
        StringBuffer sb = new StringBuffer();
        sb = writeTableRawMetrics(sb);
        sb.append("# MEDIANS \n");
        sb = writeMedianMetrics(sb);
        this.buffer = sb;
        writeTableFile(fileName, buffer);
    }

    public StringBuffer getBuffer() {
        return buffer;
    }

}
