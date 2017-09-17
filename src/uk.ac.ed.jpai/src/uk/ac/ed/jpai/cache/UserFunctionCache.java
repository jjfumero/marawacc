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
package uk.ac.ed.jpai.cache;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Insert into cache the Function or the BiFunction. It creates an UUID for each function. This UUID
 * is the unique number passed to the Graal-OpenCL runtime, which is used for cache code generation
 * and OpenCL binaries.
 *
 */
public final class UserFunctionCache {

    public static final UserFunctionCache INSTANCE = new UserFunctionCache();
    private static HashMap<String, UUID> cache;
    private static HashMap<BiFunction<?, ?, ?>, UUID> cachebi;

    private static UUID lastUUID;

    private UserFunctionCache() {
        cache = new HashMap<>();
        cachebi = new HashMap<>();
    }

    @SuppressWarnings("static-method")
    public UUID insertFunction(Function<?, ?> function) {
        if (!cache.containsKey(function.getClass().getName())) {
            lastUUID = UUID.randomUUID();
            cache.put(function.getClass().getName(), lastUUID);
            return lastUUID;
        }
        return cache.get(function.getClass().getName());
    }

    @SuppressWarnings("static-method")
    public <T, R> void insertFunction(BiFunction<R, R, R> f) {
        if (!cachebi.containsKey(f)) {
            lastUUID = UUID.randomUUID();
            cachebi.put(f, lastUUID);
        }
    }

    @SuppressWarnings("static-method")
    public <T, R> void insertBiFunction(BiFunction<T, R, R> f) {
        if (!cachebi.containsKey(f)) {
            lastUUID = UUID.randomUUID();
            cachebi.put(f, lastUUID);
        }
    }

    @SuppressWarnings("static-method")
    public <T, R> boolean isFunction(Function<T, R> function) {
        return cache.containsKey(function.getClass().getName());
    }

    @SuppressWarnings("static-method")
    public <T, R> boolean isFunction(BiFunction<R, R, R> biFunction) {
        return cachebi.containsKey(biFunction.getClass().getName());
    }

    @SuppressWarnings("static-method")
    public UUID getLastUUID() {
        return lastUUID;
    }

    @SuppressWarnings("static-method")
    public <T, R> UUID getUUID(Function<T, R> f) {
        String functionName = f.getClass().getName();
        if (cache.containsKey(functionName)) {
            return cache.get(functionName);
        }
        return null;
    }
}
