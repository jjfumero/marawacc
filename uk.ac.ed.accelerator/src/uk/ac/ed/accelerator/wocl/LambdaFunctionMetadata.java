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
package uk.ac.ed.accelerator.wocl;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Metadata and helper for the code generator. It allows to distinguish if the lambda function is a
 * {@link Function} or {@link BiFunction}. It also keeps the number of elements will apply to the
 * lambda expression.
 */
public class LambdaFunctionMetadata {

    public enum TypeOfFunction {
        FUNCTION(1),
        BIFUNCTION(2);

        private int numParams;

        TypeOfFunction(int numParams) {
            this.numParams = numParams;
        }

        public int getNumParams() {
            return numParams;
        }
    }

    private TypeOfFunction type;
    private int elements;

    public LambdaFunctionMetadata(TypeOfFunction type) {
        this.type = type;
    }

    public TypeOfFunction getType() {
        return this.type;
    }

    public void setElements(int num) {
        this.elements = num;
    }

    public int getElements() {
        return this.elements;
    }
}
