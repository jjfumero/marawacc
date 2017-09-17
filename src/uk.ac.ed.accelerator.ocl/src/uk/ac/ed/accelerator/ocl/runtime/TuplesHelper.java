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

package uk.ac.ed.accelerator.ocl.runtime;

import java.lang.reflect.Array;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType.DataType;
import uk.ac.ed.datastructures.tuples.Tuple1;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;
import uk.ac.ed.datastructures.tuples.Tuple4;
import uk.ac.ed.datastructures.tuples.Tuple5;
import uk.ac.ed.datastructures.tuples.Tuple6;
import uk.ac.ed.datastructures.tuples.Tuple7;
import uk.ac.ed.datastructures.tuples.Tuple8;
import uk.ac.ed.datastructures.tuples.Tuple9;

/**
 * Class in order to get the data type of each field in the Tuple Class. This is useful information
 * for the OpenCL code generator. The visitor is much simpler if this information is pre-computed
 * just before the OpenCL code generator invocation.
 *
 */
public class TuplesHelper {

    private static DataType getDataTypeArray(String str) {
        if (str.equals("byte")) {
            return DataType.BYTE;
        }
        if (str.equals("float")) {
            return DataType.FLOAT;
        }
        if (str.equals("double")) {
            return DataType.DOUBLE;
        }
        if (str.equals("int")) {
            return DataType.INTEGER;
        }
        if (str.equals("long")) {
            return DataType.LONG;
        }
        if (str.equals("boolean")) {
            return DataType.BOOLEAN;
        }
        if (str.equals("short")) {
            return DataType.SHORT;
        }
        if (str.equals("char")) {
            return DataType.CHAR;
        }
        if (str.equals("tuple2")) {
            return DataType.TUPLE2;
        }
        if (str.equals("tuple3")) {
            return DataType.TUPLE3;
        }
        if (str.equals("tuple4")) {
            return DataType.TUPLE4;
        }
        if (str.equals("tuple5")) {
            return DataType.TUPLE5;
        }
        if (str.equals("tuple6")) {
            return DataType.TUPLE6;
        }
        if (str.equals("tuple7")) {
            return DataType.TUPLE7;
        }
        if (str.equals("tuple8")) {
            return DataType.TUPLE8;
        }
        if (str.equals("tuple9")) {
            return DataType.TUPLE9;
        }
        return null;
    }

    private static String getTypeArray(Object object) {
        Class<? extends Object> clazz = object.getClass();
        if (clazz.isArray()) {

            while (clazz.isArray()) {
                clazz = clazz.getComponentType();
            }

            if (clazz.isPrimitive()) {
                return clazz.getName();
            }

            Pattern pattern = Pattern.compile("\\.\\w+");
            Matcher matcher = pattern.matcher(clazz.getName());
            String str = null;
            while (matcher.find()) {
                str = matcher.group().substring(1);
            }
            return str;
        }
        return null;
    }

    private static int getNumArrayDimensions(Object array) {
        int count = 0;
        Class<?> arrayClass = array.getClass();
        while (arrayClass.isArray()) {
            count++;
            arrayClass = arrayClass.getComponentType();
        }
        return count;
    }

