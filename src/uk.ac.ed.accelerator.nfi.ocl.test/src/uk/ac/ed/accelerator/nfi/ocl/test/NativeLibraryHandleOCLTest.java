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
package uk.ac.ed.accelerator.nfi.ocl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;

import org.junit.Assume;
import org.junit.Test;

import com.oracle.nfi.NativeFunctionInterfaceRuntime;
import com.oracle.nfi.api.NativeFunctionHandle;
import com.oracle.nfi.api.NativeFunctionInterface;
import com.oracle.nfi.api.NativeLibraryHandle;

public class NativeLibraryHandleOCLTest {

    public final NativeFunctionInterface nfi = NativeFunctionInterfaceRuntime.getNativeFunctionInterface();

    private static String getOpenCLLibPath() {
        String path = "/usr/lib64/libOpenCL.so";
        Assume.assumeTrue(new File(path).exists());
        return path;
    }

    @SuppressWarnings("unused")
    @Test
    public void test() {

        // Explore OpenCL with GNFI

        assert nfi != null;

        assertNotNull(nfi);

        NativeLibraryHandle oclLib = nfi.getLibraryHandle(getOpenCLLibPath());
        if (oclLib != null) {
            System.out.println("OpenCL test");
            System.out.println(oclLib.getName());
            NativeFunctionHandle clGetPlatformIDs = nfi.getFunctionHandle(oclLib, "clGetPlatformIDs", int.class, int.class, int[].class, long[].class);
            System.out.println(clGetPlatformIDs.toString());
            long[] platform = new long[1];
            int[] status = new int[1];
            int stat = -1;
            stat = (int) clGetPlatformIDs.call(1, status, platform);
            assertEquals(0, stat);
            System.out.println(Arrays.toString(platform));

            long[] platforms = new long[(int) platform[0]];
            NativeFunctionHandle clGetPlatformIDs2 = nfi.getFunctionHandle(oclLib, "clGetPlatformIDs", int.class, long.class, long[].class, int[].class);
            stat = (int) clGetPlatformIDs2.call(platform[0], platforms, new int[]{});
            assertEquals(0, stat);
            System.out.println("PLATFORMS: " + Arrays.toString(platforms));

            NativeFunctionHandle clGetPlatformInfo = nfi.getFunctionHandle(oclLib, "clGetPlatformInfo", int.class, long[].class, long[].class, long.class, char[].class, long[].class);

            char[] buffer = new char[100];
            int clPlatformVendor = 0x0903;

            // Print information about the platform
            for (int i = 0; i < platforms.length; i++) {
                // clGetPlatformInfo.call(new long[]{platforms[i]}, new long[]{CL_PLATFORM_VENDOR},
                // buffer.length, buffer, new long[]{});
                System.out.println(Arrays.toString(buffer));
            }
        }
    }
}
