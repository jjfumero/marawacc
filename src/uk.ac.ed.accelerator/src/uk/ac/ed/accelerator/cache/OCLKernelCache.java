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

public final class OCLKernelCache {

    private static OCLKernelCache instance = null;
    private HashMap<UUID, ArrayList<OCLKernelPackage>> cache;

    public static OCLKernelCache getInstance() {
        if (instance == null) {
            instance = new OCLKernelCache();
        }
        return instance;
    }

    private OCLKernelCache() {
        cache = new HashMap<>();
    }

    public void insert(UUID uuid, int cacheIndex, OCLKernelPackage object) {
        if (cache.containsKey(uuid)) {
            ArrayList<OCLKernelPackage> arrayList = cache.get(uuid);
            if ((arrayList.size() - 1) < cacheIndex) {
                int start = arrayList.size() - 1;
                for (int i = start; i <= cacheIndex; i++) {
                    arrayList.add(new OCLKernelPackage());
                }
            }
            arrayList.set(cacheIndex, object);
            cache.put(uuid, arrayList);
        } else {
            ArrayList<OCLKernelPackage> kernel = new ArrayList<>();
            if (cacheIndex == 0) {
                kernel.add(object);
            } else {
                for (int i = 0; i <= cacheIndex; i++) {
                    kernel.add(new OCLKernelPackage());
                }
                kernel.set(cacheIndex, object);
            }
            cache.put(uuid, kernel);
        }
    }

    public OCLKernelPackage get(UUID uuid, int idxCache) {
        try {
            OCLKernelPackage oclKernelPackage = cache.get(uuid).get(idxCache);
            return oclKernelPackage;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isInCache(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public void clean() {
        cache.clear();
    }
}
