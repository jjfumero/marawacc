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

package uk.ac.ed.accelerator.ocl;

import java.util.HashMap;

public final class TimeDescriptor {

    public enum Time {
        OCL_KERNEL_EXECUTION("OCLKernel Execution Time", 1),
        COPY_IN("Copy IN", 2),
        COPY_OUT("Copy OUT", 3),
        UNMARSHALL_TIME("UnMarshall Time", 4),
        KERNEL_GENERATION_TIME("Kernel Generation Time", 5),
        BUILD_TIME("Build Time", 6),
        OVERALL_TIME("Overall Time", 7),
        MARSHALLING_TIME("Marshalling Time", 8),
        SEQUENTIAL_TIME("Sequential Time", 9),
        MAP_CPU_TIME("Map CPU Time", 10),
        TUPLE_INPUT("Tuple Input", 11),
        SERIAL_OUTPUT("Serial Output", 12),
        NAIVE_KERNEL_TIME("Naive Kernel Time", 13),
        NAIVE_TOTAL_TIME("Naive Total Time", 14),
        JAVA_ARRAYS_CPU("Java Arrays", 15),
        TUPLES_BUFFER("Tuples Buffer", 16),
        TUPLES_BUFFER2("Tuples Buffer2", 17),
        TOTAL_TIME("Total Time", 18),
        KERNEL_EXECUTION("Kernel Execution Time", 19);

        private Time(final String text, int index) {
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

    private static TimeDescriptor instance = null;
    private HashMap<Time, Long> desc;

    public static TimeDescriptor getInstance() {
        if (instance == null) {
            instance = new TimeDescriptor();
        }
        return instance;
    }

    private TimeDescriptor() {
        desc = new HashMap<>();
    }

    public void put(Time description, Long time) {
        desc.put(description, time);
    }

    public void printComparison() {
        for (Time event : desc.keySet()) {
            System.out.println(event.toString() + "\t:" + desc.get(event));
        }
    }

    public HashMap<Time, Long> getDescriptor() {
        return this.desc;
    }

    public void clear() {
        desc.clear();
    }
}
