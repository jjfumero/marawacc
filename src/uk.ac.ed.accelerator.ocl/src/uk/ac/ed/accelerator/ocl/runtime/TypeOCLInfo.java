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

public class TypeOCLInfo extends AbstractTypeInfo<AcceleratorType> {

    private ArrayList<TypeOCLInfo> nested;
    private TypeInfo typeInfo;

    public TypeOCLInfo(TypeInfo typeInfo, Class<?>[] klassAttr) throws Exception {

        this.typeInfo = typeInfo;

        input = createDataTypeStructure(typeInfo.getInput(), true);
        output = createDataTypeStructure(typeInfo.getOutput(), true);

        if (output == null) {
            output = createDataTypeStructure(typeInfo.getOutput(), false, klassAttr);
        }

        Object[] externalTypes = typeInfo.getScopeInput();
        scopeInput = new AcceleratorType[externalTypes.length];
        for (int i = 0; i < externalTypes.length; i++) {
            scopeInput[i] = createDataTypeStructure(externalTypes[i], true);
        }

        if (scopeInput.length == 1 && (scopeInput[0] == null)) {
            scopeInput = new AcceleratorType[externalTypes.length];
        }

        nested = new ArrayList<>();

        for (TypeInfo type : typeInfo.getNested()) {
            TypeOCLInfo oclType = new TypeOCLInfo(type, klassAttr);
            nested.add(oclType);
        }
    }

    private static AcceleratorType createDataTypeStructure(Object obj, boolean instance, Class<?>... klassAttr) throws Exception {
        AcceleratorType dataType = null;
        if (instance) {
            dataType = TuplesHelper.getElementDataType(obj);
        } else {
            dataType = TuplesHelper.getElementDataTypeClass((Class<?>) obj);
        }

        if (dataType != null) {

            ArrayList<AcceleratorType> tupleComponents = dataType.getTupleComponents();

            if (dataType.getType() != DataType.ARRAY) {
                int counter = 0;

                TuplesHelper.createMetaData(dataType, obj, klassAttr);

                boolean removed = false;
                for (int i = 0; i < tupleComponents.size(); i++) {
                    AcceleratorType elem = tupleComponents.get(i);
                    if (elem.getType() == DataType.ARRAY) {
                        elem.invalidate();
                        removed = true;
                    } else {
                        counter++;
                    }
                }

                if (removed) {
                    dataType.setNumAttributes(counter);
                }
            }
        }
        return dataType;
    }

    @Override
    public ArrayList<TypeOCLInfo> getNested() {
        return nested;
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }

    @Override
    public String toString() {
        String stringRepresentation = "input => " + input + "; output => " + output;
        if (nested.size() != 0) {
            stringRepresentation += "; nested: " + nested;
        }
        return stringRepresentation;
    }

}
