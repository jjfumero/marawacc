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
package uk.ac.ed.jpai.test.graalAPI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.function.Function;

import org.junit.Test;

import uk.ac.ed.accelerator.ocl.runtime.GraalIRConversion;
import uk.ac.ed.datastructures.common.AcceleratorPArray;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.graal.GraalOpenCLCompilationUnit;
import uk.ac.ed.jpai.graal.GraalOpenCLJITCompiler;
import uk.ac.ed.jpai.graal.GraalOpenCLExecutor;

import com.oracle.graal.nodes.StructuredGraph;

/**
 * Test to compile directly to OpenCL given the Graal IR for a Method.
 */
public class TestAPIForGraalOpenCL {

    @Test
    public void test01() {

        Function<Tuple2<Double, Double>, Double> lambdaFunction = ((t) -> 2.5f * t._1() + t._2());

        int size = 100;
        PArray<Tuple2<Double, Double>> input = new PArray<>(size, new Tuple2<>(0.0, 0.0).getType());
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((double) i, (double) i + 2));
        }

        StructuredGraph graphLambda = GraalIRConversion.getOptimizedGraalIRLambda(lambdaFunction.getClass());
        assertNotNull(graphLambda);

        // Test for just compilation
        GraalOpenCLCompilationUnit gpuCompilationUnit = GraalOpenCLJITCompiler.compileGraphToOpenCL(input, graphLambda);
        assertNotNull(gpuCompilationUnit);
    }

    @Test
    public void test02() {

        Function<Tuple2<Double, Double>, Double> lambdaFunction = ((t) -> t._1() + t._2());

        int size = 100;
        PArray<Tuple2<Double, Double>> input = new PArray<>(size, new Tuple2<>(0.0, 0.0).getType());
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((double) i, (double) i + 2));
        }

        StructuredGraph graphLambda = GraalIRConversion.getOptimizedGraalIRLambda(lambdaFunction.getClass());
        assertNotNull(graphLambda);

        // Compilation
        GraalOpenCLCompilationUnit compileGraphToGPU = GraalOpenCLJITCompiler.compileGraphToOpenCL(input, graphLambda);

        // Execution
        GraalOpenCLExecutor executor = new GraalOpenCLExecutor();
        AcceleratorPArray<Tuple2<Double, Double>> copyToDevice = executor.copyToDevice(input, compileGraphToGPU.getInputType());
        AcceleratorPArray<Double> executeOnTheDevice = executor.<Tuple2<Double, Double>, Double> executeOnTheDevice(graphLambda, copyToDevice, compileGraphToGPU.getOuputType(), null);
        PArray<Double> result = executor.copyToHost(executeOnTheDevice, compileGraphToGPU.getOuputType());

        for (int i = 0; i < result.size(); ++i) {
            assertEquals(((input.get(i)._1() + input.get(i)._2())), result.get(i), 0.001);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCompilationTypesMissing() {

        Function<Tuple2<Double, Double>, Double> lambdaFunction = ((t) -> t._1() + t._2());

        int size = 100;
        PArray<Tuple2<Double, Double>> input = new PArray<>(size, new Tuple2<>(0.0, 0.0).getType());
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((double) i, (double) i + 2));
        }

        StructuredGraph graphLambda = GraalIRConversion.getOptimizedGraalIRLambda(lambdaFunction.getClass());
        assertNotNull(graphLambda);

        // Compilation
        GraalOpenCLCompilationUnit compileGraphToGPU = GraalOpenCLJITCompiler.compileGraphToOpenCL(input, graphLambda);

        GraalOpenCLExecutor executor = new GraalOpenCLExecutor();
        // Execution
        AcceleratorPArray<?> copyToDevice = executor.copyToDevice(input, compileGraphToGPU.getInputType());
        AcceleratorPArray<?> executeOnTheDevice = executor.executeOnTheDevice(graphLambda, copyToDevice, compileGraphToGPU.getOuputType(), null);
        PArray<?> result = executor.copyToHost(executeOnTheDevice, compileGraphToGPU.getOuputType());

        for (int i = 0; i < result.size(); ++i) {
            assertEquals(((input.get(i)._1() + input.get(i)._2())), ((PArray<Double>) result).get(i), 0.001);
        }
    }

    @Test
    public void testScopeNoGraal() throws IllegalArgumentException, IllegalAccessException {

        final int[] a = new int[]{100, 200, 300};

        Function<Integer, Double> lambdaFunction = (x -> x + 2.0 + a[0]);

        Field[] declaredFields = lambdaFunction.getClass().getDeclaredFields();
        Object scoped = null;
        for (int i = 0; i < declaredFields.length; i++) {
            declaredFields[i].setAccessible(true);
            scoped = declaredFields[i].get(lambdaFunction);
        }

        assertTrue(scoped instanceof int[]);
        assertEquals(a, scoped);
    }

    @Test
    public void testScopeWithGraalBasic() {

        final int[] a = new int[]{100, 200, 300};

        Function<Integer, Double> lambdaFunction = (x -> x + 2.0 + a[0]);

        StructuredGraph graphLambda = GraalIRConversion.getOptimizedGraalIRLambda(lambdaFunction.getClass());
        assertNotNull(graphLambda);

        assertEquals(graphLambda.method().toParameterTypes()[0].getName(), "[I");
        assertEquals(graphLambda.method().toParameterTypes()[1].getName(), "Ljava/lang/Integer;");
    }
}
