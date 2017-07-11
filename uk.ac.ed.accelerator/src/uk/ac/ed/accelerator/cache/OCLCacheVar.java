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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorVar;

public final class OCLCacheVar {

    private static OCLCacheVar instance = null;
    private HashMap<UUID, ArrayList<OCLGraalAcceleratorVar>> cacheVar;

    public static OCLCacheVar getInstance() {
        if (instance == null) {
            instance = new OCLCacheVar();
        }
        return instance;
    }

    private OCLCacheVar() {
        cacheVar = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public void insertArrayVars(UUID uuid, ArrayList<OCLGraalAcceleratorVar> vars) {
        if (!cacheVar.containsKey(uuid)) {
            ArrayList<OCLGraalAcceleratorVar> oclVars = (ArrayList<OCLGraalAcceleratorVar>) vars.clone();
            cacheVar.put(uuid, oclVars);
        }
    }

    public boolean isUUIDInCache(UUID uuid) {
        return cacheVar.containsKey(uuid);
    }

    public ArrayList<OCLGraalAcceleratorVar> getArrayList(UUID uuid) {
        if (cacheVar.containsKey(uuid)) {
            return cacheVar.get(uuid);
        }
        return null;
    }
}
