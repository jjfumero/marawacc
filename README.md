# README #

Marawacc is a compiler framework for OpenCL Just-In-Time (JIT) that automatically compiles a subset of the Java bytecode into OpenCL, and a
runtime system that orchestrates the execution of the GPU program within Java. Marawacc is a research prototype.

Marawacc makes use of the Graal compiler and Truffle DSL ([https://github.com/graalvm/graal](https://github.com/graalvm/graal)) to optimise Java and R programs on top of the JVM. 

Marawacc integrates an API, called **JPAI** (Java Programming Array Interface) [2] to develop GPU and multi-core Java applications using the Function interface in Java 8.
JPAI is a new Java API based on Java 8 Stream for parallel and heterogeneous programming. JPAI uses algorithmic skeletons and the new feature of Java lambda expressions to facilitate the programmability and readability. 
Parallel operations using map/reduce in JPAI can be composed and reused. 


### Install Marawacc ###

See the INSTALL.md in this repository. 


### Run Marawacc ###

Once Marawacc is compiled, it runs within the Graal VM. To enable OpenCL JIT compilation you need the following flags:


```
#!bash

-jvmci -XX:-BootstrapJVMCI -XX:-UseJVMCIClassLoader -Dmarawacc.printOCLKernel=true 

```

### Marawacc Options ###

See DOCUMENTATION_OPTIONS.md file in this repository. 


### Example in JPAI ###

Marawacc is the backend that generates OpenCL code at runtime from the Graal IR and executes the GPU program. 
JPAI is the interface to program GPUs and multi-core CPUs. For GPU execution, JPAI invokes Marawacc. This section shows a full example in JPAI.


```
#!java

public class Hello {

    public static void main(String[] args) {

        // Main Function in JPAI is the ArrayFunction<T, R>
        // ArrayFunction computes a function from types T to R 
        ArrayFunction<Tuple2<Integer, Integer>, Double> computation = new MapAccelerator<>(vectors -> (2.5 * vectors._1() + vectors._2());

        // Prepare the input data
        int size = 262144;
        PArray<Tuple2<Integer, Integer>> input = new PArray<>(size, TypeFactory.Tuple("Tuple2<Integer, Integer>"));
        for (int i = 0; i < size; ++i) {
            input.put(i, new Tuple2<>((double) i, (double) (i + 2)));
        }

        // Compute on the GPU. The apply method will generate the OpenCL code and execute it on the GPU. 
        PArray<Double> output = computation.apply(input);
    }
}

```

* ArrayFunction<T, R>: it is the main class in JPAI. It extends the Function interface in Java 8 with algorithmic skeletons such as map and reduce. 
* PArray<T>: Portable Array (PArray) is a data structure created for avoiding the data transformation between Java and OpenCL (marshalling) and increase the overall performance. Details of the PArray data structure in [1].


### Publications 

[1] Juan José Fumero, Toomas Remmelg, Michel Steuwer, and Christophe Dubach. 2015. **Runtime Code Generation and Data Management for Heterogeneous Computing in Java.** In Proceedings of the Principles and Practices of Programming on The Java Platform (PPPJ '15). ACM, New York, NY, USA, 16-26. DOI: http://dx.doi.org/10.1145/2807426.2807428 

[2] Juan José Fumero, Michel Steuwer, and Christophe Dubach. 2014. **A Composable Array Function Interface for Heterogeneous Computing in Java.** In Proceedings of ACM SIGPLAN International Workshop on Libraries, Languages, and Compilers for Array Programming (ARRAY'14). ACM, New York, NY, USA, Pages 44, 6 pages. DOI=http://dx.doi.org/10.1145/2627373.2627381

Marawacc is used as a backend for FastR+GPU project (OpenCL JIT compilation for the R programs using Partial Evaluation in Graal)

[3] Juan Fumero, Michel Steuwer, Lukas Stadler, and Christophe Dubach. 2017. **Just-In-Time GPU Compilation for Interpreted Languages with Partial Evaluation.** In Proceedings of the 13th ACM SIGPLAN/SIGOPS International Conference on Virtual Execution Environments (VEE '17). ACM, New York, NY, USA, 60-73. DOI: https://doi.org/10.1145/3050748.3050761 

[4] Juan Fumero, Michel Steuwer, Lukas Stadler, Christophe Dubach. ** OpenCL JIT Compilation for Dynamic Programming Language.** MoreVMs 2017. [http://conf.researchr.org/event/MoreVMs-2017/morevms-2017-papers-opencl-jit-compilation-for-dynamic-programming-languages](http://conf.researchr.org/event/MoreVMs-2017/morevms-2017-papers-opencl-jit-compilation-for-dynamic-programming-languages)


### License ###

GPL V2

### Who do I talk to? ###

Marawacc project is a part of my PhD at the University of Edinburgh. My PhD has been partially funded by Oracle Labs.

#### Main Developer
* Juan Fumero <juan.fumero @ ed.ac.uk>

#### Supervisors

* Christophe Dubach <christophe.dubach @ ed.ac.uk> 
* Michel Steuwer <michel.steuwer @ ed.ac.uk >
