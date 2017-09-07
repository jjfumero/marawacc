# -*- coding: utf-8 -*-

## Script to set the correct name of libJOCL.so for Centos and Fedora distributions

import platform
import os
import os.path

linuxDistribution = platform.linux_distribution()[0].lower()

if (linuxDistribution.startswith("centos") or linuxDistribution.startswith("fedora")):

	print "Linux Distribution: " + linuxDistribution + " detected"
	
	joclLibraryName = "libJOCL_0_1_9-linux-x86_64.so"

	if os.path.exists(joclLibraryName):
		print "Updating name of JOCL library"
		command = "mv /tmp/libJOCL_0_1_9-linux-x86_64.so /tmp/libJOCL_0_1_9.so"
		os.system(command)

