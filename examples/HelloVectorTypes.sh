#!/bin/bash
echo -e "[Script] Running simple test, product version\n"

vm="vm"
if [[ $1 == "debug" ]]
then
	vm="vmg"
fi

mx $vm \
	-Xmx4g \
	-Xms4g \
	-jvmci \
	-Dmarawacc.printOCLKernel=true \
	-Dmarawacc.multidevice=true \
	-Dmarawacc.useVectorTypes=true \
	-XX:-BootstrapJVMCI \
	-XX:-UseJVMCIClassLoader \
	-Dmarawacc.printOCLInfo=true \
	-Xbootclasspath/p:graal.jar \
	-cp @uk.ac.ed.jpai.samples \
	uk.ac.ed.jpai.samples.HelloVectorTypes 

