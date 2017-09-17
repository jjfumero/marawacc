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

package uk.ac.ed.accelerator.ocl.helper;

public class Timer {

    private long startTime;
    private long endTime;

    public void start() {
        startTime = 0;
        startTime = System.nanoTime();
    }

    public void end() {
        endTime = System.nanoTime();
    }

    public long getTotalTimeNanoseconds() {
        return this.endTime - this.startTime;
    }

    public double getTotalTimeSeconds() {
        return (this.endTime - this.startTime) / 1_000_000_000;
    }
}
