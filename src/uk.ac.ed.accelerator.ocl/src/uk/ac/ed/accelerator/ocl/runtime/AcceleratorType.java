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

public class AcceleratorType {

    /**
     * Enumerate to specify the data type. It could be simple or Tuple
     */
    public enum DataType {

        // -------------------------------------------------------------------
        // FORMAT
        // -------------------------------------------------------------------
        // Name, numAttrs, father (arrays of arrays only), OpenCL variable type
        // -------------------------------------------------------------------

        BYTE("Byte", 1, null, "byte"),
        SHORT("Short", 1, null, "short"),
        INTEGER("Integer", 1, null, "int"),
        FLOAT("Float", 1, null, "float"),
        DOUBLE("Double", 1, null, "double"),
        LONG("Long", 1, null, "long"),
        BOOLEAN("Boolean", 1, null, "boolean"),
        CHAR("Char", 1, null, "char"),
        TUPLE1("Tuple1", 1, "Tuple", "struct_tuples1"),
        TUPLE2("Tuple2", 2, "Tuple", "struct_tuples2"),
        TUPLE3("Tuple3", 3, "Tuple", "struct_tuples3"),
        TUPLE4("Tuple4", 4, "Tuple", "struct_tuples4"),
        TUPLE5("Tuple5", 5, "Tuple", "struct_tuples5"),
        TUPLE6("Tuple6", 6, "Tuple", "struct_tuples6"),
        TUPLE7("Tuple7", 7, "Tuple", "struct_tuples7"),
        TUPLE8("Tuple8", 8, "Tuple", "struct_tuples8"),
        TUPLE9("Tuple9", 9, "Tuple", "struct_tuples9"),
        TUPLE10("Tuple10", 10, "Tuple", "struct_tuples10"),
        TUPLE11("Tuple11", 11, "Tuple", "struct_tuples11"),
        ARRAY("Array", 0, null, null);

        private String name;
        private int numAttrs;
        private String father;
        private String arrayType;
        private String oclName;

        // Format:
        DataType(String name, int num, String father, String oclName) {
            this.name = name;
            this.numAttrs = num;
            this.father = father;
            this.oclName = oclName;
        }

        public String getFather() {
            return this.father;
        }

        public int getNumAttributes() {
            return this.numAttrs;
        }

        public String getOCLName() {
            return this.oclName;
        }

        public String getArrayType() {
            return this.arrayType;
        }

        @Override
        public String toString() {
            return this.name.toString();
        }
    }

    private int numAttrs;
    private DataType dataTypeElementArray;
    private int arrayDim;
    private int position;
    private String arrayType;

    private ArrayList<AcceleratorType> tupleComponents;

    private DataType type;
    private boolean valid;
    private int[] arraySize;
    private int newSize;

    public AcceleratorType(DataType type) {
        this.type = type;
        this.numAttrs = -1;
        this.valid = true;  // The attr is valid by default
        this.arraySize = new int[3];
        this.newSize = -1;
        tupleComponents = new ArrayList<>();
    }

    public DataType getType() {
        return this.type;
    }

    /*
     * 
     * set of attributes in the case of Tuples
     */
    public void setNumAttributes(int numAttrs) {
        this.numAttrs = numAttrs;
    }

    public int getNumAttributes() {
        return numAttrs;
    }

    public void setArrayType(String str) {
        this.arrayType = str;
    }

    public String getArrayNameType() {
        return this.arrayType;
    }

    public void setArrayDataType(DataType dataTypeArray) {
        this.dataTypeElementArray = dataTypeArray;
    }

    public DataType getArrayDataType() {
        return this.dataTypeElementArray;
    }

    public void setArrayDim(int dim) {
        this.arrayDim = dim;
    }

    public void setSize(int dim, int size) {
        if (dim < 3) {
            this.arraySize[dim] = size;
        }
    }

    public void setNewSizeforMultidevice(int size) {
        this.newSize = size;
    }

    public int getNewSizeforMultidevice() {
        return this.newSize;
    }

    public int getSize(int dim) {
        if (dim < 3) {
            return arraySize[dim];
        } else {
            return -1;
        }
    }

    public int getArrayDim() {
        return this.arrayDim;
    }

    public void setPosition(int pos) {
        this.position = pos;
    }

    public int getPosition() {
        return this.position;
    }

    public void invalidate() {
        this.valid = false;
    }

    public boolean isValid() {
        return this.valid;
    }

    @Override
    public String toString() {
        String stringRepresentation = getType().toString();

        if (type == DataType.ARRAY) {
            stringRepresentation += " [" + getArrayDataType() + "]";
        } else if (tupleComponents != null && tupleComponents.size() != 0) {
            stringRepresentation += " " + tupleComponents;
        }

        return stringRepresentation;
    }

    // Returns the number of arrays required for storing
    public int getNumberOfComponents() {
        if (type == DataType.ARRAY) {
            return getNumberOfComponents(dataTypeElementArray);
        } else {
            return getNumberOfComponents(type);
        }
    }

    private static int getNumberOfComponents(DataType type) {
        return type.getNumAttributes() == 0 ? 1 : type.getNumAttributes();
    }

    public ArrayList<AcceleratorType> getTupleComponents() {
        return tupleComponents;
    }

    public void setTupleComponents(ArrayList<AcceleratorType> tupleComponents) {
        this.tupleComponents = tupleComponents;
    }
}
