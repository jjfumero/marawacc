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

import org.jocl.Pointer;
import org.jocl.cl_event;
import org.jocl.cl_mem;

public class StageInfo {

    private cl_mem clmem;
    private cl_event clevents;
    private cl_event kernelEvent;
    private Pointer pointer;

    public StageInfo(cl_mem clmem, cl_event clevents, Pointer pointers) {
        this.clmem = clmem;
        this.clevents = clevents;
        this.pointer = pointers;
    }

    public cl_event getOCLevent() {
        return clevents;
    }

    public void setOCLevent(cl_event clevents) {
        this.clevents = clevents;
    }

    public cl_mem getOCLmem() {
        return clmem;
    }

    public void setOCLmem(cl_mem clmem) {
        this.clmem = clmem;
    }

    public Pointer getPointer() {
        return this.pointer;
    }

    public void setKernelEvent(cl_event kernelEvent) {
        this.kernelEvent = kernelEvent;
    }

    public cl_event getKernelEvent() {
        return this.kernelEvent;
    }
}
