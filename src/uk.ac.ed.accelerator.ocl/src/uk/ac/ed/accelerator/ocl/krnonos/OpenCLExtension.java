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
package uk.ac.ed.accelerator.ocl.krnonos;

public enum OpenCLExtension {

    // Extensions in OpenCL 1.0
    cl_khr_fp64("cl_khr_fp64"),
    cl_khr_select_fprounding_mode("cl_khr_select_fprounding_mode"),
    cl_khr_global_int32_base_atomics("cl_khr_global_int32_base_atomics"),
    cl_khr_global_int32_extended_atomics("cl_khr_global_int32_extended_atomics"),
    cl_khr_local_int32_base_atomics("cl_khr_local_int32_base_atomics"),
    cl_khr_local_int32_extended_atomics("cl_khr_local_int32_extended_atomics"),
    cl_khr_int64_base_atomics("cl_khr_int64_base_atomics"),
    cl_khr_int64_extended_atomics("cl_khr_int64_extended_atomics"),
    cl_khr_3d_image_writes("cl_khr_3d_image_writes"),
    cl_khr_byte_addressable_store("cl_khr_byte_addressable_store"),
    cl_khr_fp16("cl_khr_fp16"),
    CL_APPLE_gl_sharing("CL_APPLE_gl_sharing"),
    CL_KHR_gl_sharing("CL_KHR_gl_sharing"),

    // OpenCL 2.0
    cl_khr_d3d10_sharing("cl_khr_d3d10_sharing"),
    cl_khr_d3d11_sharing("cl_khr_d3d11_sharing"),
    cl_khr_dx9_media_sharing("cl_khr_dx9_media_sharing"),
    cl_khr_egl_event("cl_khr_egl_event"),
    cl_khr_egl_image("cl_khr_egl_image"),
    cl_khr_gl_depth_images("cl_khr_gl_depth_images"),
    cl_khr_gl_event("cl_khr_gl_event"),
    cl_khr_gl_msaa_sharing("cl_khr_gl_msaa_sharing"),
    cl_khr_gl_sharing("cl_khr_gl_sharing"),
    cl_khr_icd("cl_khr_icd"),
    cl_khr_initialize_memory("cl_khr_initialize_memory"),
    cl_khr_spir("cl_khr_spir"),
    cl_khr_srgb_image_writes("cl_khr_srgb_image_writes"),
    cl_khr_subgroups("cl_khr_subgroups"),
    cl_khr_terminate_context("cl_khr_terminate_context");

    OpenCLExtension(String name) {
        this.name = name;
    }

    private String name;

    @Override
    public String toString() {
        return this.name;
    }
}
