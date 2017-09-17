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
package uk.ac.ed.accelerator.math.ocl;

/**
 * OCLMath - Class to provide math operations in OpenCL. The precision in Java and OpenCL is
 * different. By using this class, the code generator knows how to generate the OpenCL code and the
 * user is aware of the precision.
 *
 */
public final class OCLMath {

    private OCLMath() {
    }

    public static float fabs(float a) {
        return Math.abs(a);
    }

    public static double fabs(double a) {
        return Math.abs(a);
    }

    public static float log(float a) {
        return (float) Math.log(a);
    }

    public static double log(double a) {
        return Math.log(a);
    }

    public static float sqrt(float a) {
        return (float) Math.sqrt(a);
    }

    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    public static float exp(float a) {
        return (float) Math.exp(a);
    }

    public static double exp(double a) {
        return Math.exp(a);
    }

    public static float pow2(float a) {
        return (float) Math.pow(a, 2);
    }

    public static double pow2(double a) {
        return Math.pow(a, 2.0);
    }

    public static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    public static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    public static float hypot(float a, float b) {
        return (float) Math.hypot(a, b);
    }

    public static double hypot(double a, double b) {
        return Math.hypot(a, b);
    }

}
