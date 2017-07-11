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
package uk.ac.ed.accelerator.cache;

import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import org.jocl.cl_mem;

public final class OCLBufferMemoryCache {

    // Data cache
    private static HashMap<UUID, Vector<cl_mem[]>> buffersCache;

    public static void put(UUID uuidData, Vector<cl_mem[]> buffers) {
        if (buffersCache == null) {
            buffersCache = new HashMap<>();
        }
        buffersCache.put(uuidData, buffers);
    }

    public static Vector<cl_mem[]> get(UUID uuidData) {

        if (buffersCache == null) {
            return null;
        }

        if (buffersCache.containsKey(uuidData)) {
            return buffersCache.get(uuidData);
        }
        return null;
    }

    public static boolean isInCache(UUID uuidData) {
        if (buffersCache == null) {
            return false;
        }
        return buffersCache.containsKey(uuidData);
    }

    public static cl_mem[] getOCLListMemory(UUID uuidData, int idx) {
        Vector<cl_mem[]> vector = get(uuidData);
        if (vector != null) {
            return vector.get(idx);
        } else {
            return null;
        }
    }

    private OCLBufferMemoryCache() {
        // No public instance
    }

}
