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
import java.util.HashMap;
import java.util.List;

public final class HashDataReference {

    private static HashDataReference instance = null;
    private HashMap<Object, ArrayList<Object>> hash;
    private HashMap<Object, Integer> sizes;
    private HashMap<Integer, Object> hashSimple;

    public static HashDataReference getInstance() {
        if (instance == null) {
            instance = new HashDataReference();
        }
        return instance;
    }

    private HashDataReference() {
        hash = new HashMap<>();
        sizes = new HashMap<>();
        hashSimple = new HashMap<>();
    }

    public boolean isObjectInCache(Object simpleData) {
        return hashSimple.containsKey(simpleData);
    }

    public Object getTuple(Object simpleData) {
        return hashSimple.get(simpleData);
    }

    private void add(Object reference, Object a, Object b) {
        ArrayList<Object> listObject = new ArrayList<>();
        listObject.add(a);
        listObject.add(b);
        hash.put(reference, listObject);
    }

    private void add(Object reference, Object a, Object b, int size, int value) {
        hashSimple.put(value, reference);
        add(reference, a, b);
        sizes.put(reference, size);
    }

    public boolean isObject(Object reference) {
        return hash.containsKey(reference);
    }

    public ArrayList<Object> getReferences(Object reference) {
        return hash.get(reference);
    }

    public Integer getSize(Object reference) {
        return sizes.get(reference);
    }

    public void clear() {
        hash.clear();
    }

    @SuppressWarnings("unused")
    private static void insert(Object reference, Object a, Object b) {
        HashDataReference.getInstance().add(reference, a, b);
    }

    public static void insert(Object reference, int size, Object a, Object b, int value) {
        HashDataReference.getInstance().add(reference, a, b, size, value);
    }

    public void insertTuples(Object reference, int value, int size, Object... fields) {
        hashSimple.put(value, reference);
        sizes.put(reference, size);

        ArrayList<Object> listObject = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            listObject.add(fields[i]);
        }

        hash.put(reference, listObject);
    }

    public void insertTuples1(Object reference, int value, int size, Object field) {
        hashSimple.put(value, reference);
        sizes.put(reference, size);

        ArrayList<Object> listObject = new ArrayList<>();
        listObject.add(field);

        hash.put(reference, listObject);
    }

    public static void insertTuple(Object reference, int value, int size, Object... fields) {
        HashDataReference.getInstance().insertTuples(reference, value, size, fields);
    }

    public static boolean isReference(Object reference) {
        return HashDataReference.getInstance().isObject(reference);
    }

    public static List<Object> get(Object reference) {
        return HashDataReference.getInstance().getReferences(reference);
    }
}
