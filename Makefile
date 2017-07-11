default: parallel 

all:
	mx build 

parallel:
	mx build -p

clean:
	mx clean

eclipse:
	mx eclipseinit

sample:
	# Execution with JVMCI
	bash examples/helloMarawaccJVMCI.sh

esample:
	# server mode
	bash examples/helloMarawaccServer.sh

vector:
	bash examples/HelloVectorTypes.sh

doc_install:
	# documentation in the webbrowser
	grip -b INSTALL.md

doc_options:
	# VM options, documentation
	grip -b DOCUMENTATION_OPTIONS.md
