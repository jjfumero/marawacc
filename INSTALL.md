# Marawacc Installation and Configuration 

OpenCL backend for Graal. It also provides an API for directly compiling and running a Java application in map/reduce style to OpenCL via Graal.


## Prerequisites 
* Oracle/OpenJDK jdk8_91 or jdk8_92 [1,2]
* MX Tool [3]
* OpenCL Driver >= 1.2 [4]
* GCC <= 5.3.1 (GCC 6.X does not compile with this version of HotSpot)
* Git >= 2.0

## Current implementation tools and compilers

The current Java compiler is `JDK8 1.8_91`.

The current mx is the one provide in the Github repository.

## Marawacc configuration 

1. Graal mx Tool

```bash
$ cd ~/bin/
$ git clone https://github.com/graalvm/mx
$ cd mx 
$ git checkout 900cc06  
$ cd -- 
```

Update the PATH to MX

```bash
$ export PATH=$HOME/bin/mx/:$PATH
```

Export `JAVA_HOME` and set `DEFAULT_VM` to `server` or `jvmci`

NOTE: Use JVMCI to use Graal with bootstrap 

```bash
$ export JAVA_HOME=$HOME/bin/jdk1.8.0_91
$ export DEFAULT_VM="jvmci"
```

Then: 

```bash
$ mkdir marawacc
$ cd marawacc
```

```bash
$ git clone git@github.com:jjfumero/jvmci-marawacc.git jvmci     ## Download JVMCI dependency
$ git clone git@github.com:jjfumero/truffle-marawacc.git truffle ## Download Truffle dependency
$ git clone https://github.com/jjfumero/graal-marawacc graal     ## Download Graal dependency
```

```bash
$ git clone git@github.com:jjfumero/marawacc.git marawacc
$ make 
```

Check example

```bash
$ make sample  		# graal-jvmci compiler (recommended for Java apps)
$ make esample 		# with server compiler (for Truffle apps) 
```

## Import to Eclipse 

Generate eclipse files: 

```bash
$ make eclipse
```

Change the JVM in eclipse to the new modified Graal version:

Windows -> Preferences -> Java -> Installed JREs -> Add 

Add the VM in marawacc/jvmci/jdkXX/product/ with the following options:

```bash
-jvmci -XX:-BootstrapJVMCI -XX:-UseJVMCIClassLoader -Dmarawacc.printOCLKernel=true -Dmarawacc.printOCLInfo=true
```

And give the name "GraalVM" for example. 

To import the source code into eclipse:

```
File > Import > Existing Projects into Workspace 
```

Select the marawacc directory. It will also import all the dependencies (Graal, Truffle and JVMCI).

### Links

1. http://www.oracle.com/technetwork/java/javase/downloads/index.html 
2. https://jdk8.java.net/download.html 
3. https://github.com/graalvm/mx
3. https://wiki.tiker.net/OpenCLHowTo

