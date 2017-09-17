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

import java.util.concurrent.RecursiveTask;

public class ParallelCopyTask<T> extends RecursiveTask<T> {

    private static final long serialVersionUID = 1L;
    private Object object;
    private Object array;
    private int from;
    private int to;

    public ParallelCopyTask(Object object, int from, int to, Object array) {
        this.object = object;
        this.from = from;
        this.to = to;
        this.array = array;
    }

    @Override
    protected T compute() {
        System.out.println("Running thread: " + Thread.currentThread().getName());
        if (object.getClass().getName().contains("F")) {
            int j = 0;
            for (int i = from; i < to; i++) {
                ((float[]) array)[j] = ((Float[]) object)[i];
                j++;
            }
        }
        // Write in the singleton
        return null;
    }
}
