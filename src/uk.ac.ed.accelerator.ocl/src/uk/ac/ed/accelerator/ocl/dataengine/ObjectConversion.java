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

package uk.ac.ed.accelerator.ocl.dataengine;

import java.lang.reflect.Field;
import java.util.ArrayList;

import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType;
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
 * ObjectConversion: Translate the Tuple class with Generics into simple data type to do the
 * marshaling of each field (SoA).
 *
 */
public class ObjectConversion {

    private int maxElements;

    private static Class<?> getTupleClass(int num) {
        switch (num) {
            case 1:
                return Tuple1.class;
            case 2:
                return Tuple2.class;
            case 3:
                return Tuple3.class;
            case 4:
                return Tuple4.class;
            case 5:
                return Tuple5.class;
            case 6:
                return Tuple6.class;
            case 7:
                return Tuple7.class;
            case 8:
                return Tuple8.class;
            case 9:
                return Tuple9.class;
            default:
                return null;
        }
    }

    public Object objectAllocation(AcceleratorType t, Object[] input, int[] sizes, int fromToSize) {
        // 2D -> 1D. We provide flattened arrays.
        DataType dataType = t.getArrayDataType();
        int lenSubArray = t.getSize(0);
        int numElementsTuple = input.length;
        if (fromToSize != -1) {
            numElementsTuple = fromToSize;
        }
        sizes[0] = lenSubArray * numElementsTuple;
        switch (dataType) {
            case SHORT:
                short[] as2 = new short[lenSubArray * numElementsTuple];
                return as2;

            case INTEGER:
                int[] ai = new int[lenSubArray * numElementsTuple];
                return ai;

            case FLOAT:
                float[] af = new float[lenSubArray * numElementsTuple];
                return af;

            case DOUBLE:
                double[] ad = new double[lenSubArray * numElementsTuple];
                return ad;

            default:
                return null;
        }
    }

    public ArrayList<Object> fromArrayJavaToOCL(ArrayList<AcceleratorType> datatypeList, AcceleratorType t, Object[] input, int index, boolean[] generateMatrix, int fromA, int toA) {

        // Support for 2D
        // TODO: 3D

        ArrayList<Object> params = new ArrayList<>();

        this.maxElements = input.length;

        Class<?> objectClass = getTupleClass(datatypeList.size());

        DataType dataType = t.getArrayDataType();
        int lenSubArray = t.getSize(0);

        generateMatrix[index - 1] = true;

        boolean partialCopy = false;
        int from = fromA;
        int to = toA;
        if (fromA != -1 && toA != -1) {
            partialCopy = true;
        }

        int size = partialCopy ? (to - from) : maxElements;
        int idx = 0;
        if (!partialCopy) {
            from = 0;
            to = maxElements;
        }

        switch (dataType) {
            case SHORT:
                Short[][] as2 = new Short[size][lenSubArray];
                idx = 0;
                for (int j = from; j < to; j++) {
                    Short[] aux = (Short[]) getElement(objectClass, input[j], index);
                    for (int k = 0; k < lenSubArray; k++) {
                        try {
                            as2[idx][k] = aux[k];
                        } catch (Exception e) {
                            as2[idx][k] = (short) 0;
                        }
                    }
                    idx++;
                }
                params.add(as2);
                break;
            case FLOAT:
                Float[][] af = new Float[size][lenSubArray];
                idx = 0;
                for (int j = from; j < to; j++) {
                    Float[] aux = (Float[]) getElement(objectClass, input[j], index);
                    for (int k = 0; k < lenSubArray; k++) {
                        try {
                            af[idx][k] = aux[k];
                        } catch (Exception e) {
                            af[idx][k] = 0.0f;
                        }
                    }
                    idx++;
                }
                params.add(af);
                break;
            case DOUBLE:
                Double[][] ad = new Double[size][lenSubArray];
                idx = 0;
                for (int j = from; j < to; j++) {
                    Double[] aux = (Double[]) getElement(objectClass, input[j], index);
                    for (int k = 0; k < lenSubArray; k++) {
                        try {
                            ad[idx][k] = aux[k];
                        } catch (Exception e) {
                            ad[idx][k] = 0.0;
                        }
                    }
                    idx++;
                }
                params.add(ad);
                break;
            default:
                params = null;
                break;
        }
        return params;
    }

    public ArrayList<Object> fromJavaToOCL(ArrayList<AcceleratorType> datatypeList, Object[] input, boolean[] generateMatrix, int fromA, int toA, int[] sizes, Object... arr) throws Exception {

        ArrayList<Object> params = new ArrayList<>();

        this.maxElements = input.length;

        boolean isArray = false;
        if (arr != null) {
            isArray = true;
        }
        boolean partialCopy = false;
        int from = fromA;
        int to = toA;
        if (fromA != -1 && toA != -1) {
            partialCopy = true;
        }

        Class<?> objectClass = getTupleClass(datatypeList.size());

        for (int i = 0; i < datatypeList.size(); i++) {
            DataType dataType = datatypeList.get(i).getType();

            int field = i + 1;
            int size = partialCopy ? (to - from) : maxElements;
            sizes[i] = size;
            int idx = 0;
            if (!partialCopy) {
                from = 0;
                to = maxElements;
            }
            switch (dataType) {
                case FLOAT:
                    Float[] af = new Float[size];
                    idx = 0;
                    for (int j = from; j < to; j++) {
                        af[idx] = (Float) getElement(objectClass, input[j], field);
                        idx++;
                    }
                    params.add(af);
                    break;
                case DOUBLE:
                    Double[] ad = new Double[size];
                    idx = 0;
                    for (int j = from; j < to; j++) {
                        ad[idx] = (Double) getElement(objectClass, input[j], field);
                        idx++;
                    }
                    params.add(ad);
                    break;
                case INTEGER:
                    Integer[] ai = new Integer[size];
                    idx = 0;
                    for (int j = from; j < to; j++) {
                        ai[idx] = (Integer) getElement(objectClass, input[j], field);
                        idx++;
                    }
                    params.add(ai);
                    break;
                case SHORT:
                    if (isArray) {
                        Short[][] as2 = new Short[size][maxElements];
                        idx = 0;
                        for (int j = from; j < to; j++) {
                            for (int k = 0; k < maxElements; k++) {
                                try {
                                    as2[idx][k] = (Short) input[j];
                                } catch (Exception e) {
                                    as2[idx][k] = (short) 0;
                                }
                            }
                            idx++;
                        }
                        params.add(as2);
                    } else {
                        Short[] as = new Short[size];
                        idx = 0;
                        for (int j = from; j < to; j++) {
                            as[idx] = (Short) getElement(objectClass, input[j], field);
                            idx++;
                        }
                        params.add(as);
                    }

                    break;
                case ARRAY:
                    ArrayList<Object> l = fromArrayJavaToOCL(datatypeList, datatypeList.get(i), input, field, generateMatrix, from, to);
                    if (l != null) {
                        params.add(l.get(0));
                    } else {
                        throw new Exception("Data type parameter not found");
                    }
                    break;
            }
        }
        return params;
    }

    private static Object getElement(Class<?> k, Object o, int field) {
        Object element = null;
        try {
            Field f = k.getField("_" + field);
            element = f.get(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return element;
    }

}
