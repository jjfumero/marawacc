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

import org.jocl.Sizeof;

/**
 * Data type controlling and management for Graal-JOCL communication.
 *
 */
public class TypeUtil {

    public static int getTypeSizeForCL(Class<?> klass) {
        if (klass == Integer.class) {
            return Sizeof.cl_int;
        } else if (klass == Float.class) {
            return Sizeof.cl_float;
        } else if (klass == Long.class) {
            return Sizeof.cl_long;
        } else if (klass == Double.class) {
            return Sizeof.cl_double;
        } else if (klass == Byte.class) {
            return Sizeof.cl_char;
        } else if (klass == Short.class) {
            return Sizeof.cl_short;
        } else if (klass == Boolean.class) {
            return Sizeof.cl_char;
        } else if (klass == Character.class) {
            return Sizeof.cl_char;
        }
        return -1;
    }

    public static int getTypeSizeForCL(Object obj) {
        if (obj.getClass().equals(Integer.class)) {
            return Sizeof.cl_int;
        } else if (obj.getClass().getName().contains("I")) {
            return Sizeof.cl_int;
        } else if (obj.getClass().getName().contains("F")) {
            return Sizeof.cl_float;
        } else if (obj.getClass().getName().contains("C")) {
            return Sizeof.cl_char;
        } else if (obj.getClass().getName().contains("J")) {
            return Sizeof.cl_long;
        } else if (obj.getClass().getName().contains("S")) {
            return Sizeof.cl_short;
        } else if (obj.getClass().getName().contains("B")) { // byte
            return Sizeof.cl_char;
        } else if (obj.getClass().getName().contains("Z")) { // boolean
            return Sizeof.cl_char;
        } else if (obj.getClass().getName().contains("D")) {
            return Sizeof.cl_double;
        }
        return -1;
    }

    public static Object newArray(Object obj, int size) {
        if (obj.getClass().equals(Integer.class)) {
            return new int[size];
        } else if (obj.getClass().getName().contains("I")) {
            return new int[size];
        } else if (obj.getClass().getName().contains("F")) {
            return new float[size];
        } else if (obj.getClass().getName().contains("C")) {
            return new char[size];
        } else if (obj.getClass().getName().contains("J")) {
            return new long[size];
        } else if (obj.getClass().getName().contains("S")) {
            return new short[size];
        } else if (obj.getClass().getName().contains("B")) { // byte
            return new byte[size];
        } else if (obj.getClass().getName().contains("Z")) { // boolean
            return new boolean[size];
        } else if (obj.getClass().getName().contains("D")) {
            return new double[size];
        }
        return null;
    }

