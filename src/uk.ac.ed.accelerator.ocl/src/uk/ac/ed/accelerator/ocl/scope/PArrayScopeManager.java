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
package uk.ac.ed.accelerator.ocl.scope;

import java.util.HashSet;
import java.util.UUID;

public class PArrayScopeManager {

    private HashSet<UUID> table;
    private HashSet<UUID> tuples;

    public static final PArrayScopeManager INSTANCE = new PArrayScopeManager();

    private PArrayScopeManager() {
        table = new HashSet<>();
        tuples = new HashSet<>();
    }

    public void insertFunction(UUID uuid) {
        if (!table.contains(uuid)) {
            table.add(uuid);
        }
    }

    public void insertTuple(UUID uuid) {
        if (!tuples.contains(uuid)) {
            tuples.add(uuid);
        }
    }

    public boolean hasScope(UUID uuid) {
        return table.contains(uuid);
    }

    public boolean isTuple(UUID uuid) {
        return tuples.contains(uuid);
    }
}
