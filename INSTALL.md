# Marawacc Installation and Configuration 

OpenCL backend for Graal. It also provides an API for directly compiling and running a Java application in map/reduce style to OpenCL via Graal.


## Prerequisites ##
* Oracle/OpenJDK jdk8_91 or jdk8_92 [1,2]
* MX Tool [3]
* OpenCL Driver >= 1.2 [4]
* GCC <= 5.3.1 (GCC 6.X does not compile with Hotspot)

## Current implementation tools and compilers

The current Java compiler is JDK8 1.8_91.
The current mx is the one provide in bitbucket repository.

## Marawacc configuration ##

1. Oracle mx tool

```bash
$ cd ~/bin/
$ hg clone https://bitbucket.org/allr/mx
```

Update the PATH to MX

```bash
$ export PATH=$HOME/bin/mx/:$PATH
```

Export JAVA_HOME and set DEFAULT VM to "server" or "jvmci" 

NOTE: Use JVMCI to use Graal with bootstrap 

```bash
$ export JAVA_HOME=$HOME/bin/jdk1.8.0_91
$ export DEFAULT_VM="jvmci"
```

Then: 

```bash
$ mkdir graal-ocl
$ cd graal-ocl
```

MX is a tool to manage repositories.  It manages the repository and all the dependencies automatically.

```bash
$ mx sclone ssh://hg@bitbucket.org/juanfumero/marawacc marawacc
$ cd marawacc
$ make 
```

Check example

```bash
$ make sample  		# graal-jvmci compiler (recommended for Java apps)
$ make esample 		# with server compiler (for Truffle apps) 
```

## Import to Eclipse ##

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

# Links

1. http://www.oracle.com/technetwork/java/javase/downloads/index.html 
2. https://jdk8.java.net/download.html 
3. https://bitbucket.org/allr/mx
3. https://wiki.tiker.net/OpenCLHowTo

