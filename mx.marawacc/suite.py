#
# Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
suite = {
  "mxversion" : "5.6.5",
  "name" : "marawacc",

  "imports" : {
    "suites" : [
            {
               "name" : "graal",
               "version" : "50baf5ef8953526971f006a02da8f2cb96afe905",
               "urls" : [{"url" : "ssh://hg@bitbucket.org/juanfumero/graalx", "kind" : "hg"}]
            },
        ],
   },

  "javac.lint.overrides" : "none",

  # distributions that we depend on
  "libraries" : {

    "JOCL" : {
      "urls" : [
        	"http://homepages.inf.ed.ac.uk/s1369892/JOCL-0.1.9.jar",	
		],
		"sha1" : "e0ef09966507d23c2d33398215f268e3a9578fb8",
    },
  },

  "projects" : {

	"uk.ac.ed.accelerator.hotspot" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
				"graal:GRAAL_TRUFFLE_HOTSPOT",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

	"uk.ac.ed.accelerator" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
			"JOCL",
                        "truffle:TRUFFLE_API",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

    "uk.ac.ed.jpai" : {
      "subDir" : "src",
      "sourceDirs" : ["src","test"],
      "dependencies" : [
		"graal:GRAAL_HOTSPOT",
		"uk.ac.ed.accelerator.ocl",
		"uk.ac.ed.datastructures",
		"graal:GRAAL",
		"uk.ac.ed.accelerator",
                "truffle:TRUFFLE_API",
		"JOCL",
      ],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

     "uk.ac.ed.jpai.test" : {
      "subDir" : "src",
      "sourceDirs" : ["src","test"],
      "dependencies" : [
		"graal:GRAAL_HOTSPOT",
		"uk.ac.ed.accelerator.ocl",
		"uk.ac.ed.datastructures",
		"graal:GRAAL",
		"uk.ac.ed.accelerator",
		"uk.ac.ed.jpai",
		"mx:JUNIT",
		"JOCL",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

    "uk.ac.ed.jpai.samples" : {
      "subDir" : "src",
      "sourceDirs" : ["src","test"],
      "dependencies" : [
		"graal:GRAAL_HOTSPOT",
		"uk.ac.ed.accelerator.ocl",
		"uk.ac.ed.datastructures",
		"graal:GRAAL",
		"uk.ac.ed.accelerator",
		"uk.ac.ed.jpai",
		"JOCL",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

    "uk.ac.ed.replacements" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
			"graal:GRAAL",
			"graal:GRAAL_API",
			"graal:GRAAL_HOTSPOT",
			"uk.ac.ed.accelerator.math",
			"uk.ac.ed.datastructures",
			"uk.ac.ed.accelerator.hotspot",
			"graal:GRAAL_HOTSPOT",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

    "uk.ac.ed.datastructures" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [ 
			"JOCL", 
			"uk.ac.ed.accelerator",
                         "truffle:TRUFFLE_API",
                        "truffle:TRUFFLE_DSL_PROCESSOR"
  		        ],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

	"uk.ac.ed.accelerator.ocl" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
			"JOCL",
			"graal:GRAAL_COMPILER",
			"graal:GRAAL_API",
			"graal:GRAAL",
			"uk.ac.ed.datastructures",
			"uk.ac.ed.replacements",
			"uk.ac.ed.accelerator",
			"uk.ac.ed.compiler.utils",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

    "uk.ac.ed.compiler.utils" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
			"graal:GRAAL_COMPILER",
			"graal:GRAAL_API",
			"graal:GRAAL",
			"uk.ac.ed.replacements",
			"uk.ac.ed.accelerator",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

    "uk.ac.ed.compiler.utils.test" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
			"graal:GRAAL_COMPILER",
			"graal:GRAAL_API",
			"graal:GRAAL",
			"uk.ac.ed.replacements",
			"uk.ac.ed.accelerator",
			"uk.ac.ed.compiler.utils",
			"mx:JUNIT",
			],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

	"uk.ac.ed.accelerator.nfi.ocl.test" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "dependencies" : [
                       "graal:GRAAL_TRUFFLE_HOTSPOT",
                       "graal:GRAAL_COMPILER",
                       "mx:JUNIT",
                       ],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,HotSpot",
    },

	"uk.ac.ed.accelerator.math" : {
      "subDir" : "src",
      "sourceDirs" : ["src"],
      "checkstyle" : "com.oracle.graal.graph",
      "javaCompliance" : "1.8",
      "workingSets" : "Graal,Replacements",
    },
  },

  "distributions" : {
     "MARAWACC" : {
      "dependencies" : ["uk.ac.ed.jpai", "uk.ac.ed.jpai.samples"],
      "distDependencies" : [
		"graal:GRAAL_COMPILER",
		"graal:GRAAL_API",
		"graal:GRAAL",
		"graal:GRAAL_HOTSPOT",
		"graal:GRAAL_TRUFFLE",
                "truffle:TRUFFLE_API",
      ],
    },

  },

}