    private static int getArraySize(Object a) {
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

    public static AcceleratorType getElementDataTypeClass(Class<?> klass) throws Exception {
        if (klass == Float.class) {
            return new AcceleratorType(DataType.FLOAT);
        } else if (klass == Integer.class) {
            return new AcceleratorType(DataType.INTEGER);
        } else if (klass == Double.class) {
            return new AcceleratorType(DataType.DOUBLE);
        } else if (klass == Byte.class) {
            return new AcceleratorType(DataType.BYTE);
        } else if (klass == Long.class) {
            return new AcceleratorType(DataType.LONG);
        } else if (klass == Boolean.class) {
            return new AcceleratorType(DataType.BOOLEAN);
        } else if (klass == Character.class) {
            return new AcceleratorType(DataType.CHAR);
        } else if (klass == Short.class) {
            return new AcceleratorType(DataType.SHORT);
        } else if (klass == Tuple2.class) {
            return new AcceleratorType(DataType.TUPLE2);
        } else if (klass == Tuple3.class) {
            return new AcceleratorType(DataType.TUPLE3);
        } else if (klass == Tuple4.class) {
            return new AcceleratorType(DataType.TUPLE4);
        } else if (klass == Tuple5.class) {
            return new AcceleratorType(DataType.TUPLE5);
        } else if (klass == Tuple6.class) {
            return new AcceleratorType(DataType.TUPLE6);
        } else if (klass == Tuple7.class) {
            return new AcceleratorType(DataType.TUPLE7);
        } else if (klass == Tuple8.class) {
            return new AcceleratorType(DataType.TUPLE8);
        } else if (klass == Tuple9.class) {
            return new AcceleratorType(DataType.TUPLE9);
        }
        throw new Exception(klass + " data type not supported yet");
    }

    public static AcceleratorType getElementDataType(Object object) {
        if (object == null) {
            return new AcceleratorType(DataType.FLOAT);
        } else if (object instanceof Byte) {
            return new AcceleratorType(DataType.BYTE);
        } else if (object instanceof Float) {
            return new AcceleratorType(DataType.FLOAT);
        } else if (object instanceof Double) {
            return new AcceleratorType(DataType.DOUBLE);
        } else if (object instanceof Integer) {
            return new AcceleratorType(DataType.INTEGER);
        } else if (object instanceof Long) {
            return new AcceleratorType(DataType.LONG);
        } else if (object instanceof Boolean) {
            return new AcceleratorType(DataType.BOOLEAN);
        } else if (object instanceof Short) {
            return new AcceleratorType(DataType.SHORT);
        } else if (object instanceof Character) {
            return new AcceleratorType(DataType.CHAR);
        } else if (object instanceof Tuple1) {
            return new AcceleratorType(DataType.TUPLE1);
        } else if (object instanceof Tuple2) {
            return new AcceleratorType(DataType.TUPLE2);
        } else if (object instanceof Tuple3) {
            return new AcceleratorType(DataType.TUPLE3);
        } else if (object instanceof Tuple4) {
            return new AcceleratorType(DataType.TUPLE4);
        } else if (object instanceof Tuple5) {
            return new AcceleratorType(DataType.TUPLE5);
        } else if (object instanceof Tuple6) {
            return new AcceleratorType(DataType.TUPLE6);
        } else if (object instanceof Tuple7) {
            return new AcceleratorType(DataType.TUPLE7);
        } else if (object instanceof Tuple8) {
            return new AcceleratorType(DataType.TUPLE8);
        } else if (object instanceof Tuple9) {
            return new AcceleratorType(DataType.TUPLE9);
        }
        // if array
        int dim = getNumArrayDimensions(object);
        if (dim > 0) {

            AcceleratorType arrayType = new AcceleratorType(DataType.ARRAY);

            arrayType.setArrayDim(dim);

            String str = getTypeArray(object).toLowerCase();
            if (str.equals("integer")) {
                str = "int";
            }
            DataType d = getDataTypeArray(str);

            Object a = object;
            for (int j = 0; j < dim; j++) {
                int size = getArraySize(a);
                if (size > 0) {
                    a = Array.get(a, j);
                }
                arrayType.setSize(j, size);
            }

            arrayType.setArrayType(str);
            arrayType.setArrayDataType(d);

            return arrayType;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static <T> void createMetaData(AcceleratorType type, T input, Class<?>... klassAttr) throws Exception {
        int numberFields = type.getType().getNumAttributes();
        AcceleratorType a;
        AcceleratorType b;
        AcceleratorType c;
        AcceleratorType d;
        AcceleratorType e;
        AcceleratorType f;
        AcceleratorType g;
        AcceleratorType h;
        AcceleratorType i;
        List<AcceleratorType> list = type.getTupleComponents();

        switch (numberFields) {
            case 1:
                a = getElementDataType(((((Tuple1) input)._1())));
                a.setPosition(1);
                list.add(a);
                break;
            case 2:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple2) input)._1())));
                    b = getElementDataType(((((Tuple2) input)._2())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                }
                a.setPosition(1);
                b.setPosition(2);
                list.add(a);
                list.add(b);
                break;
            case 3:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple3) input)._1())));
                    b = getElementDataType(((((Tuple3) input)._2())));
                    c = getElementDataType(((((Tuple3) input)._3())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                    c = getElementDataTypeClass(klassAttr[2]);
                }
                a.setPosition(1);
                b.setPosition(2);
                c.setPosition(3);
                list.add(a);
                list.add(b);
                list.add(c);
                break;
            case 4:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple4) input)._1())));
                    b = getElementDataType(((((Tuple4) input)._2())));
                    c = getElementDataType(((((Tuple4) input)._3())));
                    d = getElementDataType(((((Tuple4) input)._4())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                    c = getElementDataTypeClass(klassAttr[2]);
                    d = getElementDataTypeClass(klassAttr[3]);
                }
                a.setPosition(1);
                b.setPosition(2);
                c.setPosition(3);
                d.setPosition(4);
                list.add(a);
                list.add(b);
                list.add(c);
                list.add(d);
                break;
            case 5:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple5) input)._1())));
                    b = getElementDataType(((((Tuple5) input)._2())));
                    c = getElementDataType(((((Tuple5) input)._3())));
                    d = getElementDataType(((((Tuple5) input)._4())));
                    e = getElementDataType(((((Tuple5) input)._5())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                    c = getElementDataTypeClass(klassAttr[2]);
                    d = getElementDataTypeClass(klassAttr[3]);
                    e = getElementDataTypeClass(klassAttr[4]);
                }
                a.setPosition(1);
                b.setPosition(2);
                c.setPosition(3);
                d.setPosition(4);
                e.setPosition(5);
                list.add(a);
                list.add(b);
                list.add(c);
                list.add(d);
                list.add(e);
                break;
            case 6:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple6) input)._1())));
                    b = getElementDataType(((((Tuple6) input)._2())));
                    c = getElementDataType(((((Tuple6) input)._3())));
                    d = getElementDataType(((((Tuple6) input)._4())));
                    e = getElementDataType(((((Tuple6) input)._5())));
                    f = getElementDataType(((((Tuple6) input)._6())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                    c = getElementDataTypeClass(klassAttr[2]);
                    d = getElementDataTypeClass(klassAttr[3]);
                    e = getElementDataTypeClass(klassAttr[4]);
                    f = getElementDataTypeClass(klassAttr[5]);
                }
                a.setPosition(1);
                b.setPosition(2);
                c.setPosition(3);
                d.setPosition(4);
                e.setPosition(5);
                f.setPosition(6);
                list.add(a);
                list.add(b);
                list.add(c);
                list.add(d);
                list.add(e);
                list.add(f);
                break;
            case 7:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple7) input)._1())));
                    b = getElementDataType(((((Tuple7) input)._2())));
                    c = getElementDataType(((((Tuple7) input)._3())));
                    d = getElementDataType(((((Tuple7) input)._4())));
                    e = getElementDataType(((((Tuple7) input)._5())));
                    f = getElementDataType(((((Tuple7) input)._6())));
                    g = getElementDataType(((((Tuple7) input)._7())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                    c = getElementDataTypeClass(klassAttr[2]);
                    d = getElementDataTypeClass(klassAttr[3]);
                    e = getElementDataTypeClass(klassAttr[4]);
                    f = getElementDataTypeClass(klassAttr[5]);
                    g = getElementDataTypeClass(klassAttr[6]);
                }
                a.setPosition(1);
                b.setPosition(2);
                c.setPosition(3);
                d.setPosition(4);
                e.setPosition(5);
                f.setPosition(6);
                g.setPosition(7);
                list.add(a);
                list.add(b);
                list.add(c);
                list.add(d);
                list.add(e);
                list.add(f);
                list.add(g);
                break;
            case 8:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple8) input)._1())));
                    b = getElementDataType(((((Tuple8) input)._2())));
                    c = getElementDataType(((((Tuple8) input)._3())));
                    d = getElementDataType(((((Tuple8) input)._4())));
                    e = getElementDataType(((((Tuple8) input)._5())));
                    f = getElementDataType(((((Tuple8) input)._6())));
                    g = getElementDataType(((((Tuple8) input)._7())));
                    h = getElementDataType(((((Tuple8) input)._8())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                    c = getElementDataTypeClass(klassAttr[2]);
                    d = getElementDataTypeClass(klassAttr[3]);
                    e = getElementDataTypeClass(klassAttr[4]);
                    f = getElementDataTypeClass(klassAttr[5]);
                    g = getElementDataTypeClass(klassAttr[6]);
                    h = getElementDataTypeClass(klassAttr[7]);
                }
                a.setPosition(1);
                b.setPosition(2);
                c.setPosition(3);
                d.setPosition(4);
                e.setPosition(5);
                f.setPosition(6);
                g.setPosition(7);
                h.setPosition(8);
                list.add(a);
                list.add(b);
                list.add(c);
                list.add(d);
                list.add(e);
                list.add(f);
                list.add(g);
                list.add(h);
                break;
            case 9:
                if (klassAttr.length == 0) {
                    a = getElementDataType(((((Tuple9) input)._1())));
                    b = getElementDataType(((((Tuple9) input)._2())));
                    c = getElementDataType(((((Tuple9) input)._3())));
                    d = getElementDataType(((((Tuple9) input)._4())));
                    e = getElementDataType(((((Tuple9) input)._5())));
                    f = getElementDataType(((((Tuple9) input)._6())));
                    g = getElementDataType(((((Tuple9) input)._7())));
                    h = getElementDataType(((((Tuple9) input)._8())));
                    i = getElementDataType(((((Tuple9) input)._9())));
                } else {
                    a = getElementDataTypeClass(klassAttr[0]);
                    b = getElementDataTypeClass(klassAttr[1]);
                    c = getElementDataTypeClass(klassAttr[2]);
                    d = getElementDataTypeClass(klassAttr[3]);
                    e = getElementDataTypeClass(klassAttr[4]);
                    f = getElementDataTypeClass(klassAttr[5]);
                    g = getElementDataTypeClass(klassAttr[6]);
                    h = getElementDataTypeClass(klassAttr[7]);
                    i = getElementDataTypeClass(klassAttr[9]);
                }
                a.setPosition(1);
                b.setPosition(2);
                c.setPosition(3);
                d.setPosition(4);
                e.setPosition(5);
                f.setPosition(6);
                g.setPosition(7);
                h.setPosition(8);
                i.setPosition(9);
                list.add(a);
                list.add(b);
                list.add(c);
                list.add(d);
                list.add(e);
                list.add(f);
                list.add(g);
                list.add(h);
                list.add(i);
                break;
            default:
                break;
        }
    }
}
