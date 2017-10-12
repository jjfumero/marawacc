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

package uk.ac.ed.accelerator.ocl.datastructures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType.DataType;

public final class GenerateDynamicTuples {

    public enum DIRECTION {
        IN,
        OUT
    }

    private static GenerateDynamicTuples instance = null;
    private static Set<DIRECTION> direction;
    private static boolean out;

    public static GenerateDynamicTuples getInstance() {
        if (instance == null) {
            instance = new GenerateDynamicTuples();
        }
        return instance;
    }

    private GenerateDynamicTuples() {
        direction = new HashSet<>();
        out = false;
    }

    @SuppressWarnings("static-method")
    public synchronized void setDirection(DIRECTION d) {
        direction.add(d);
    }

    @SuppressWarnings("static-method")
    public synchronized void setOutput() {
        out = true;
    }

    @SuppressWarnings("static-method")
    public boolean isGenerated() {
        if (out) {
            for (DIRECTION d : DIRECTION.values()) {
                if (!direction.contains(d)) {
                    return false;
                }
            }
        } else if (!direction.contains(DIRECTION.IN)) {
            return false;
        }
        return true;
    }

    private static String prepareDataTemplate(ArrayList<AcceleratorType> typesList) {
        String template = "public ";
        String finalData = "";
        for (int i = 0; i < typesList.size(); i++) {
            AcceleratorType typeElement = typesList.get(i);
            DataType dataType = typesList.get(i).getType();
            String nameType = "";
            if (typeElement.isValid()) {
                nameType = dataType.getOCLName();
                finalData += template + " " + nameType + " _" + (i + 1) + "; \n\t";
            }
        }
        return finalData;
    }

    @SuppressWarnings("unused")
    private static void checkObject(int n) {
        // Method for debugging
        try {
            URL[] myurl = {new URL("file://" + System.getProperty("user.dir") + "/tmp/dynamic/generation/")};
            URLClassLoader x = new URLClassLoader(myurl);

            Class<?> klass = x.loadClass("Tuple" + n);
            Object object = klass.newInstance();

            // Field access
            Field f = klass.getField("_2");

            // Default value
            System.out.println(f.get(object));

        } catch (IllegalAccessException e) {
        } catch (RuntimeException e) {
        } catch (Exception e) {
        }
    }

    private static void compilerToByteCode(File sourceFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File("./tmp/dynamic/generation")));

        // Compile the file
        compiler.getTask(null, fileManager, null, null, null, fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile))).call();
        fileManager.close();

        // checkObject(n); // for testing

        // delete the source file
        // sourceFile.deleteOnExit();
    }

    private static String fillTemplate(ArrayList<AcceleratorType> typesList, String sourceCode, int n) {

        String dataSource = prepareDataTemplate(typesList);

        String newCode = sourceCode.replace("<int>", (new Integer(n).toString()));
        newCode = newCode.replace("<access_template>", dataSource);

        if (GraalAcceleratorOptions.templateVerbose) {
            System.out.println(sourceCode);
        }
        return newCode;
    }

    public static void generateTuple(ArrayList<AcceleratorType> typesList, int counter, DIRECTION d) throws IOException {

        direction.add(d);
        if (counter == 0) {
            return;
        }

        int n = counter;

        // Create a new directory
        (new File("./tmp/dynamic/generation")).mkdirs();

        // create the source
        // From eclipse the directory is created in the wrong path

        File sourceFile = new File("./tmp/dynamic/generation/Tuple" + n + ".java");
        FileWriter writer = new FileWriter(sourceFile);

        // @formatter:off
        String sourceCode =
                        "// Autogenerated Tuple \n" +
                        //"package tmp.dynamic.generation;\n" +
                        "public class Tuple<int> { \n" +
                        "\t<access_template>"
                        + "}";
        // @formatter:on

        sourceCode = fillTemplate(typesList, sourceCode, n);

        writer.write(sourceCode);
        writer.close();

        compilerToByteCode(sourceFile);
    }
}
