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

import java.nio.Buffer;

public class ArraySlice<T> extends PArray<T> {

    private PArray<T> array;
    private int offset;
    private int size;

    public ArraySlice(PArray<T> array, int offset, int size) {
        this.array = array;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public void put(int index, T e) {
        array.put(index + offset, e);
    }

    @Override
    public T get(int index) {
        return array.get(index + offset);
    }

    @Override
    public int offset() {
        return this.offset;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Buffer getArrayReference() {
        return array.getArrayReference();
    }

    @Override
    public Buffer getArrayReference(int idxArray) {
        return array.getArrayReference(idxArray);
    }

    @Override
    public int grade() {
        return array.grade();
    }

    @Override
    public Class<?> getClassObject() {
        return array.getClassObject();
    }

    @Override
    public StorageMode getStorageMode() {
        return array.getStorageMode();
    }

    @Override
    public ArraySlice<T>[] splitInChunksOfSize(int chunkSize) {
        return array.splitInChunksOfSize(chunkSize);
    }

    @Override
    public ArraySlice<T>[] splitInFixedNumberOfChunks(int numberOfChunks) {
        return array.splitInFixedNumberOfChunks(numberOfChunks);
    }
}
