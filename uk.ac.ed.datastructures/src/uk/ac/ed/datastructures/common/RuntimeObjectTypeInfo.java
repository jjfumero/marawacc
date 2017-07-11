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

package uk.ac.ed.datastructures.common;

import uk.ac.ed.datastructures.interop.InteropTable;
import uk.ac.ed.datastructures.tuples.Tuple;
import uk.ac.ed.datastructures.tuples.Tuple1;
import uk.ac.ed.datastructures.tuples.Tuple10;
import uk.ac.ed.datastructures.tuples.Tuple11;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;
import uk.ac.ed.datastructures.tuples.Tuple4;
import uk.ac.ed.datastructures.tuples.Tuple5;
import uk.ac.ed.datastructures.tuples.Tuple6;
import uk.ac.ed.datastructures.tuples.Tuple7;
import uk.ac.ed.datastructures.tuples.Tuple8;
import uk.ac.ed.datastructures.tuples.Tuple9;

public class RuntimeObjectTypeInfo {

    private Class<?> klass;
    private InteropTable interop;
    private RuntimeObjectTypeInfo[] nestedTypes;

    // Tuples currently supported
    private static final Class<?>[] PREDEFINED_TUPLES = new Class[]{
                    Tuple1.class,
                    Tuple2.class,
                    Tuple3.class,
                    Tuple4.class,
                    Tuple5.class,
                    Tuple6.class,
                    Tuple7.class,
                    Tuple8.class,
                    Tuple9.class,
                    Tuple10.class,
                    Tuple11.class};

    public static RuntimeObjectTypeInfo inferFromObject(Object object) {
        Class<?> c = object.getClass();
        RuntimeObjectTypeInfo[] nestedTypes = null;

        if (object instanceof Tuple) {
            nestedTypes = ((Tuple) object).getType().getNestedTypes();
        }
        return new RuntimeObjectTypeInfo(c, nestedTypes);
    }

    public RuntimeObjectTypeInfo(Class<?> c) {
        this.klass = c;
        this.nestedTypes = null;
    }

    public RuntimeObjectTypeInfo(Class<?> c, InteropTable interop) {
        this.klass = c;
        this.interop = interop;
        this.nestedTypes = null;
    }

    public RuntimeObjectTypeInfo(Class<?> c, RuntimeObjectTypeInfo... nestedTypes) {
        this.klass = c;
        this.nestedTypes = nestedTypes;
    }

    public Class<?> getClassObject() {
        return klass;
    }

    public InteropTable getInterop() {
        return interop;
    }

    public void setInterop(InteropTable interop) {
        this.interop = interop;
    }

    public RuntimeObjectTypeInfo[] getNestedTypes() {
        return nestedTypes;
    }

    public RuntimeObjectTypeInfo[] getNestedTypesOrSelf() {
        if (nestedTypes == null) {
            return new RuntimeObjectTypeInfo[]{this};
        } else {
            return nestedTypes;
        }
    }

    public boolean isTupleType() {
        for (Class<?> c1 : PREDEFINED_TUPLES) {
            if (getClassObject() == c1) {
                return true;
            }
        }
        return false;
    }

    public boolean isScalarType() {
        return !isTupleType();
    }

    private int getOCLSize(int grade) {
        int sum = 0;
        for (int i = 0; i < grade; i++) {
            sum += nestedTypes[i].getOCLSize();
        }
        return sum;
    }

    private int getOCLSizeNoInterop() {
        if (getClassObject() == Float.class) {
            return JavaDataTypeSizes.FLOAT.getOCLSize();
        } else if (getClassObject() == Double.class) {
            return JavaDataTypeSizes.DOUBLE.getOCLSize();
        } else if (getClassObject() == Integer.class) {
            return JavaDataTypeSizes.INT.getOCLSize();
        } else if (getClassObject() == Short.class) {
            return JavaDataTypeSizes.SHORT.getOCLSize();
        } else if (getClassObject() == Long.class) {
            return JavaDataTypeSizes.LONG.getOCLSize();
        } else if (getClassObject() == Byte.class) {
            return JavaDataTypeSizes.BYTE.getOCLSize();
        } else if (getClassObject() == Character.class) {
            return JavaDataTypeSizes.CHAR.getOCLSize();
        } else if (getClassObject() == Tuple1.class) {
            return getOCLSize(1);
        } else if (getClassObject() == Tuple2.class) {
            return getOCLSize(2);
        } else if (getClassObject() == Tuple3.class) {
            return getOCLSize(3);
        } else if (getClassObject() == Tuple4.class) {
            return getOCLSize(4);
        } else if (getClassObject() == Tuple5.class) {
            return getOCLSize(5);
        } else if (getClassObject() == Tuple6.class) {
            return getOCLSize(6);
        } else if (getClassObject() == Tuple7.class) {
            return getOCLSize(7);
        } else if (getClassObject() == Tuple8.class) {
            return getOCLSize(8);
        } else if (getClassObject() == Tuple9.class) {
            return getOCLSize(9);
        } else if (getClassObject() == Tuple10.class) {
            return getOCLSize(10);
        } else if (getClassObject() == Tuple11.class) {
            return getOCLSize(11);
        } else {
            System.err.println("[Not Implemented yet] " + getClassObject().getName() + " ==> RuntimeObjectTypeInfo");
            throw new RuntimeException();
        }
    }

    private int getOCLSizeInterop() {
        if (getClassObject() == Float.class) {
            return JavaDataTypeSizes.FLOAT.getOCLSize();
        } else if (getClassObject() == Double.class) {
            return JavaDataTypeSizes.DOUBLE.getOCLSize();
        } else if (getClassObject() == Integer.class) {
            return JavaDataTypeSizes.INT.getOCLSize();
        } else if (getClassObject() == Short.class) {
            return JavaDataTypeSizes.SHORT.getOCLSize();
        } else if (getClassObject() == Long.class) {
            return JavaDataTypeSizes.LONG.getOCLSize();
        } else if (getClassObject() == Byte.class) {
            return JavaDataTypeSizes.BYTE.getOCLSize();
        } else if (getClassObject() == Character.class) {
            return JavaDataTypeSizes.CHAR.getOCLSize();
        } else if (getClassObject() == Tuple1.class) {
            return getOCLSize(1);
        } else if (getClassObject() == Tuple2.class) {
            return getOCLSize(2);
        } else if (getClassObject() == Tuple3.class) {
            return getOCLSize(3);
        } else if (getClassObject() == Tuple4.class) {
            return getOCLSize(4);
        } else if (getClassObject() == Tuple5.class) {
            return getOCLSize(5);
        } else if (getClassObject() == Tuple6.class) {
            return getOCLSize(6);
        } else if (getClassObject() == Tuple7.class) {
            return getOCLSize(7);
        } else if (getClassObject() == Tuple8.class) {
            return getOCLSize(8);
        } else if (getClassObject() == Tuple9.class) {
            return getOCLSize(9);
        } else if (getClassObject() == Tuple10.class) {
            return getOCLSize(10);
        } else if (getClassObject() == Tuple11.class) {
            return getOCLSize(11);
        } else {
            System.err.println("[Not Implemented yet] " + getClassObject().getName() + " ==> RuntimeObjectTypeInfo");
            throw new RuntimeException();
        }
    }

    public int getOCLSize() {
        if (interop == null) {
            return getOCLSizeNoInterop();
        } else {
            return getOCLSizeInterop();
        }
    }
}
