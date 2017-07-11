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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package uk.ac.ed.jpai.samples;

import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.MarawaccOptions;

/**
 * Simple example of how to use the GPU from JPAI and manage efficient data transfer to the GPU via
 * {@link PArray}.
 *
 * Execute the JVM with the following options to see the auto-generated kernel and OpenCL stats:
 *
 * -jvmci -Dmarawacc.printOCLKernel=true -XX:-BootstrapJVMCI -XX:-UseJVMCIClassLoader
 * -Dmarawacc.printOCLInfo=true
 *
 */
public class HelloMarawacc {

    private static final int ITERATIONS = 11;

    public static void main(String[] args) throws Exception {

        // We force the GPU execution, if not, an exception will be launched.
        // This flag is true by default, which means that if there is any exception
        // in OpenCL, we catch it and run the Java Thread applications instead.
        MarawaccOptions.DEOPTIMIZE = true;

        // Function creation - It defines the computation in the lambda expression
        // The following function computes the DAXPY problem.
        ArrayFunction<Tuple2<Double, Double>, Double> computation = new MapAccelerator<>(vectors -> 2.5f * vectors._1() + vectors._2());

        // Prepare the input data
        int size = 262144;
        PArray<Tuple2<Double, Double>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Double, Double>"));
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((double) i, (double) i + 2));
        }

        // Check the code cache system. Only in the first iteration, the code is generated. Then is
        // got from a shared cache.
        boolean resultIsCorrect = true;
        boolean testIsCorrect = true;
        for (int j = 0; j < ITERATIONS; j++) {
            System.out.println("\tRunning iteration : " + j);

            // For profile all the metrics (OpenCL + Java)
            Profiler.getInstance().print("\nRunning iteration : " + j);

            // Execution on the OpenCL device (eg. GPU)
            PArray<Double> output = computation.apply(input);

            // Check result
            resultIsCorrect = true;
            for (int i = 0; i < output.size(); ++i) {
                if (output.get(i) != (2.5f * input.get(i)._1 + input.get(i)._2)) {
                    System.out.println("EXPECTED: " + (2.5f * input.get(i)._1 + input.get(i)._2) + "  -- GOT: " + output.get(i) + " --> index : " + i);
                    resultIsCorrect = false;
                    break;
                }
            }

            testIsCorrect &= resultIsCorrect;

            if (!resultIsCorrect) {
                System.out.println("Result is not correct");
            }
        }

        if (testIsCorrect) {
            System.out.println("Test correct");
        }

        // Print the profile information if enabled (-Dmarawacc.printOCLInfo=true)
        Profiler.getInstance().printMediansOCLEvents();
    }
}
