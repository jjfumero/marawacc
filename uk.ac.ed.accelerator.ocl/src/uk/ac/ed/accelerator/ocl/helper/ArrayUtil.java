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

import java.lang.reflect.Array;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ed.accelerator.ocl.RuntimeSymbolTable;

public class ArrayUtil {

    public static Object flattenArray(Object srcArray, boolean isReadOnly) {
        if (srcArray == null || !srcArray.getClass().isArray()) {
            return null;
        }
        int arraySize = ArrayUtil.getArraySize(srcArray);
        RuntimeSymbolTable runtimest = RuntimeSymbolTable.getInstance();
        if ((getNumArrayDimensions(srcArray) == 1) && (runtimest.isObjectInRuntimeSymbolTable(srcArray))) {
            return srcArray;
        }
        Object flatArray = TypeUtil.newArray(srcArray, arraySize);
        if (!isReadOnly) {
            return flatArray;
        } else {
            flattenArray(srcArray, flatArray, new int[]{0});
            return flatArray;
        }
    }

    private static void flattenArray(Object srcArray, Object destArray, int[] index) {
        if (srcArray != null) {
            int length = Array.getLength(srcArray);
            for (int i = 0; i < length; i++) {
                Object elem = Array.get(srcArray, i);
                if (elem != null) {
                    if (elem.getClass().isArray()) {
                        flattenArray(elem, destArray, index);
                    } else {
                        // Workaround for boxed types, for example array indices
                        if (i == 0 && srcArray.getClass().equals(destArray.getClass())) {
                            // srcArray is one-dimensional, use System.arraycopy
                            System.arraycopy(srcArray, 0, destArray, index[0], length);
                            index[0] += length;
                            break;
                        }
                        Array.set(destArray, index[0]++, elem);
                    }
                }
            }
        }
    }

    public static int getArraySize(Object a) {
        if (a == null) {
            return 0;
        }
        if (a.getClass().isArray()) {
            int count = 0;
            for (int i = 0; i < Array.getLength(a); i++) {
                int size = getArraySize(Array.get(a, i));
                if (size == -1) {
                    return Array.getLength(a);
                } else {
                    count += size;
                }
            }

            return count;
        } else {
            return -1;
        }
    }

    public static void rebuildToJavaParam(Object srcArray, Object destArray, int[] index) {
        RuntimeSymbolTable runtimest = RuntimeSymbolTable.getInstance();
        if (!((getNumArrayDimensions(srcArray) == 1) && (runtimest.isObjectInRuntimeSymbolTable(srcArray)))) {
            rebuild(srcArray, destArray, index);
        }
        runtimest.clear();
    }

    public static void rebuild(Object srcArray, Object destArray, int[] index) {
        if (srcArray != null) {
            int length = Array.getLength(destArray);
            for (int i = 0; i < length; i++) {
                Object elem = Array.get(destArray, i);
                if (elem != null) {
                    if (elem.getClass().isArray()) {
                        rebuild(srcArray, elem, index);
                    } else {
                        // Workaround for boxed types, for example array indices
                        if (i == 0 && srcArray.getClass().equals(destArray.getClass())) {
                            // destArray is one-dimensional, use System.arraycopy
                            System.arraycopy(srcArray, index[0], destArray, i, length);
                            index[0] += length;
                            break;
                        }
                        Array.set(destArray, i, Array.get(srcArray, index[0]++));
                    }
                }
            }
        }
    }

    public static int getNumArrayDimensions(Object a) {
        if (a.getClass().isArray()) {
            Pattern pattern = Pattern.compile("^\\[+");
            Matcher matcher = pattern.matcher(a.getClass().getName());
            int dim = 0;
            while (matcher.find()) {
                dim = matcher.group().length();
            }
            return dim;
        }
        return 0;
    }
}
