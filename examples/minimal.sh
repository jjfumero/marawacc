#!/bin/bash
echo -e "[Script] Running simple test, product version\n"

vm="vm"
if [[ $1 == "debug" ]]
then
	vm="vmg"
fi

mx $vm \
  -jvmci \
  -Xmx4g \
  -Xms4g \
  -jvmci \
  -Dmarawacc.printOCLKernel=true \
  -XX:-BootstrapJVMCI \
  -XX:-UseJVMCIClassLoader \
  -Xbootclasspath/p:graal.jar \
  -cp @uk.ac.ed.jpai.samples \
  uk.ac.ed.jpai.test.samples.MarawaccMinimalExample 

