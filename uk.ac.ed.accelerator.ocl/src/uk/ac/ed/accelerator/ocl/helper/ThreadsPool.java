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

import java.util.concurrent.ForkJoinPool;

/**
 * Singleton to control the thread pool
 *
 */
public final class ThreadsPool {

    public static final int MAX_POOL_THREADS = 10;

    private static ThreadsPool instance = null;
    private ForkJoinPool executor = null;
    private int nThreads;
    private volatile int finished;

    public static ThreadsPool getInstance() {
        if (instance == null) {
            instance = new ThreadsPool();
        }
        return instance;
    }

    private ThreadsPool() {
        finished = 0;
    }

    public void setNumberOfThreads(int nThreads) {
        if (nThreads < MAX_POOL_THREADS) {
            this.nThreads = nThreads;
        } else {
            this.nThreads = MAX_POOL_THREADS;
        }
    }

    public void createExecutor() {
        if (executor == null && nThreads > 0) {
            executor = new ForkJoinPool(this.nThreads);
        }
    }

    public void executeRunnable(Runnable r) {
        // Asynchronous call
        executor.execute(r);
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            while (!executor.isTerminated()) {
                // empty
            }
        }
        executor = null;
    }

    public synchronized void finished() {
        finished++;
    }

    public synchronized boolean isFinished() {
        if (finished == nThreads) {
            return true;
        }
        return false;
    }
}
