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

/**
 * Small utility to debug OpenCL component.
 */
public class LoggerMarawacc {

    public static void warning(String message) {
        System.out.println(Thread.currentThread().getStackTrace()[2].getFileName() + " :: " + Thread.currentThread().getStackTrace()[2].getLineNumber() + " ## " + message);
    }

    public static void info(String message) {
        System.out.println(Thread.currentThread().getStackTrace()[2].getFileName() + " :: " + Thread.currentThread().getStackTrace()[2].getLineNumber() + " ## " + message);
    }
}
