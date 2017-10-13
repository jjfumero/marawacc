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

package uk.ac.ed.jpai.graphs;

import java.util.Arrays;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple5;
import uk.ac.ed.jpai.MapAccelerator;

/**
 * Bread First Search algorithm solved with JPAI and Marawacc. The graph computation is performed on
 * the GPU via OpenCL when Marawacc detects an available GPU.
 *
 * BFS parallel algorithm adapted from: classroom.udacity.com/courses/cs344/
 *
 */
public class BFS {

    public static void connect(int from, int to, int[] graph, int N) {
        graph[from * N + to] = 1;
    }

    /**
     * It builds a simple graph just for showing the example.
     *
     * @param adjacencyMatrix
     * @param numNodes
     */
    public static void initilizeAdjacencyMatrixSimpleGraph(int[] adjacencyMatrix, int numNodes) {
        Arrays.fill(adjacencyMatrix, 0);
        connect(0, 1, adjacencyMatrix, numNodes);
        connect(0, 4, adjacencyMatrix, numNodes);
        connect(1, 2, adjacencyMatrix, numNodes);
        connect(2, 3, adjacencyMatrix, numNodes);
        connect(2, 4, adjacencyMatrix, numNodes);
        connect(3, 4, adjacencyMatrix, numNodes);
    }

    public static void jpaiBFS(int root, int numberOfNodes) {

        int numNodes = numberOfNodes;

        int[] vertices = new int[numNodes];
        int[] adjacencyMatrix = new int[numNodes * numNodes];

        initilizeAdjacencyMatrixSimpleGraph(adjacencyMatrix, numNodes);

        // Step 1: vertices initialisation
        for (int i = 0; i < numNodes; i++) {
            if (i == root) {
                vertices[i] = 0;
            } else {
                vertices[i] = -1;
            }
        }

        PArray<Tuple2<Integer, Integer>> input = new PArray<>(numNodes * numNodes, TypeFactory.Tuple("Tuple2<Integer, Integer>"));
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                input.put(i * numNodes + j, new Tuple2<>(i, j));
            }
        }

        boolean done = false;
        int current = -1;

        // Step 2: kernel computation
        while (!done) {

            current++;
            int currentNode = current;

            MapAccelerator<Tuple2<Integer, Integer>, Tuple5<Integer, Integer, Integer, Integer, Short>> function = new MapAccelerator<>(x -> {

                int idx = x._1();
                int jdx = x._2();
                short h_true = 1;
                int access = idx * numNodes + jdx;
                int vx = -1;
                int vy = -1;

                if (adjacencyMatrix[access] == 1) {
                    int dfirst = vertices[idx];
                    int dsecond = vertices[jdx];
                    if ((currentNode == dfirst) && (dsecond == -1)) {
                        vy = dfirst + 1;
                        h_true = 0;
                    }

                    if ((currentNode == dsecond) && (dfirst == -1)) {
                        vx = dsecond + 1;
                        h_true = 0;
                    }
                }

                Tuple5<Integer, Integer, Integer, Integer, Short> t = new Tuple5<>();
                t._1 = vx;
                t._2 = idx;
                t._3 = vy;
                t._4 = jdx;
                t._5 = h_true;
                return t;
            });

            PArray<Tuple5<Integer, Integer, Integer, Integer, Short>> output = function.apply(input);

            // Step 3: update vertices info
            for (int i = 0; i < numNodes; i++) {
                for (int j = 0; j < numNodes; j++) {
                    if (output.get(i * numNodes + j)._1 != -1) {
                        int position = output.get(i * numNodes + j)._2;
                        vertices[position] = output.get(i * numNodes + j)._1;
                    }
                    if (output.get(i * numNodes + j)._3 != -1) {
                        int position = output.get(i * numNodes + j)._4;
                        vertices[position] = output.get(i * numNodes + j)._3;
                    }
                }
            }

            System.out.println("Partial Solution: " + Arrays.toString(vertices));

            // Check if there is no updated
            boolean check = true;
            for (int i = 0; i < numNodes * numNodes; i++) {
                if (output.get(i)._5 == 0) {
                    check = false;
                    break;
                }
            }
            if (check == true) {
                done = true;
            }
        }
        System.out.println("Solution: " + Arrays.toString(vertices));
    }

    public static void main(String[] args) {
        jpaiBFS(0, 5);
    }
}
