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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class TypeFactory {

    public static final Pattern PATTERN_TUPLE_REPETITION = Pattern.compile("Tuple(\\d)::(\\w+)>");
    public static final Pattern PATTERN_TUPLE = Pattern.compile("Tuple\\d<(\\w+)((,\\w+)*)>");

    public static RuntimeObjectTypeInfo Integer() {
        return new RuntimeObjectTypeInfo(Integer.class);
    }

    public static RuntimeObjectTypeInfo Double() {
        return new RuntimeObjectTypeInfo(Double.class);
    }

    public static RuntimeObjectTypeInfo Long() {
        return new RuntimeObjectTypeInfo(Long.class);
    }

    public static RuntimeObjectTypeInfo Short() {
        return new RuntimeObjectTypeInfo(Short.class);
    }

    public static RuntimeObjectTypeInfo Float() {
        return new RuntimeObjectTypeInfo(Float.class);
    }

    public static RuntimeObjectTypeInfo Boolean() {
        return new RuntimeObjectTypeInfo(Boolean.class);
    }

    public static RuntimeObjectTypeInfo Char() {
        return new RuntimeObjectTypeInfo(Character.class);
    }

    public static RuntimeObjectTypeInfo Character() {
        return new RuntimeObjectTypeInfo(Character.class);
    }

    private static Object typeUtil(String type) {
        String typeClean = type.replaceAll(",", "");
        typeClean = typeClean.replaceAll(" ", "");
        if (typeClean.equals("Float")) {
            return 0.0f;
        } else if (typeClean.equals("Integer")) {
            return 0;
        } else if (typeClean.equals("Short")) {
            return (short) 0;
        } else if (typeClean.equals("Boolean")) {
            return true;
        } else if (typeClean.equals("Double")) {
            return 0.0;
        } else if (typeClean.equals("Character")) {
            return 'c';
        } else if (typeClean.equals("Long")) {
            return (long) 0;
        } else {
            throw new RuntimeException("Data type not supported yet");
        }
    }

    public static RuntimeObjectTypeInfo inferFromObject(Object object) {
        return RuntimeObjectTypeInfo.inferFromObject(object);
    }

    /**
     * Build a TupleN<T1, T2, ...,TN> from an input string that represent that Tuple. For example:
     *
     * <pre>
     * RuntimeTypeInfo t = TypeFactory.Tuple("Tuple2<Float, Long>");
     * </pre>
     *
     * @param tuple
     * @return {@link RuntimeObjectTypeInfo}
     */
    public static RuntimeObjectTypeInfo Tuple(String tuple) {

        String tupleClean = tuple.replaceAll(" ", "");
        String finalTuple = "";

        // TupleX::Type -> if it is the same type
        if (tuple.contains("::")) {
            // Compose the real expression
            Pattern pattern = Pattern.compile("Tuple(\\d)::(\\w+)");
            Matcher matcher = pattern.matcher(tupleClean);

            if (matcher.find()) {
                int tupleNumber = Integer.parseInt(matcher.group(1));
                String type = matcher.group(2);
                System.out.println(tupleNumber);
                finalTuple = "Tuple" + tupleNumber + "<";

                for (int i = 0; i < tupleNumber; i++) {
                    finalTuple += type + ",";
                }
                finalTuple = finalTuple.substring(0, finalTuple.length() - 1);
                finalTuple += ">";
                tupleClean = finalTuple;
            }
        }

        // Tuple2<A, B> | A, B are simple data types
        Matcher matcher = PATTERN_TUPLE.matcher(tupleClean);

        if (matcher.find()) {
            int tupleNumber = matcher.groupCount();
            String rest = matcher.group(2).substring(1, matcher.group(2).length());
            String[] m = rest.split(",");

            if (m.length > 1) {
                tupleNumber = m.length + 1;
            } else {
                tupleNumber = 2;
            }

            switch (tupleNumber) {
                case 2:
                    return new Tuple2<>(typeUtil(matcher.group(1)), typeUtil(m[0])).getType();
                case 3:
                    return new Tuple3<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1])).getType();
                case 4:
                    return new Tuple4<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2])).getType();
                case 5:
                    return new Tuple5<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2]), typeUtil(m[3])).getType();
                case 6:
                    return new Tuple6<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2]), typeUtil(m[3]), typeUtil(m[4])).getType();
                case 7:
                    return new Tuple7<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2]), typeUtil(m[3]), typeUtil(m[4]), typeUtil(m[5])).getType();
                case 8:
                    return new Tuple8<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2]), typeUtil(m[3]), typeUtil(m[4]), typeUtil(m[5]), typeUtil(m[6])).getType();
                case 9:
                    return new Tuple9<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2]), typeUtil(m[3]), typeUtil(m[4]), typeUtil(m[5]), typeUtil(m[6]),
                                    typeUtil(m[7])).getType();
                case 10:
                    return new Tuple10<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2]), typeUtil(m[3]), typeUtil(m[4]), typeUtil(m[5]), typeUtil(m[6]), typeUtil(m[7]),
                                    typeUtil(m[8])).getType();
                case 11:
                    return new Tuple11<>(typeUtil(matcher.group(1)), typeUtil(m[0]), typeUtil(m[1]), typeUtil(m[2]), typeUtil(m[3]), typeUtil(m[4]), typeUtil(m[5]), typeUtil(m[6]), typeUtil(m[7]),
                                    typeUtil(m[8]), typeUtil(m[9])).getType();
                default:
                    throw new RuntimeException("Tuple not supported yet.");
            }
        }
        return null;
    }
}
