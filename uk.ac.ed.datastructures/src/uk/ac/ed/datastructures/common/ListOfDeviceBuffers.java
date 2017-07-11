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
package uk.ac.ed.datastructures.common;

import java.util.ArrayList;

public class ListOfDeviceBuffers {

    // What do I need?
    private ArrayList<AcceleratorBuffer> deviceBuffers;

    public ListOfDeviceBuffers() {
        this.deviceBuffers = new ArrayList<>();
    }

    public AcceleratorBuffer getAcceleratorBuffer(int idx) {
        return deviceBuffers.get(idx);
    }

    public AcceleratorBuffer getAcceleratorBuffer() {
        return deviceBuffers.get(0);
    }

    public void addAcceleratorBuffer(AcceleratorBuffer buffers) {
        deviceBuffers.add(buffers);
    }

    public int getTotalDevices() {
        return deviceBuffers.size();
    }

    public void clean() {
        for (AcceleratorBuffer buffers : deviceBuffers) {
            buffers.clean();
        }
        deviceBuffers.clear();
    }
}
