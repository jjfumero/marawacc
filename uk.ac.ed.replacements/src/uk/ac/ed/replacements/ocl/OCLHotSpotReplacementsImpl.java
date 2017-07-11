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

package uk.ac.ed.replacements.ocl;

import jdk.vm.ci.code.TargetDescription;
import uk.ac.ed.accelerator.math.ocl.OCLMath;

import com.oracle.graal.api.replacements.SnippetReflectionProvider;
import com.oracle.graal.phases.util.Providers;
import com.oracle.graal.replacements.ReplacementsImpl;

public class OCLHotSpotReplacementsImpl extends ReplacementsImpl {

    public OCLHotSpotReplacementsImpl(Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target) {
        super(providers, snippetReflection, target);
        registerSubstitutions(OCLMath.class, OCLMathSubstitutions.class);
    }
}