    public static Object newArrayCopy(Object obj, int size) {
        if (obj.getClass().equals(Integer.class)) {
            return new int[size];
        } else if (obj.getClass().getName().contains("I")) {
            int[] newArray = new int[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        } else if (obj.getClass().getName().contains("F")) {
            float[] newArray = new float[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        } else if (obj.getClass().getName().contains("C")) {
            char[] newArray = new char[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        } else if (obj.getClass().getName().contains("J")) {
            long[] newArray = new long[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        } else if (obj.getClass().getName().contains("S")) {
            short[] newArray = new short[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        } else if (obj.getClass().getName().contains("B")) { // byte
            byte[] newArray = new byte[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        } else if (obj.getClass().getName().contains("Z")) { // boolean
            boolean[] newArray = new boolean[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        } else if (obj.getClass().getName().contains("D")) {
            double[] newArray = new double[size];
            System.arraycopy(obj, size, newArray, 0, size);
            return newArray;
        }
        return null;
    }

    public static boolean isPrimative(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj.getClass().equals(Integer.class)) {
            return true;
        } else if (obj.getClass().equals(Float.class)) {
            return true;
        } else if (obj.getClass().equals(Character.class)) {
            return true;
        } else if (obj.getClass().equals(Long.class)) {
            return true;
        } else if (obj.getClass().equals(Short.class)) {
            return true;
        } else if (obj.getClass().equals(Byte.class)) {
            return true;
        } else if (obj.getClass().equals(Boolean.class)) {
            return true;
        } else if (obj.getClass().equals(Double.class)) {
            return true;
        }
        return false;
    }

    public static Object getPrimativeForCL(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj.getClass().equals(Integer.class)) {
            return new int[]{(int) obj};
        } else if (obj.getClass().equals(Float.class)) {
            return new float[]{(float) obj};
        } else if (obj.getClass().equals(Character.class)) {
            return new char[]{(char) obj};
        } else if (obj.getClass().equals(Long.class)) {
            return new long[]{(long) obj};
        } else if (obj.getClass().equals(Short.class)) {
            return new short[]{(short) obj};
        } else if (obj.getClass().equals(Byte.class)) {
            return new byte[]{(byte) obj};
        } else if (obj.getClass().equals(Boolean.class)) {
            return new boolean[]{(boolean) obj};
        } else if (obj.getClass().equals(Double.class)) {
            return new double[]{(double) obj};
        }
        return null;
    }

    public static String getTypeFromArrayNonPrimitive(Class<?> klass) {
        if (klass.getComponentType().equals(Integer.class)) {
            return "[I";
        } else if (klass.getComponentType().equals(Float.class)) {
            return "[F";
        } else if (klass.getComponentType().equals(Long.class)) {
            return "[J";
        } else if (klass.getComponentType().equals(Double.class)) {
            return "[D";
        } else if (klass.getComponentType().equals(Byte.class)) {
            return "[B";
        } else if (klass.getComponentType().equals(Short.class)) {
            return "[S";
        } else if (klass.getComponentType().equals(Boolean.class)) {
            return "[Z";
        } else if (klass.getComponentType().equals(Character.class)) {
            return "[C";
        }
        return null;
    }

    public static String getTypeFromArrayNonPrimitiveClass(Class<?> klass) {
        if (klass == Integer.class) {
            return "[I";
        } else if (klass == Float.class) {
            return "[F";
        } else if (klass == Long.class) {
            return "[J";
        } else if (klass == Double.class) {
            return "[D";
        } else if (klass == Byte.class) {
            return "[B";
        } else if (klass == Short.class) {
            return "[S";
        } else if (klass == Boolean.class) {
            return "[Z";
        } else if (klass == Character.class) {
            return "[C";
        }
        return null;
    }

    public static String getTypeFromArray2DNonPrimitive(Class<?> klass) {
        if (klass.getComponentType().equals(Integer[].class)) {
            return "[[I";
        } else if (klass.getComponentType().equals(Float[].class)) {
            return "[[F";
        } else if (klass.getComponentType().equals(Long[].class)) {
            return "[[J";
        } else if (klass.getComponentType().equals(Double[].class)) {
            return "[[D";
        } else if (klass.getComponentType().equals(Byte[].class)) {
            return "[[B";
        } else if (klass.getComponentType().equals(Short[].class)) {
            return "[[S";
        } else if (klass.getComponentType().equals(Boolean[].class)) {
            return "[[Z";
        } else if (klass.getComponentType().equals(Character[].class)) {
            return "[[C";
        } else if (klass.getComponentType().equals(int[].class)) {
            return "[[I";
        } else if (klass.getComponentType().equals(float[].class)) {
            return "[[F";
        } else if (klass.getComponentType().equals(long[].class)) {
            return "[[J";
        } else if (klass.getComponentType().equals(double[].class)) {
            return "[[D";
        } else if (klass.getComponentType().equals(byte[].class)) {
            return "[[B";
        } else if (klass.getComponentType().equals(short[].class)) {
            return "[[S";
        } else if (klass.getComponentType().equals(boolean[].class)) {
            return "[[Z";
        } else if (klass.getComponentType().equals(char[].class)) {
            return "[[C";
        }
        return null;
    }

    public static String getTypeFromArrayNonPrimitiveObject(Class<?> klass) {
        if (klass.getComponentType().equals(Integer.class)) {
            return "int";
        } else if (klass.getComponentType().equals(Float.class)) {
            return "float";
        } else if (klass.getComponentType().equals(Long.class)) {
            return "long";
        } else if (klass.getComponentType().equals(Double.class)) {
            return "double";
        } else if (klass.getComponentType().equals(Byte.class)) {
            return "byte";
        } else if (klass.getComponentType().equals(Short.class)) {
            return "short";
        } else if (klass.getComponentType().equals(Boolean.class)) {
            return "boolean";
        } else if (klass.getComponentType().equals(Character.class)) {
            return "character";
        }
        return null;
    }

}
