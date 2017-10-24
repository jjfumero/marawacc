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
package uk.ac.ed.accelerator.common;

import uk.ac.ed.accelerator.common.GraalAcceleratorOptions.AcceleratorPlatformKind;
import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorPlatform;

/**
 * Graal Accelerator Platform. It is totally independent of the heterogeneous programming model and
 * technology.
 *
 */
public final class GraalAcceleratorSystem {

    private static GraalAcceleratorSystem instance = null;

    private GraalAcceleratorPlatform graalAccPlatform = null;

    private static boolean isInitialized = false;
    private static boolean initializating = false;

    public static GraalAcceleratorSystem getInstance() {
        if (instance == null) {
            instance = new GraalAcceleratorSystem();
            isInitialized = true;
        }
        return instance;
    }

    private GraalAcceleratorSystem() {

        initializating = true;

        if (GraalAcceleratorOptions.printOCLInfo) {
            System.out.println("[OCL GRAAL] Initializating platform .... ");
        }

        if (GraalAcceleratorOptions.DEFAULT_ACCELERATOR_PLATFORM == AcceleratorPlatformKind.OPENCL) {
            this.graalAccPlatform = new OCLGraalAcceleratorPlatform();
        } else if (GraalAcceleratorOptions.DEFAULT_ACCELERATOR_PLATFORM == AcceleratorPlatformKind.CUDA) {
            throw new UnsupportedOperationException("CUDA platform not supported yet");
        } else {
            throw new UnsupportedOperationException("Unknown platform");
        }

        initializating = false;
    }

    /**
     * Method to avoid race conditions in the initialisation process. This method has to be called
     * by the multiple threads in the initialisation.
     *
     * @return boolean
     */
    @SuppressWarnings("static-method")
    public boolean isSystemInitialized() {
        return isInitialized;
    }

    /**
     * Method to by another thread if the main one is in the initialisation process.
     *
     * @return boolean
     */
    @SuppressWarnings("static-method")
    public boolean isInitializating() {
        return initializating;
    }

    /**
     *
     * @return GraalAcceleratorPlatform
     */
    public GraalAcceleratorPlatform getPlatform() {
        return this.graalAccPlatform;
    }

    @Override
    public String toString() {
        return graalAccPlatform.toString();
    }
}
