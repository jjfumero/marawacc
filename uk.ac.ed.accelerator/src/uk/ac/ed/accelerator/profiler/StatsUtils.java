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

package uk.ac.ed.accelerator.profiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Vector;

public final class StatsUtils {

    public static double computeAvarage(LinkedList<Long> list) {
        double average = 0;
        for (Long element : list) {
            average += element;
        }
        average /= list.size();
        return average;
    }

    public static double computeAvarage(Vector<Long> list) {
        double average = 0;
        for (Long element : list) {
            average += element;
        }
        average /= list.size();
        return average;
    }

    public static double computeAvarage(ArrayList<Long> list) {
        double average = 0;
        for (Long element : list) {
            average += element;
        }
        average /= list.size();
        return average;
    }

    public static long computeMedian(LinkedList<Long> list) {
        Collections.sort(list);
        int middle = list.size() / 2;
        long medianEvent = 0;
        if (list.size() % 2 == 1) {
            medianEvent = list.get(middle);
        } else {
            medianEvent = (list.get(middle - 1) + list.get(middle)) / 2;
        }
        return medianEvent;
    }

    public static long computeMedian(Vector<Long> list) {
        if (list.isEmpty()) {
            return 0;
        }
        Collections.sort(list);
        int middle = list.size() / 2;
        long medianEvent = 0;
        if (list.size() % 2 == 1) {
            medianEvent = list.get(middle);
        } else {
            medianEvent = (list.get(middle - 1) + list.get(middle)) / 2;
        }
        return medianEvent;
    }

    public static long computeMedian(ArrayList<Long> list) {
        Collections.sort(list);
        int middle = list.size() / 2;
        long medianEvent = 0;
        if (list.size() % 2 == 1) {
            medianEvent = list.get(middle);
        } else {
            medianEvent = (list.get(middle - 1) + list.get(middle)) / 2;
        }
        return medianEvent;
    }
}
