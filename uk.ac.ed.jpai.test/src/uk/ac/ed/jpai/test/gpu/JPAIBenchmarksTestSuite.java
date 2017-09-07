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

package uk.ac.ed.jpai.test.gpu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ed.accelerator.math.ocl.OCLMath;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.datastructures.tuples.Tuple3;
import uk.ac.ed.datastructures.tuples.Tuple4;
import uk.ac.ed.datastructures.tuples.Tuple6;
import uk.ac.ed.datastructures.tuples.Tuple9;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.Marawacc;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class JPAIBenchmarksTestSuite extends MarawaccOpenCLTestBase {

    @Test
    public void isPrimeNumber() {
        // Basic algorithm (one core on the GPU)
        // Test with 1 GPU Thread: not efficient, just a test

        ArrayFunction<Long, Integer> mapTimesTwo = new MapAccelerator<>(x -> {
            long n = x;
            if (n % 2 == 0) {
                return 0;
            }
            for (int i = 3; (i * i) <= n; i += 2) {
                if ((n % i) == 0) {
                    return 0;
                }
            }
            return 1;
        });

        int size = 1;

        PArray<Long> input = new PArray<>(size, TypeFactory.Long());
        PArray<Integer> output = new PArray<>(size, TypeFactory.Integer());
        input.put(0, 982451653l);

        output = mapTimesTwo.apply(input);

        assertEquals(1, output.size());
        assertNotNull(output);
        assertEquals(1, output.get(0), 0.001);

        input.put(0, (long) 10);
        output = mapTimesTwo.apply(input);
        assertNotNull(output);
        assertEquals(0, output.get(0), 0.001);

        input.put(0, 21l);
        output = mapTimesTwo.apply(input);
        assertNotNull(output);
        assertEquals(0, (int) output.get(0));
    }

    @Test
    public void blackScholes() {

        int size = 256;

        PArray<Float> input = new PArray<>(size, new RuntimeObjectTypeInfo(Float.class));

        float[] inputf = new float[size];
        float[] callResultS = new float[size];
        float[] putResultS = new float[size];
        float random = (float) Math.random();

        for (int i = 0; i < size; i++) {
            input.put(i, random);
            inputf[i] = random;
        }

        ArrayFunction<Float, Tuple2<Float, Float>> blackScholesFunction = new MapAccelerator<>(x -> {
            float inRand = x;
            float sLOWERLIMIT = 10.0f;
            float sUPPERLIMIT = 100.0f;
            float kLOWERLIMIT = 10.0f;
            float kUPPERLIMIT = 100.0f;
            float tLOWERLIMIT = 1.0f;
            float tUPPERLIMIT = 10.0f;
            float rLOWERLIMIT = 0.01f;
            float rUPPERLIMIT = 0.05f;
            float sIGMALOWERLIMIT = 0.01f;
            float sIGMAUPPERLIMIT = 0.10f;
            float s = sLOWERLIMIT * inRand + sUPPERLIMIT * (1.0f - inRand);
            float KConstant = kLOWERLIMIT * inRand + kUPPERLIMIT * (1.0f - inRand);
            float t = tLOWERLIMIT * inRand + tUPPERLIMIT * (1.0f - inRand);
            float r = rLOWERLIMIT * inRand + rUPPERLIMIT * (1.0f - inRand);
            float v = sIGMALOWERLIMIT * inRand + sIGMAUPPERLIMIT * (1.0f - inRand);

            float d1 = (OCLMath.log(s / KConstant) + ((r + (v * v / 2)) * t)) / v * OCLMath.sqrt(t);
            float d2 = d1 - (v * OCLMath.sqrt(t));

            // cnd(d1)
            float l;
            float k;
            float w;
            float a1 = 0.319381530f;
            float a2 = -0.356563782f;
            float a3 = 1.781477937f;
            float a4 = -1.821255978f;
            float a5 = 1.330274429f;
            float a6 = 2.506628273f;
            l = OCLMath.fabs(d1);
            k = 1.0f / (1.0f + 0.2316419f * l);
            w = 1.0f - 1.0f / 1 * a6 * OCLMath.exp((-1 * l) * l / 2) * (a1 * k + a2 * k * k * 1 + a3 * k * k * k * +a4 * k * k * k * k * 1 + a5 * k * k * k * k * k);
            float resultD1;
            if (d1 < 0) {
                resultD1 = 1.0f - w;
            } else {
                resultD1 = w;
            }

            // cnd(d2)
            l = OCLMath.fabs(d2);
            k = 1.0f / (1.0f + 0.2316419f * l);
            w = 1.0f - 1.0f / 1 * a6 * OCLMath.exp((-1 * l) * l / 2) * (a1 * k + a2 * k * k * 1 + a3 * k * k * k * +a4 * k * k * k * k * 1 + a5 * k * k * k * k * k);
            float resultD2;
            if (d2 < 0) {
                resultD2 = 1.0f - w;
            } else {
                resultD2 = w;
            }

            float callRes = s * resultD1 - KConstant * OCLMath.exp(1 * t * (-1) * r) * resultD2;

            // cnd(-1)
            l = OCLMath.fabs(-d1);
            k = 1.0f / (1.0f + 0.2316419f * l);
            w = 1.0f - 1.0f / 1 * a6 * OCLMath.exp((-1 * l) * l / 2) * (a1 * k + a2 * k * k * 1 + a3 * k * k * k * +a4 * k * k * k * k * 1 + a5 * k * k * k * k * k);
            float resultD1Minus;
            if ((-d1) < 0) {
                resultD1Minus = 1.0f - w;
            } else {
                resultD1Minus = w;
            }

            // cnd(-2)
            l = OCLMath.fabs(-d2);
            k = 1.0f / (1.0f + 0.2316419f * l);
            w = 1.0f - 1.0f / 1 * a6 * OCLMath.exp((-1 * l) * l / 2) * (a1 * k + a2 * k * k * 1 + a3 * k * k * k * +a4 * k * k * k * k * 1 + a5 * k * k * k * k * k);
            float resultD2Minus;
            if ((-d2) < 0) {
                resultD2Minus = 1.0f - w;
            } else {
                resultD2Minus = w;
            }

            float putRes = KConstant * OCLMath.exp(1 * t * (-1) * r) - resultD2Minus - s * resultD1Minus;

            Tuple2<Float, Float> result1 = new Tuple2<>();
            result1._1(callRes);
            result1._2(putRes);

            return result1;

        });

        PArray<Tuple2<Float, Float>> output = blackScholesFunction.apply(input);

        blackScholesSequentialFloat(inputf[0], callResultS, putResultS);

        double delta = 0.001;
        assertNotNull(output);

        for (int i = 0; i < size; i++) {
            assertEquals(callResultS[i], output.get(i)._1, delta);
            assertEquals(putResultS[i], output.get(i)._2, delta);
        }
    }

    public void blackScholesSequentialFloat(Float rand, float[] callResult, float[] putResult) {
        float inRand = rand;
        float sLOWERLIMIT = 10.0f;
        float sUPPERLIMIT = 100.0f;
        float kLOWERLIMIT = 10.0f;
        float kUPPERLIMIT = 100.0f;
        float tLOWERLIMIT = 1.0f;
        float tUPPERLIMIT = 10.0f;
        float rLOWERLIMIT = 0.01f;
        float rUPPERLIMIT = 0.05f;
        float sIGMALOWERLIMIT = 0.01f;
        float sIGMAUPPERLIMIT = 0.10f;
        float s = sLOWERLIMIT * inRand + sUPPERLIMIT * (1.0f - inRand);
        float k = kLOWERLIMIT * inRand + kUPPERLIMIT * (1.0f - inRand);
        float t = tLOWERLIMIT * inRand + tUPPERLIMIT * (1.0f - inRand);
        float r = rLOWERLIMIT * inRand + rUPPERLIMIT * (1.0f - inRand);
        float v = sIGMALOWERLIMIT * inRand + sIGMAUPPERLIMIT * (1.0f - inRand);

        for (int i = 0; i < callResult.length; i++) {
            float d1 = (OCLMath.log(s / k) + ((r + (v * v / 2)) * t)) / v * OCLMath.sqrt(t);
            float d2 = d1 - (v * OCLMath.sqrt(t));
            callResult[i] = s * cnd(d1) - k * OCLMath.exp(t * (-1) * r) * cnd(d2);
            putResult[i] = k * OCLMath.exp(t * (-1) * r) - cnd(-d2) - s * cnd(-d1);
        }
    }

    // This method has to be static
    public static float cnd(float x) {
        float l;
        float k;
        float w;
        float a1 = 0.319381530f;
        float a2 = -0.356563782f;
        float a3 = 1.781477937f;
        float a4 = -1.821255978f;
        float a5 = 1.330274429f;
        float a6 = 2.506628273f;
        l = OCLMath.fabs(x);
        k = 1.0f / (1.0f + 0.2316419f * l);
        w = 1.0f - 1.0f / 1 * a6 * OCLMath.exp((-1 * l) * l / 2) * (a1 * k + a2 * k * k * 1 + a3 * k * k * k * +a4 * k * k * k * k * 1 + a5 * k * k * k * k * k);
        float result;
        if (x < 0) {
            result = 1.0f - w;
        } else {
            result = w;
        }
        return result;
    }

    @Test
    public void monteCarlo() {

        int size = 100;

        PArray<Float> input = new PArray<>(size, new RuntimeObjectTypeInfo(Float.class));

        float[] inputSeq = new float[size];

        for (int i = 0; i < input.size(); i++) {
            input.put(i, (float) i);
            inputSeq[i] = i;
        }

        // @formatter:off
        ArrayFunction<Float, Float> blackScholesFunction = new MapAccelerator<>(z -> {
            float idxf = z;
            int idx = (int) idxf;
            int iter = 25000;

            long seed = idx;
            float sum = 0.0f;

            for (int j = 0; j < iter; ++j) {
                // generate a pseudo random number (you do need it twice)
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);

                // this generates a number between 0 and 1 (with an awful entropy)
                float x = (seed & 0x0FFFFFFF) / 268435455f;

                // repeat for y
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
                float y = (seed & 0x0FFFFFFF) / 268435455f;

                float dist = OCLMath.sqrt(x * x + y * y);
                if (dist <= 1.0f) {
                    sum += 1.0f;
                }
            }
            sum *= 4;
            return sum / iter;
        });
        // @formatter:on

        PArray<Float> output = blackScholesFunction.apply(input);

        ArrayFunction<Float, Float> reduce = Marawacc.reduce((x, y) -> x + y, 1.0f);
        PArray<Float> apply = reduce.apply(output);
        System.out.println(output);
        float r = apply.get(0) / size;
        System.out.println(r);

        Float[] sequential = monteCarloSeq(inputSeq);
        for (int i = 0; i < sequential.length; i++) {
            assertEquals(sequential[i], output.get(i), 0.1);
        }

    }

    private static Float[] monteCarloSeq(float[] input) {
        Float[] result = new Float[input.length];
        int total = input.length;

        for (int idx = 0; idx < total; idx++) {
            int iter = 25000;

            long seed = idx;
            float sum = 0.0f;

            for (int j = 0; j < iter; ++j) {
                // generate a pseudo random number (you do need it twice)
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);

                // this generates a number between 0 and 1 (with an awful entropy)
                float x = (seed & 0x0FFFFFFF) / 268435455f;

                // repeat for y
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
                seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
                float y = (seed & 0x0FFFFFFF) / 268435455f;

                float dist = OCLMath.sqrt(x * x + y * y);
                if (dist <= 1.0f) {
                    sum += 1.0f;
                }
            }

            sum *= 4;
            result[idx] = sum / iter;
        }
        return result;
    }

    @Test
    @Ignore
    public void nbodyDouble() {

        double deltaTS = 0.005f;
        double espSqrS = 500.0f;
        int numBodiesS = 100;

        PArray<Tuple9<Double, Double, Double, Double, Double, Double, Double, Double, Integer>> input = new PArray<>(numBodiesS,
                        TypeFactory.Tuple("Tuple9<Double, Double, Double, Double, Double, Double, Double, Double, Integer>"));

        double[] auxPositionRandom = new double[numBodiesS * 4];
        double[] auxVelocityZero = new double[numBodiesS * 3];

        for (int i = 0; i < auxPositionRandom.length; i++) {
            auxPositionRandom[i] = Math.random();
        }
        Arrays.fill(auxVelocityZero, 0.0);

        for (int i = 0; i < numBodiesS; i++) {
            int body = i * 4;
            input.put(i, new Tuple9<>(auxPositionRandom[body], auxPositionRandom[body + 1], auxPositionRandom[body + 2], 0.0, 0.0, 0.0, deltaTS, espSqrS, numBodiesS));
        }
        double[] posSeq = new double[numBodiesS * 4];
        double[] velSeq = new double[numBodiesS * 4];

        for (int i = 0; i < auxPositionRandom.length; i++) {
            posSeq[i] = auxPositionRandom[i];
        }
        for (int i = 0; i < auxVelocityZero.length; i++) {
            velSeq[i] = auxVelocityZero[i];
        }

        ArrayFunction<Tuple9<Double, Double, Double, Double, Double, Double, Double, Double, Integer>, Tuple6<Double, Double, Double, Double, Double, Double>> function = new MapAccelerator<>(x -> {
            double px = x._1();
            double py = x._2();
            double pz = x._3();
            double vx = x._4();
            double vy = x._5();
            double vz = x._6();
            double deltaT = x._7();
            double espSqr = x._8();
            int numBodies = x._9();

            double[] acc = new double[]{0.0f, 0.0f, 0.0f};
            for (int j = 0; j < numBodies; j++) {
                int body = 4 * j;

                double[] r = new double[3];
                double distSqr = 0.0f;

                r[0] = auxPositionRandom[body] - px;
                r[1] = auxPositionRandom[body + 1] - py;
                r[2] = auxPositionRandom[body + 2] - pz;

                for (int k = 0; k < 3; k++) {
                    distSqr += r[k] * r[k];
                }

                double invDist = (1.0f / OCLMath.sqrt(distSqr + espSqr));

                double invDistCube = invDist * invDist * invDist;
                double s = auxPositionRandom[body + 3] * invDistCube;

                for (int k = 0; k < 3; k++) {
                    acc[k] += s * r[k];
                }
            }

            px += vx * deltaT + 0.5f * acc[0] * deltaT * deltaT;
            py += vy * deltaT + 0.5f * acc[1] * deltaT * deltaT;
            pz += vz * deltaT + 0.5f * acc[2] * deltaT * deltaT;

            vx += acc[0] * deltaT;
            vy += acc[1] * deltaT;
            vz += acc[2] * deltaT;

            Tuple6<Double, Double, Double, Double, Double, Double> t6 = new Tuple6<>();

            t6._1(px);
            t6._2(py);
            t6._3(pz);
            t6._4(vx);
            t6._5(vy);
            t6._6(vz);

            return t6;
        });

        PArray<Tuple6<Double, Double, Double, Double, Double, Double>> output = function.apply(input);
        assertNotNull(output);

        nBodySequential(numBodiesS, posSeq, velSeq, deltaTS, espSqrS); // Sequential execution
        checkResultDouble(numBodiesS, posSeq, velSeq, output);
    }

    @Test
    public void nbody() {

        float deltaTS = 0.005f;
        float espSqrS = 500.0f;
        int numBodiesS = 100;

        PArray<Tuple9<Float, Float, Float, Float, Float, Float, Float, Float, Integer>> input = new PArray<>(numBodiesS, new Tuple9<>(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0).getType());

        float[] auxPositionRandom = new float[numBodiesS * 4];
        float[] auxVelocityZero = new float[numBodiesS * 3];

        for (int i = 0; i < auxPositionRandom.length; i++) {
            auxPositionRandom[i] = (float) Math.random();
        }
        Arrays.fill(auxVelocityZero, 0.0f);

        for (int i = 0; i < numBodiesS; i++) {
            int body = i * 4;
            input.put(i, new Tuple9<>(auxPositionRandom[body], auxPositionRandom[body + 1], auxPositionRandom[body + 2], 0.0f, 0.0f, 0.0f, deltaTS, espSqrS, numBodiesS));
        }
        float[] posSeq = new float[numBodiesS * 4];
        float[] velSeq = new float[numBodiesS * 4];

        for (int i = 0; i < auxPositionRandom.length; i++) {
            posSeq[i] = auxPositionRandom[i];
        }
        for (int i = 0; i < auxVelocityZero.length; i++) {
            velSeq[i] = auxVelocityZero[i];
        }

        ArrayFunction<Tuple9<Float, Float, Float, Float, Float, Float, Float, Float, Integer>, Tuple6<Float, Float, Float, Float, Float, Float>> function = new MapAccelerator<>(x -> {
            float px = x._1();
            float py = x._2();
            float pz = x._3();
            float vx = x._4();
            float vy = x._5();
            float vz = x._6();
            float deltaT = x._7();
            float espSqr = x._8();
            int numBodies = x._9();

            float[] acc = new float[]{0.0f, 0.0f, 0.0f};
            for (int j = 0; j < numBodies; j++) {
                int body = 4 * j;

                float[] r = new float[3];
                float distSqr = 0.0f;

                r[0] = auxPositionRandom[body] - px;
                r[1] = auxPositionRandom[body + 1] - py;
                r[2] = auxPositionRandom[body + 2] - pz;

                for (int k = 0; k < 3; k++) {
                    distSqr += r[k] * r[k];
                }

                float invDist = (1.0f / OCLMath.sqrt(distSqr + espSqr));

                float invDistCube = invDist * invDist * invDist;
                float s = auxPositionRandom[body + 3] * invDistCube;

                for (int k = 0; k < 3; k++) {
                    acc[k] += s * r[k];
                }
            }

            px += vx * deltaT + 0.5f * acc[0] * deltaT * deltaT;
            py += vy * deltaT + 0.5f * acc[1] * deltaT * deltaT;
            pz += vz * deltaT + 0.5f * acc[2] * deltaT * deltaT;

            vx += acc[0] * deltaT;
            vy += acc[1] * deltaT;
            vz += acc[2] * deltaT;

            Tuple6<Float, Float, Float, Float, Float, Float> t6 = new Tuple6<>();

            t6._1(px);
            t6._2(py);
            t6._3(pz);
            t6._4(vx);
            t6._5(vy);
            t6._6(vz);

            return t6;
        });

        PArray<Tuple6<Float, Float, Float, Float, Float, Float>> output = function.apply(input);

        assertNotNull(output);

        nBodySequential(numBodiesS, posSeq, velSeq, deltaTS, espSqrS); // Sequential execution
        checkResult(numBodiesS, posSeq, velSeq, output);

    }

    private static void checkResult(int numBodies, float[] posSeq, float[] velSeq, PArray<Tuple6<Float, Float, Float, Float, Float, Float>> result) {
        float delta = 0.001f;
        for (int i = 0; i < numBodies; i++) {
            int body = 4 * i;
            int vbody = 3 * i;
            // pos: x,y,z
            assertEquals(posSeq[body], result.get(i)._1, delta);
            assertEquals(posSeq[body + 1], result.get(i)._2, delta);
            assertEquals(posSeq[body + 2], result.get(i)._3, delta);

            // vel: x,y,z
            assertEquals(velSeq[vbody], result.get(i)._4, delta);
            assertEquals(velSeq[vbody + 1], result.get(i)._5, delta);
            assertEquals(velSeq[vbody + 2], result.get(i)._6, delta);
        }
    }

    private static void checkResultDouble(int numBodies, double[] posSeq, double[] velSeq, PArray<Tuple6<Double, Double, Double, Double, Double, Double>> result) {
        double delta = 0.001f;
        for (int i = 0; i < numBodies; i++) {
            int body = 4 * i;
            int vbody = 3 * i;
            // pos: x,y,z
            assertEquals(posSeq[body], result.get(i)._1, delta);
            assertEquals(posSeq[body + 1], result.get(i)._2, delta);
            assertEquals(posSeq[body + 2], result.get(i)._3, delta);

            // vel: x,y,z
            assertEquals(velSeq[vbody], result.get(i)._4, delta);
            assertEquals(velSeq[vbody + 1], result.get(i)._5, delta);
            assertEquals(velSeq[vbody + 2], result.get(i)._6, delta);
        }
    }

    private static void nBodySequential(int numBodies, float[] refPos, float[] refVel, float delT, float espSqr) {
        for (int i = 0; i < numBodies; i++) {
            int body = 4 * i;
            float[] acc = new float[]{0.0f, 0.0f, 0.0f};
            for (int j = 0; j < numBodies; j++) {
                float[] r = new float[3];
                int index = 4 * j;

                float distSqr = 0.0f;
                for (int k = 0; k < 3; k++) {
                    r[k] = refPos[index + k] - refPos[body + k];
                    distSqr += r[k] * r[k];
                }

                float invDist = (float) (1.0f / Math.sqrt(distSqr + espSqr));

                float invDistCube = invDist * invDist * invDist;
                float s = refPos[index + 3] * invDistCube;

                for (int k = 0; k < 3; k++) {
                    acc[k] += s * r[k];
                }
            }
            for (int k = 0; k < 3; k++) {
                refPos[body + k] += refVel[body + k] * delT + 0.5f * acc[k] * delT * delT;
                refVel[body + k] += acc[k] * delT;
            }
        }
    }

    private static void nBodySequential(int numBodies, double[] refPos, double[] refVel, double delT, double espSqr) {
        for (int i = 0; i < numBodies; i++) {
            int body = 4 * i;
            double[] acc = new double[]{0.0f, 0.0f, 0.0f};
            for (int j = 0; j < numBodies; j++) {
                double[] r = new double[3];
                int index = 4 * j;

                double distSqr = 0.0f;
                for (int k = 0; k < 3; k++) {
                    r[k] = refPos[index + k] - refPos[body + k];
                    distSqr += r[k] * r[k];
                }

                double invDist = (1.0f / Math.sqrt(distSqr + espSqr));

                double invDistCube = invDist * invDist * invDist;
                double s = refPos[index + 3] * invDistCube;

                for (int k = 0; k < 3; k++) {
                    acc[k] += s * r[k];
                }
            }
            for (int k = 0; k < 3; k++) {
                refPos[body + k] += refVel[body + k] * delT + 0.5f * acc[k] * delT * delT;
                refVel[body + k] += acc[k] * delT;
            }
        }
    }

    @Test
    public void centroid() {
        // final int ks = 10;
        int numPoints = 1000;
        final int ks = 10;
        final double range = 10;

        float[] centres1D = new float[ks * 2];
        float[][] points = new float[numPoints][2];

        for (int i = 0; i < ks; i++) {
            centres1D[i] = (float) (Math.random() * range * 2 - range);
            centres1D[i + ks] = (float) (Math.random() * range * 2 - range);
        }

        for (int i = 0; i < ks; i++) {
            points[i][0] = (float) (Math.random() * range * 2 - range);
            points[i][1] = (float) (Math.random() * range * 2 - range);
        }

        PArray<Tuple2<Float, Float>> input = new PArray<>(numPoints, new Tuple2<>(0.0f, 0.0f).getType());

        for (int i = 0; i < input.size(); i++) {
            input.put(i, new Tuple2<>(points[i][0], points[i][1]));
        }

        ArrayFunction<Tuple2<Float, Float>, Integer> kmeansFunction = new MapAccelerator<>(x -> {
            float minDist = Float.MAX_VALUE;
            int id = -1;
            int k = ks;
            for (int i = 0; i < k; i++) {
                float currentDist = (x._1() - centres1D[i]) * (x._1() - centres1D[i]) + (x._2() - centres1D[i + k]) * (x._2() - centres1D[i + k]);
                if (currentDist < minDist) {
                    minDist = currentDist;
                    id = i;
                }
            }
            return id;
        });

        int[] resultSeq = centroidSequentialKernel(points, centres1D);

        int j = 0;
        do {
            PArray<Integer> output = kmeansFunction.apply(input);
            assertNotNull(output);
            for (int i = 0; i < resultSeq.length; i++) {
                assertEquals(resultSeq[i], output.get(i).intValue());
            }
        } while (j++ < 10);
    }

    private static int[] fullKMeansSequential(float[][] points, float[] centres) {

        float[] newCentres = new float[centres.length];
        int[] numPoints = new int[centres.length];

        int[] membership = new int[points.length];
        int[] newMembership = new int[points.length];

        int diff;

        do {
            diff = 0;
            // Assign points to clusters
            for (int i = 0; i < points.length; i++) {

                float minDist = Float.MAX_VALUE;
                int id = -1;

                for (int j = 0; j < centres.length / 2; j++) {

                    float currentDist = 0.0f;

                    for (int k = 0; k < 2; k++) {
                        currentDist += (points[i][k] - centres[j + k]) * (points[i][k] - centres[j + k]);
                    }

                    if (currentDist < minDist) {
                        minDist = currentDist;
                        id = j;
                    }

                }

                newMembership[i] = id;
            }

            // Calculate the new cluster centres
            for (int i = 0; i < points.length; i++) {
                int id = newMembership[i];
                numPoints[id]++;
                if (newMembership[i] != membership[i]) {
                    diff++;
                    membership[i] = id;
                }

                for (int j = 0; j < points[i].length; j++) {
                    newCentres[id + j] += points[i][j];
                }
            }

            for (int i = 0; i < centres.length / 2; i++) {
                for (int j = 0; j < centres.length / 2; j++) {
                    if (numPoints[i] != 0) {
                        centres[i + j] = newCentres[i + j] / numPoints[i];
                        newCentres[i + j] = 0.0f;
                    }
                }
                numPoints[i] = 0;
            }

        } while (diff != 0);

        return membership;
    }

    private static int[] centroidSequentialKernel(float[][] points, float[] centres) {

        int[] newMembership = new int[points.length];

        int k = centres.length / 2;

        // Assign points to clusters
        for (int i = 0; i < points.length; i++) {

            float minDist = Float.MAX_VALUE;
            int id = -1;

            for (int j = 0; j < centres.length / 2; j++) {
                float currentDist = (points[i][0] - centres[j]) * (points[i][0] - centres[j]) + (points[i][1] - centres[j + k]) * (points[i][1] - centres[j + k]);
                if (currentDist < minDist) {
                    minDist = currentDist;
                    id = j;
                }
            }
            newMembership[i] = id;
        }

        return newMembership;
    }

    @Test
    public void fullKmeansOCL() {
        final int ks = 10;
        int numPoints = 1000;
        final double range = 10;

        float[] centres1D = new float[ks * 2];
        float[] originalCenters = new float[ks * 2];
        float[][] points = new float[numPoints][2];
        PArray<Integer> membership = new PArray<>(numPoints, TypeFactory.Integer());

        for (int i = 0; i < ks; i++) {
            centres1D[i] = (float) (Math.random() * range * 2 - range);
            centres1D[i + ks] = (float) (Math.random() * range * 2 - range);
            originalCenters[i] = centres1D[i];
            originalCenters[i + ks] = centres1D[i + ks];
        }

        for (int i = 0; i < ks; i++) {
            points[i][0] = (float) (Math.random() * range * 2 - range);
            points[i][1] = (float) (Math.random() * range * 2 - range);
        }

        PArray<Tuple2<Float, Float>> input = new PArray<>(numPoints, new Tuple2<>(0.0f, 0.0f).getType());

        for (int i = 0; i < input.size(); i++) {
            input.put(i, new Tuple2<>(points[i][0], points[i][1]));
        }

        ArrayFunction<Tuple2<Float, Float>, Integer> kmeansFunction = new MapAccelerator<>(x -> {
            float minDist = Float.MAX_VALUE;
            int id = -1;
            int k = ks;

            for (int i = 0; i < k; i++) {
                float currentDist = (x._1() - centres1D[i]) * (x._1() - centres1D[i]) + (x._2() - centres1D[i + k]) * (x._2() - centres1D[i + k]);
                if (currentDist < minDist) {
                    minDist = currentDist;
                    id = i;
                }
            }
            return id;
        });

        int size = numPoints;

        int diff = 0;
        do {
            int[] numPointsA = new int[size];
            float[] newCentres = new float[centres1D.length];
            diff = 0;

            PArray<Integer> newMembership = kmeansFunction.apply(input);

            // Calculate new cluster centres
            for (int i = 0; i < input.size(); i++) {
                int id = newMembership.get(i);
                numPointsA[id]++;

                if (newMembership.get(i) != membership.get(i)) {
                    diff++;
                    membership.put(i, id);
                }
                newCentres[id] += input.get(i)._1;
                newCentres[id + ks] += input.get(i)._2;
            }

            for (int i = 0; i < centres1D.length; i++) {
                if (numPointsA[i] != 0) {
                    centres1D[i] = newCentres[i] / numPointsA[i];
                    centres1D[i + ks] = newCentres[i + ks] / numPointsA[i];
                }
            }
            // membership = newMembership;

        } while (diff != 0);

        assertNotNull(membership);
        int[] resultSeq = fullKMeansSequential(points, originalCenters);

        assertEquals(resultSeq.length, membership.size());

        for (int i = 0; i < resultSeq.length; i++) {
            // assertEquals(resultSeq[i], membership.get(i).intValue());
        }
    }

    public static double f(double x) {
        return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
    }

    public static double integrateSeq(double a, double b, int N) {
        double h = (b - a) / N;
        double sum = 0.5 * (f(a) + f(b));

        for (int i = 1; i < N; i++) {
            double x = a + h * i;
            sum += f(x);
        }
        return sum * h;
    }

    @Test
    public void integration() {

        final int N = 100000;
        int points = N / 100;

        ArrayFunction<Tuple4<Double, Double, Integer, Integer>, Double> partialIntegration = new MapAccelerator<>(x -> {
            double a = x._1();
            double b = x._2();
            int inf = x._3();
            int sup = x._4();
            double h = (b - a) / N;
            double auxa = (-a * a) / 2;
            a = OCLMath.exp(auxa) / OCLMath.sqrt(2 * Math.PI);
            b = OCLMath.exp(-b * b / 2) / OCLMath.sqrt(2 * Math.PI);
            double sum = 0.5 * (a + b);

            for (int i = inf; i < sup; i++) {
                double xx = a + h * i;
                sum += OCLMath.exp(-xx * xx / 2) / OCLMath.sqrt(2 * Math.PI);
            }
            return sum * h;
        });

        PArray<Tuple4<Double, Double, Integer, Integer>> input = new PArray<>(points, TypeFactory.Tuple("Tuple4<Double, Double, Integer, Integer>"));
        // Format : <a , b , inf, sup>
        for (int i = 0; i < points; i++) {
            input.put(i, new Tuple4<>(0.0, 10.0, i * points, (i * points) + points));
        }

        PArray<Double> result = partialIntegration.apply(input);

        assertNotNull(result);

        double resultSeq = integrateSeq(0, 10.0, N);

        double sum = 0.0;
        for (int i = 0; i < points; i++) {
            sum += result.get(i);
        }

        assertEquals(resultSeq, sum, 1.0);
    }

    /* return element i,j of infinite matrix A */
    private final static double eval_A(int i, int j) {
        int div = (((i + j) * (i + j + 1) >>> 1) + i + 1);
        return 1.0 / div;
    }

    @Test
    public void spectralNorm() {
        // From Programming Language Benchmark

        final int size = 1000;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        final double[] v = new double[size];
        Arrays.fill(v, 1.0);
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        Function<Integer, Tuple2<Integer, Double>> f1 = (x -> {
            int i = x;
            double sum = 0;
            for (int j = 0; j < size; j++) {
                sum += eval_A(i, j) * v[j];
            }
            Tuple2<Integer, Double> t = new Tuple2<>();
            t._1(i);
            t._2(sum);
            return t;
        });

        Function<Tuple2<Integer, Double>, Double> f2 = (x -> {
            int i = x._1;
            double sum = 0;
            for (int j = 0; j < size; j++) {
                sum += eval_A(j, i) * v[j];
            }
            return sum;
        });

        ArrayFunction<Integer, Tuple2<Integer, Double>> mapAccelerator = new MapAccelerator<>(f1);
        PArray<Tuple2<Integer, Double>> resultA = mapAccelerator.apply(input);
        // Update v
        for (int i = 0; i < size; i++) {
            v[i] = resultA.get(i)._2;
        }
        PArray<Double> result = Marawacc.mapAccelerator(f2).apply(resultA);
        assertNotNull(result);
    }

    @Test
    public void dft() {

        final int size = 100;

        double[] inreal = new double[size];
        double[] inimag = new double[size];

        IntStream.range(0, size).parallel().forEach(i -> {
            inreal[i] = Math.random();
            inimag[i] = Math.random();
        });

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        MapAccelerator<Integer, Tuple2<Double, Double>> function = new MapAccelerator<>(x -> {

            int n = size;
            double sumreal = 0;
            double sumimag = 0;
            for (int t = 0; t < n; t++) {
                double angle = 2 * Math.PI * t * x / n;
                sumreal += inreal[t] * Math.cos(angle) + inimag[t] * Math.sin(angle);
                sumimag += -inreal[t] * Math.sin(angle) + inimag[t] * Math.cos(angle);
            }
            Tuple2<Double, Double> t = new Tuple2<>();
            t._1(sumreal);
            t._2(sumimag);
            return t;
        });

        PArray<Tuple2<Double, Double>> result = function.apply(input);

        assertNotNull(result);
    }

    @Test
    @Ignore
    public void eulerConjeture() {
        final int n = 500;
        int size = n;

        PArray<Integer> input = new PArray<>(size + 1, TypeFactory.Integer());

        // precompute i^4 for i = 0..n
        IntStream.range(0, n).parallel().forEach(i -> {
            int comp = (i + 1) * (i + 1) * (i + 1) * (i + 1);
            input.put(i, comp);
        });

        MapAccelerator<Integer, Tuple4<Long, Long, Long, Long>> function = new MapAccelerator<>(x -> {
            long d4 = x;
            Tuple4<Long, Long, Long, Long> t = new Tuple4<>();
            t._1(0.0);
            t._2(0.0);
            t._3(0.0);
            t._4(0.0);
            for (int a = 0; a < n; a++) {
                long a4 = input.get(a);
                for (int b = a; b < n; b++) {
                    long b4 = input.get(b);
                    for (int c = b; c < n; c++) {
                        long c4 = input.get(c);
                        long comp = a4 + b4 + c4;
                        if (comp == d4) {
                            t._1(a4);
                            t._2(b4);
                            t._3(c4);
                            t._4(d4);
                        }
                    }
                }
            }
            return t;
        });

        PArray<Tuple4<Long, Long, Long, Long>> result = function.apply(input);
        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void piEstimation() {
        // based on spark examples
        int size = 10000;
        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        MapAccelerator<Integer, Integer> function = new MapAccelerator<>(in -> {
            long seed = in;
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);

            float x = (seed & 0x0FFFFFFF) / 268435455f;

            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            float y = (seed & 0x0FFFFFFF) / 268435455f;

            float square = (x * x + y * y);
            if (square < 1) {
                return 1;
            } else {
                return 0;
            }
        });

        PArray<Integer> result = function.reduce((x, y) -> x + y, 0).apply(input);
        assertEquals(3.1396, 4.0 * result.get(0) / size, 0.001);
    }

    private static int[] mandelbrotSequential() {
        int size = 100;
        final int iterations = 10000;
        float space = 2.0f / size;

        int[] result = new int[size * size];

        for (int i = 0; i < size; i++) {
            int indexIDX = i;
            for (int j = 0; j < size; j++) {

                int indexJDX = j;

                float Zr = 0.0f;
                float Zi = 0.0f;
                float Cr = (1 * indexJDX * space - 1.5f);
                float Ci = (1 * indexIDX * space - 1.0f);

                float ZrN = 0;
                float ZiN = 0;
                int y = 0;

                for (y = 0; y < iterations && ZiN + ZrN <= 4.0f; y++) {
                    Zi = 2.0f * Zr * Zi + Ci;
                    Zr = 1 * ZrN - ZiN + Cr;
                    ZiN = Zi * Zi;
                    ZrN = Zr * Zr;
                }
                short r = (short) ((y * 255) / iterations);
                result[i * size + j] = r;
            }
        }
        return result;
    }

    @Test
    public void mandelbrot() {

        int size = 100;
        final int iterations = 10000;
        float spaceS = 2.0f / size;

        PArray<Tuple3<Float, Integer, Integer>> input = new PArray<>(size * size, TypeFactory.Tuple("Tuple3<Float, Integer, Integer>"));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                input.put(i * size + j, new Tuple3<>(spaceS, i, j));
            }
        }

        MapAccelerator<Tuple3<Float, Integer, Integer>, Short> function = new MapAccelerator<>(x -> {
            float space = x._1();
            int indexIDX = x._2();
            int indexJDX = x._3();

            float Zr = 0.0f;
            float Zi = 0.0f;
            float Cr = (1 * indexJDX * space - 1.5f);
            float Ci = (1 * indexIDX * space - 1.0f);

            float ZrN = 0;
            float ZiN = 0;
            int y = 0;

            for (y = 0; y < iterations && ZiN + ZrN <= 4.0f; y++) {
                Zi = 2.0f * Zr * Zi + Ci;
                Zr = 1 * ZrN - ZiN + Cr;
                ZiN = Zi * Zi;
                ZrN = Zr * Zr;
            }
            short result = (short) ((y * 255) / iterations);
            return result;
        });

        PArray<Short> result = function.apply(input);
        assertNotNull(result);

        int[] sequential = mandelbrotSequential();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                assertEquals(sequential[i * size + j], result.get(i * size + j), 0.1);
            }
        }
    }

    private static float[] matrixVectorMultiplicationSequential(float[] matrix, float[] vector) {
        float[] result = new float[vector.length];
        int size = vector.length;
        for (int idx = 0; idx < size; idx++) {
            for (int jdx = 0; jdx < size; jdx++) {
                result[idx] += matrix[idx * size + jdx] * vector[jdx];
            }
        }
        return result;
    }

    @Test
    public void matrixVectorMultiplication() {

        int size = 100;
        PArray<Tuple2<Integer, Integer>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Integer, Integer>"));

        final float[] vector = new float[size];
        final float[] matrix = new float[size * size];

        Random random = new Random();

        for (int i = 0; i < size; i++) {
            input.put(i, new Tuple2<>(i, size));
            vector[i] = random.nextFloat();
            for (int j = 0; j < size; j++) {
                matrix[i * size + j] = random.nextFloat();
            }
        }

        MapAccelerator<Tuple2<Integer, Integer>, Float> function = new MapAccelerator<>(t -> {
            int idx = t._1();
            int s = t._2();

            float result = 0.0f;
            for (int jdx = 0; jdx < s; jdx++) {
                result += matrix[idx * s + jdx] * vector[jdx];
            }
            return result;
        });

        PArray<Float> result = function.apply(input);
        assertNotNull(result);
        float[] sequential = matrixVectorMultiplicationSequential(matrix, vector);
        for (int i = 0; i < size; i++) {
            assertEquals(sequential[i], result.get(i), 0.01);
        }

    }
}