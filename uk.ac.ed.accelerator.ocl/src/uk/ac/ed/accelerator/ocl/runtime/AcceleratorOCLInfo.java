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

import java.util.ArrayList;

import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType.DataType;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
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

public class AcceleratorOCLInfo {

    private RuntimeObjectTypeInfo[] inputType;
    private RuntimeObjectTypeInfo[] outputType;
    private Object[] scopeParams;

    private AcceleratorType[] inputOCLType;
    private AcceleratorType[] outputOCLType;
    private AcceleratorType[] scopeOCLTypes;

    private ArrayList<AcceleratorOCLInfo> nested;

    private AcceleratorType classInput;
    private AcceleratorType classOutput;

    public AcceleratorOCLInfo(RuntimeObjectTypeInfo[] input, RuntimeObjectTypeInfo[] output, Object[] scopeParams) throws Exception {
        this.inputType = input;
        this.outputType = output;
        this.scopeParams = scopeParams;
        inferOCLType();
    }

    public RuntimeObjectTypeInfo[] getInput() {
        return this.inputType;
    }

    public RuntimeObjectTypeInfo[] getOutput() {
        return this.outputType;
    }

    public AcceleratorType[] getOCLInput() {
        return this.inputOCLType;
    }

    public AcceleratorType[] getOCLOutput() {
        return this.outputOCLType;
    }

    public AcceleratorType[] getOCLScope() {
        return this.scopeOCLTypes;
    }

    public AcceleratorType getClassInput() {
        return this.classInput;
    }

    public AcceleratorType getClassOutput() {
        return this.classOutput;
    }

    private void inferOCLType() throws Exception {

        inputOCLType = new AcceleratorType[inputType.length];
        for (int i = 0; i < inputType.length; i++) {
            inputOCLType[i] = createOCLType(inputType[i]);
        }

        // Not sure only first position
        classInput = getElementDataTypeClass(inputType[0].getClassObject());
        classOutput = getElementDataTypeClass(outputType[0].getClassObject());

        outputOCLType = new AcceleratorType[outputType.length];
        for (int i = 0; i < outputType.length; i++) {
            outputOCLType[i] = createOCLType(outputType[i]);
        }

        scopeOCLTypes = new AcceleratorType[scopeParams.length];
        for (int i = 0; i < scopeParams.length; i++) {
            scopeOCLTypes[i] = TuplesHelper.getElementDataType(scopeParams[i]);
        }

        if (scopeOCLTypes.length == 1 && (scopeOCLTypes[0] == null)) {
            scopeOCLTypes = new AcceleratorType[scopeParams.length];
        }

        // XXX: Support nested types
        nested = new ArrayList<>();
    }

    public ArrayList<AcceleratorOCLInfo> getNested() {
        return this.nested;
    }

    private static AcceleratorType createOCLType(RuntimeObjectTypeInfo objectType) throws Exception {
        return getElementDataTypeClass(objectType.getClassObject());
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
        } else if (klass == Tuple10.class) {
            return new AcceleratorType(DataType.TUPLE10);
        } else if (klass == Tuple11.class) {
            return new AcceleratorType(DataType.TUPLE11);
        }
        throw new Exception(klass + " data type not supported yet");
    }

}
