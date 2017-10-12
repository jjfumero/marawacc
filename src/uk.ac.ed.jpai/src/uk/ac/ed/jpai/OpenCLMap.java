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

package uk.ac.ed.jpai;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Function;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

import uk.ac.ed.accelerator.cache.OCLKernelCache;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.common.GraalAcceleratorPlatform;
import uk.ac.ed.accelerator.common.GraalAcceleratorSystem;
import uk.ac.ed.accelerator.common.ParallelSkeleton;
import uk.ac.ed.accelerator.ocl.GraalOpenCLRuntime;
import uk.ac.ed.accelerator.ocl.OCLRuntimeUtils;
import uk.ac.ed.accelerator.ocl.helper.ArrayUtil;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.FunctionalPatternTemplate;
import uk.ac.ed.accelerator.ocl.runtime.GraalIRConversion;
import uk.ac.ed.accelerator.ocl.runtime.GraalIRUtilities;
import uk.ac.ed.accelerator.ocl.scope.PArrayScopeManager;
import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.accelerator.utils.OpenCLUtils;
import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorDevice;
import uk.ac.ed.datastructures.common.AcceleratorPArray;
import uk.ac.ed.datastructures.common.JavaDataTypeSizes;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.PArray.StorageMode;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.cache.UserFunctionCache;

import com.oracle.graal.graph.Node;
import com.oracle.graal.nodes.StructuredGraph;

public class OpenCLMap<inT, outT> extends MapJavaThreads<inT, outT> {

    private UUID uuidKernel;
    private boolean compilationFailed = false;
    private ArrayList<ArrayList<cl_mem>> scopedVariableBuffers = new ArrayList<>();
    private ArrayList<ArrayList<ScopeInfoProperties>> scopeVarList = new ArrayList<>();

    private static int idxWrite;
    private static int scopeWrites;
    private static int idxKernel;

    private AcceleratorPArray<Integer> deoptBufferFlag;

    private ArrayList<ScalarVarInfo> scalarVariableList = new ArrayList<>();

    public OpenCLMap(Function<inT, outT> f) {
        super(f);
        uuidKernel = UserFunctionCache.INSTANCE.insertFunction(f);
    }

    private static void writeIntoBuffer(int deviceIndex, int writeIndex, cl_command_queue commandQueue, ArrayList<ScopeInfoProperties> scopeVarDeviceList, ArrayList<cl_mem> scopeBufferDeviceList) {
        int bufferIndex = 0;
        for (ScopeInfoProperties s : scopeVarDeviceList) {
            cl_event writeEvent = new cl_event();
            CL.clEnqueueWriteBuffer(commandQueue, scopeBufferDeviceList.get(bufferIndex), CL.CL_TRUE, 0, s.getSize(), s.getPointer(), 0, null, writeEvent);
            CL.clFlush(commandQueue);
            bufferIndex++;

            // Profiling
            if (GraalAcceleratorOptions.profileOffload) {
                CL.clSetEventCallback(writeEvent, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_WRITE_BUFFER, writeIndex, deviceIndex), null);
            }
        }
    }

    private static int getNumberOfCurrentDevices() {
        int numDevices = 1;
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            numDevices = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
        }
        return numDevices;
    }

    private void writeScopeVars(int writeIndex) {
        int numDevices = getNumberOfCurrentDevices();
        for (int i = 0; i < numDevices; i++) {
            cl_command_queue queue = getOpenCLDevice(i).getCommandQueue();
            writeIntoBuffer(i, writeIndex, queue, scopeVarList.get(i), scopedVariableBuffers.get(i));
        }
    }

    private void setOutputPrimitiveArray(PArray<inT> input, RuntimeObjectTypeInfo runtimeTypeInfo, int idx) {
        if (runtimeTypeInfo.getClassObject() == Double.class) {
            double[] out = new double[input.size()];
            output.setDoubleArray(idx, out);
        } else if (runtimeTypeInfo.getClassObject() == Integer.class) {
            int[] out = new int[input.size()];
            output.setIntArray(idx, out);
        } else if (PArray.TUPLESET.contains(runtimeTypeInfo.getClassObject())) {
            // Inspect nested types
            RuntimeObjectTypeInfo[] nestedTypes = outputType.getNestedTypes();
            int i = idx;
            for (RuntimeObjectTypeInfo r : nestedTypes) {
                setOutputPrimitiveArray(input, r, i);
                i++;
            }
        } else {
            throw new RuntimeException("Data type not suppoted yet: " + outputType.getClassObject());
        }
    }

    @Override
    public PArray<outT> apply(PArray<inT> input) {

        long begin = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COMPUTE_MAP, "begin", begin);
        }

        if (!preparedExecutionFinish) {
            // It modifies compilationFailed variable
            prepareExecution(input);
        }

        if (isKernelCompilationFailed()) {
            return super.apply(input);
        }

        if (!scopeVarList.isEmpty()) {
            writeScopeVars(scopeWrites++);
        }

        if (output == null) {
            if (input.isSequence()) {
                output = new AcceleratorPArray<>(input.getTotalSizeWhenSequence(), outputType, StorageMode.OPENCL_BYTE_BUFFER, true);
            } else {
                output = allocateOutputArray(input.size(), input.getStorageMode());
            }
            if (input.isPrimitiveArray()) {
                setOutputPrimitiveArray(input, outputType, 0);
                output.setPrimitive(true);
            }
        }

        if (GraalAcceleratorOptions.deoptGuardsEnabled && deoptBufferFlag == null) {
            deoptBufferFlag = new AcceleratorPArray<>(1, TypeFactory.Integer(), StorageMode.OPENCL_BYTE_BUFFER, false);
        }

        ((AcceleratorPArray<outT>) output).allocateOpenCLBuffer(CL.CL_MEM_WRITE_ONLY);
        ((AcceleratorPArray<outT>) output).copyMetaDataToDevice(idxWrite++);

        deoptBufferFlag.allocateOpenCLBuffer(CL.CL_MEM_WRITE_ONLY);

        if ((GraalAcceleratorOptions.debugCacheGPUCode) && (OCLKernelCache.getInstance().isInCache(uuidKernel))) {
            System.out.println("[DEBUG] Kernel in Cache: " + uuidKernel);
        }

        executeOpenCLKernel((AcceleratorPArray<inT>) input, scopedVariableBuffers, (AcceleratorPArray<outT>) output);

        long end = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COMPUTE_MAP, "end", end);
            Profiler.getInstance().put(ProfilerType.COMPUTE_MAP, end - begin);
        }

        return output;
    }

    /**
     * Preparation in compute map means:
     * <ul>
     * <li>Infer the output data type</li>
     * <li>Compile the kernel</li>
     * <li>Store it in a cache</li>
     * </ul>
     *
     */
    @Override
    public PArray<outT> prepareExecution(PArray<inT> input) {

        long begin = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COMPUTE_MAP_PREPARE, "begin", begin);
        }

        // Fill the info for input and output. This method will execute the first element of the
        // function to infer the output data type.
        PArray<outT> out = inferTypes(input);

        // Generate the OpenCL C Kernel
        generateAndCompileOpenCLKernel(input);

        createPinnedBuffersForScopedVariables(input);

        long end = System.nanoTime();

        if (GraalAcceleratorOptions.profileOffload) {
            Profiler.getInstance().writeInBuffer(ProfilerType.COMPUTE_MAP_PREPARE, "end", end);
            Profiler.getInstance().put(ProfilerType.COMPUTE_MAP_PREPARE, (end - begin));
        }

        preparedExecutionFinish = true;
        return out;
    }

    @Override
    public AcceleratorPArray<outT> allocateOutputArray(int size, StorageMode mode) {
        return new AcceleratorPArray<>(size, outputType, mode, true);
    }

    @SuppressWarnings("unused")
    private static void checkStatus(int[] status, String message) {
        checkStatus(status[0], message);
    }

    private static void checkStatus(int status, String message) {
        if (status != CL.CL_SUCCESS) {
            System.err.println("[OPENCL ERROR] : " + message);
            throw new RuntimeException("[OPENCL ERROR] : " + message);
        }
    }

    private void setArguments(cl_kernel kernel, int deviceIndex, AcceleratorPArray<inT> in, ArrayList<ArrayList<cl_mem>> scope, ArrayList<ScalarVarInfo> scopeScalarVariables,
                    AcceleratorPArray<outT> out) {
        // set arguments
        int argumentNumber = 0;
        int status = 0;
        for (cl_mem inputBuffer : in.getOpenCLBuffersWithMetadata(deviceIndex)) {
            status |= CL.clSetKernelArg(kernel, argumentNumber, Sizeof.cl_mem, Pointer.to(inputBuffer));
            argumentNumber++;
        }

        ArrayList<cl_mem> scopedVarBuffers = (scope.isEmpty() ? null : scope.get(deviceIndex));

        // Build index arrays
        int base = argumentNumber;

        // build array of addresses
        int s = scopedVarBuffers == null ? 0 : (scopedVarBuffers.size() / 2);
        int size = s + scopeScalarVariables.size();
        int[] address = new int[size];
        final int INIT_VALUE = -1;
        // Initialisation
        Arrays.fill(address, INIT_VALUE);

        if ((scopeScalarVariables != null) && (!scopeScalarVariables.isEmpty())) {
            for (ScalarVarInfo scalar : scopeScalarVariables) {
                int index = scalar.getIndexVar();
                address[index] = base + index;
            }
        }

        if (scopedVarBuffers != null) {
            int accumulator = 0;
            for (int i = 0; i < address.length; i++) {
                if (address[i] == INIT_VALUE) {
                    address[i] = base + i + accumulator;
                    accumulator++;
                } else {
                    address[i] += accumulator;
                }
            }
        }

        // Set scalar variables to the kernel
        boolean[] scalars = new boolean[size];
        if ((scopeScalarVariables != null) && (!scopeScalarVariables.isEmpty())) {
            for (ScalarVarInfo scalar : scopeScalarVariables) {
                int addr = address[scalar.getIndexVar()];
                if (scalar.getOpenCLSize() == Sizeof.cl_int) {
                    status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_int, Pointer.to(new int[]{(int) scalar.getValue()}));
                    scalars[scalar.getIndexVar()] = true;
                } else if (scalar.getOpenCLSize() == Sizeof.cl_float) {
                    status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_float, Pointer.to(new float[]{(float) scalar.getValue()}));
                    scalars[scalar.getIndexVar()] = true;
                } else if (scalar.getOpenCLSize() == Sizeof.cl_double) {
                    status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_double, Pointer.to(new double[]{(double) scalar.getValue()}));
                    scalars[scalar.getIndexVar()] = true;
                } else if (scalar.getOpenCLSize() == Sizeof.cl_long) {
                    status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_long, Pointer.to(new long[]{(long) scalar.getValue()}));
                    scalars[scalar.getIndexVar()] = true;
                } else if (scalar.getOpenCLSize() == Sizeof.cl_short) {
                    status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_short, Pointer.to(new short[]{(short) scalar.getValue()}));
                    scalars[scalar.getIndexVar()] = true;
                } else if (scalar.getOpenCLSize() == Sizeof.cl_char) {
                    status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_char, Pointer.to(new char[]{(char) scalar.getValue()}));
                    scalars[scalar.getIndexVar()] = true;
                } else {
                    throw new RuntimeException("Data type not supported yet [scope clSetKernelArg");
                }
            }
        }

        // Set array buffers scope variables to the OpenCL kernel
        if (scopedVarBuffers != null) {
            int j = 0;
            int addr = argumentNumber - 1;
            for (int i = 0; i < scopedVarBuffers.size(); i += 2) {
                if (j < scalars.length) {
                    while (scalars[j] == true && j < scalars.length) {
                        j++;
                    }
                    addr = address[j];
                    j++;
                } else {
                    addr++;
                }

                cl_mem scopedBuffer = scopedVarBuffers.get(i);
                status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_mem, Pointer.to(scopedBuffer));
                scopedBuffer = scopedVarBuffers.get(i + 1);
                addr++;
                status |= CL.clSetKernelArg(kernel, addr, Sizeof.cl_mem, Pointer.to(scopedBuffer));
            }
        }

        s = scopedVarBuffers == null ? 0 : (scopedVarBuffers.size());
        size = s + scopeScalarVariables.size();
        argumentNumber += size;

        for (cl_mem outputBuffer : out.getOpenCLBuffersWithMetadata(deviceIndex)) {
            status |= CL.clSetKernelArg(kernel, argumentNumber, Sizeof.cl_mem, Pointer.to(outputBuffer));
            argumentNumber++;
        }

        if (GraalAcceleratorOptions.deoptGuardsEnabled && deoptBufferFlag != null) {
            cl_mem bufferDeopt = deoptBufferFlag.getOpenCLBuffersWithMetadata(deviceIndex).get(0);
            status |= CL.clSetKernelArg(kernel, argumentNumber, Sizeof.cl_mem, Pointer.to(bufferDeopt));
            argumentNumber++;
        }

        checkStatus(status, "clSetKernelArgs");
    }

    /**
     * Compute local work size.
     *
     * @param kernel
     * @param size
     * @return long[]
     */
    private static long[] computeLocalWorkSize(cl_kernel kernel, int deviceIDX, long size) {
        long[] localWorkSize = new long[1];
        cl_device_id device = getOpenCLDevice(deviceIDX).getDevice();
        long[] kernelWorkGroupSize = new long[1];
        CL.clGetKernelWorkGroupInfo(kernel, device, CL.CL_KERNEL_WORK_GROUP_SIZE, Sizeof.cl_ulong, Pointer.to(kernelWorkGroupSize), null);
        if (kernelWorkGroupSize[0] > size) {
            localWorkSize[0] = size;
        } else {
            localWorkSize[0] = kernelWorkGroupSize[0];
        }
        return localWorkSize;
    }

    private void executeKernelIntoDevice(int deviceIndex, cl_kernel kernel, AcceleratorPArray<inT> input, ArrayList<ArrayList<cl_mem>> scope, AcceleratorPArray<outT> outputLocal) {

        setArguments(kernel, deviceIndex, input, scope, scalarVariableList, outputLocal);

        long size = input.isSequence() ? input.getTotalSizeWhenSequence() : input.size();
        if (GraalAcceleratorOptions.multiOpenCLDevice) {
            int numMaxDevices = GraalAcceleratorSystem.getInstance().getPlatform().getNumCurrentCurrentDevices();
            if (deviceIndex > 0) {
                size = (size / numMaxDevices) + (size % numMaxDevices);
            } else {
                size = (size / numMaxDevices);
            }
        }

        long[] globalWorkSize = new long[]{size};
        long[] localWorkSize = computeLocalWorkSize(kernel, deviceIndex, size);

        cl_command_queue commandQueue = getOpenCLDevice(deviceIndex).getCommandQueue();

        if (GraalAcceleratorOptions.printOCLkernelInfo) {
            System.out.println("[OpenCL] Running kernel with #" + size + " threads");
            System.out.println("[OpenCL] Local Group Size with #" + localWorkSize[0] + " threads");
        }

        // Run the kernel
        cl_event kernelEvent = new cl_event();
        int status;
        if ((size % localWorkSize[0]) != 0) {
            status = CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, null, 0, null, kernelEvent);
        } else {
            status = CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, kernelEvent);
        }

        checkStatus(status, "clEnqueueNDRangeKernel");

        if (GraalAcceleratorOptions.profileOffload) {
            CL.clSetEventCallback(kernelEvent, CL.CL_COMPLETE, OpenCLUtils.makeCallBackFunction(ProfilerType.OCL_KERNEL, idxKernel++, deviceIndex), null);
        }
    }

    private void executeOpenCLKernel(AcceleratorPArray<inT> input, ArrayList<ArrayList<cl_mem>> scope, AcceleratorPArray<outT> outputLocal) {
        int numKernels = getNumberOfCurrentDevices();
        for (int i = 0; i < numKernels; i++) {
            cl_kernel kernel = OCLKernelCache.getInstance().get(uuidKernel, i).getKernelBinary();
            executeKernelIntoDevice(i, kernel, input, scope, outputLocal);
        }
    }

    private Deque<Object> getParametersFromTheScope() throws IllegalArgumentException, IllegalAccessException {
        Field[] functionFields = function.getClass().getDeclaredFields();
        Deque<Object> lambdaParameters = new LinkedList<>();
        for (int i = 0; i < functionFields.length; i++) {
            functionFields[i].setAccessible(true);
            lambdaParameters.add(functionFields[i].get(function));
        }
        return lambdaParameters;
    }

    /**
     * Filter the scope. Do not include the PArrays passed as a parameter.
     *
     * @param input
     * @return Deque<Object>
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private Deque<Object> getParametersFromTheScopeWithFilter(PArray<inT> input) throws IllegalArgumentException, IllegalAccessException {
        Field[] functionFields = function.getClass().getDeclaredFields();
        Deque<Object> lambdaParameters = new LinkedList<>();
        for (int i = 0; i < functionFields.length; i++) {
            functionFields[i].setAccessible(true);
            if (functionFields[i].get(function) != input) {
                lambdaParameters.add(functionFields[i].get(function));
            } else {
                PArrayScopeManager.INSTANCE.insertFunction(uuidKernel);
            }
        }
        return lambdaParameters;
    }

    private Object[] getLambdaParameters(PArray<inT> input) throws IllegalArgumentException, IllegalAccessException {
        Deque<Object> lambdaParameters = getParametersFromTheScope();
        lambdaParameters.addFirst(input);
        return lambdaParameters.toArray();
    }

    private Object[] getScope() throws IllegalArgumentException, IllegalAccessException {
        return getParametersFromTheScope().toArray();
    }

    private Object[] getScopeWithFilterPArray(PArray<inT> input) throws IllegalArgumentException, IllegalAccessException {
        return getParametersFromTheScopeWithFilter(input).toArray();
    }

    /**
     * It compiles a lambda expression in Java into OpenCL C source code. Then, the backend it will
     * install the binary into a cache. If the same expression is executed again, the library will
     * get the code from the cache.
     *
     * @param input
     */
    private void generateAndCompileOpenCLKernel(PArray<inT> input) {

        long start = System.nanoTime();

        StructuredGraph graphLambda = GraalIRConversion.getOptimizedGraalIRLambda(function.getClass());

        if (GraalAcceleratorOptions.printGraalIR) {
            GraalIRUtilities.printGraph(graphLambda, false);
        }

        if (GraalAcceleratorOptions.dumpGraalIR) {
            GraalIRUtilities.dumpGraph(graphLambda, "OptimizedLambda");
        }

        StructuredGraph skeletonGraph = GraalIRConversion.getGraalIRForMethod(ParallelSkeleton.MAP);

        if (GraalAcceleratorOptions.printGraalIR) {
            System.out.println("Bytecode for parallel skeleton");
            MetaAccessProvider metaAccess = GraalIRConversion.getMetaAccess();
            ResolvedJavaType javaType = GraalIRUtilities.getType(FunctionalPatternTemplate.class, metaAccess);
            ResolvedJavaMethod resolvedJavaMethod = GraalIRUtilities.getMethod(javaType, "lambdaComputation");
            GraalIRUtilities.printByteCodes(resolvedJavaMethod);
            GraalIRUtilities.dumpGraph(skeletonGraph, "Skeleton Graph");

            System.out.println("\n\nBytecode for the lambda expression");
            ResolvedJavaMethod resolvedJavaMethodForUserFunction = GraalIRConversion.getResolvedJavaMethodForUserFunction(function.getClass());
            GraalIRUtilities.printByteCodes(resolvedJavaMethodForUserFunction);

        }

        try {
            Object[] outputMetadata = DataTypeAPIHelper.createOneOutputElement(outputType.getClassObject());

            Object[] scopeArray = null;
            if (GraalAcceleratorOptions.multiOpenCLDevice) {
                scopeArray = getScope();
            } else {
                scopeArray = getScopeWithFilterPArray(input);
            }

            AcceleratorOCLInfo acceleratorOCLInfo = new AcceleratorOCLInfo(inputType.getNestedTypesOrSelf(), outputType.getNestedTypesOrSelf(), scopeArray);
            Object[] lambdaParameters = getLambdaParameters(input);
            boolean isTruffleCode = false;
            ArrayList<Node> scopedNodes = null;

            GraalIRConversion.generateOffloadOpenCLKernel(skeletonGraph, graphLambda, inputType.getClass(), outputMetadata, acceleratorOCLInfo, ParallelSkeleton.MAP, uuidKernel, lambdaParameters,
                            isTruffleCode, scopedNodes, input.grade());
            GraalOpenCLRuntime.compileOpenCLKernelAndInstallBinaryWithDriver(uuidKernel);

        } catch (Exception e) {
            System.out.println("[Kernel compilation failed]");
            compilationFailed = true;
        }

        long end = System.nanoTime();
        if (GraalAcceleratorOptions.printOCLInfo && !compilationFailed) {
            System.out.println("[KERNEL GENERATION TIME] (ns): " + (end - start));
        }
    }

    private static boolean isArraySimple(Class<?> array) {
        if (array == float[].class) {
            return true;
        } else if (array == double[].class) {
            return true;
        } else if (array == int[].class) {
            return true;
        } else if (array == short[].class) {
            return true;
        } else if (array == int[].class) {
            return true;
        } else if (array == long[].class) {
            return true;
        } else if (array == boolean[].class) {
            return true;
        }
        return false;
    }

    public static long getLong(cl_device_id device, int paramName) {
        return getLongs(device, paramName, 1)[0];
    }

    public static long[] getLongs(cl_device_id device, int paramName, int numValues) {
        long[] values = new long[numValues];
        CL.clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues, Pointer.to(values), null);
        return values;
    }

    public static ByteOrder getEndianess(int deviceIndex) {
        long endianness = getLong(getOpenCLDevice(deviceIndex).getDevice(), CL.CL_DEVICE_ENDIAN_LITTLE);
        if (endianness == 0) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    private static class SizeBuffer {
        public int size;
        public int totalSize;

        public SizeBuffer(int size, int totalSize) {
            this.size = size;
            this.totalSize = totalSize;
        }

        public int getSize() {
            return size;
        }

        public int getTotalSize() {
            return totalSize;
        }
    }

    private static SizeBuffer getSizes(String type, Object array) {
        int size;
        int totalSize;
        if (type.equals("[I")) {
            size = ((int[]) array).length;
            totalSize = JavaDataTypeSizes.INT.getOCLSize() * ((int[]) array).length;
        } else if (type.equals("[J")) {
            size = ((long[]) array).length;
            totalSize = JavaDataTypeSizes.LONG.getOCLSize() * ((long[]) array).length;
        } else if (type.equals("[F")) {
            size = ((float[]) array).length;
            totalSize = JavaDataTypeSizes.FLOAT.getOCLSize() * ((float[]) array).length;
        } else if (type.equals("[D")) {
            size = ((double[]) array).length;
            totalSize = JavaDataTypeSizes.DOUBLE.getOCLSize() * ((double[]) array).length;
        } else if (type.equals("[S")) {
            size = ((short[]) array).length;
            totalSize = JavaDataTypeSizes.SHORT.getOCLSize() * ((short[]) array).length;
        } else {
            throw new RuntimeException("Data type not supported yet: " + type);
        }
        return new SizeBuffer(size, totalSize);
    }

    private static ByteBuffer createHostPinnedMemoryForVarScope(int totalSize) {
        // Pinned memory
        // Buffer creation (pinned memory)
        cl_mem hostData = CL.clCreateBuffer(getOpenCLDevice(0).getContext(), CL.CL_MEM_ALLOC_HOST_PTR, totalSize, null, null);
        ByteOrder order = getEndianess(0);
        ByteBuffer pinnedData = CL.clEnqueueMapBuffer(getOpenCLDevice(0).getCommandQueue(), hostData, CL.CL_TRUE, CL.CL_MAP_WRITE, 0, totalSize, 0, null, null, null);
        pinnedData.order(order);
        return pinnedData;
    }

    private static void copyDataIntoPinnedBuffer(Object array, ByteBuffer pinnedData, int size) {
        // Copy data
        if (array.getClass() == byte[].class) {
            // directly passed to the pinned buffer
            pinnedData.put((byte[]) array);
        } else {
            // XXX: Think a faster way of doing this
            for (int i = 0; i < size; i++) {
                if (array.getClass() == float[].class) {
                    pinnedData.putFloat(i * JavaDataTypeSizes.FLOAT.getOCLSize(), ((float[]) array)[i]);
                } else if (array.getClass() == int[].class) {
                    pinnedData.putInt(i * JavaDataTypeSizes.INT.getOCLSize(), ((int[]) array)[i]);
                } else if (array.getClass() == long[].class) {
                    pinnedData.putLong(i * JavaDataTypeSizes.LONG.getOCLSize(), ((long[]) array)[i]);
                } else if (array.getClass() == short[].class) {
                    pinnedData.putShort(i * JavaDataTypeSizes.SHORT.getOCLSize(), ((short[]) array)[i]);
                } else if (array.getClass() == double[].class) {
                    pinnedData.putDouble(i * JavaDataTypeSizes.DOUBLE.getOCLSize(), ((double[]) array)[i]);
                }
            }
        }
    }

    private static class ScopeInfoProperties {
        private Pointer pointer;
        private int size;

        public ScopeInfoProperties(Pointer pointer, int size) {
            this.pointer = pointer;
            this.size = size;
        }

        public Pointer getPointer() {
            return pointer;
        }

        public int getSize() {
            return size;
        }
    }

    private static class ScalarVarInfo {

        private int openCLSize;
        private Object value;
        private int indexVar;

        public ScalarVarInfo(Object value, int openCLSize, int indexVar) {
            this.value = value;
            this.openCLSize = openCLSize;
            this.indexVar = indexVar;
        }

        public int getOpenCLSize() {
            return openCLSize;
        }

        public Object getValue() {
            return value;
        }

        public int getIndexVar() {
            return indexVar;
        }

    }

    private void initLists(int maxDevices) {
        if (scopedVariableBuffers.isEmpty()) {
            for (int i = 0; i < maxDevices; i++) {
                ArrayList<cl_mem> aux = new ArrayList<>();
                scopedVariableBuffers.add(aux);
            }
        }
        if (scopeVarList.isEmpty()) {
            for (int i = 0; i < maxDevices; i++) {
                ArrayList<ScopeInfoProperties> aux = new ArrayList<>();
                scopeVarList.add(aux);
            }
        }
    }

    private void createOpenCLBuffersForScopeVars(ByteBuffer pinnedData, int totalSize, int deviceIDX) {

        cl_context context = getOpenCLDevice(deviceIDX).getContext();

        ArrayList<cl_mem> clMemList = new ArrayList<>();
        ArrayList<ScopeInfoProperties> propertiesList = new ArrayList<>();

        // Data buffer with pinned memory
        cl_mem dataBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, totalSize, null, null);
        Pointer dataPtr = Pointer.to(pinnedData);
        clMemList.add(dataBuffer);

        ScopeInfoProperties info = new ScopeInfoProperties(dataPtr, totalSize);
        propertiesList.add(info);

        // utility buffer
        int[] utilityArray = {totalSize, 0};
        cl_mem utilityBuffer = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, JavaDataTypeSizes.INT.getOCLSize() * 2, null, null);
        Pointer utilityPtr = Pointer.to(utilityArray);
        clMemList.add(utilityBuffer);

        ScopeInfoProperties info2 = new ScopeInfoProperties(utilityPtr, JavaDataTypeSizes.INT.getOCLSize() * 2);
        propertiesList.add(info2);

        scopedVariableBuffers.get(deviceIDX).addAll(clMemList);
        scopeVarList.get(deviceIDX).addAll(propertiesList);
    }

    private void createPinnedMemoryForJavaArray(Object scopedVar, int numDevices) {
        // get read only flat reference
        Object array = null;
        if (isArraySimple(scopedVar.getClass())) {
            array = scopedVar;
        } else {
            // Flatten only if it is > 1D
            array = ArrayUtil.flattenArray(scopedVar, true);
        }

        String type = array.getClass().getName();
        SizeBuffer sizeBuffer = getSizes(type, array);

        int size = sizeBuffer.getSize();
        int totalSize = sizeBuffer.getTotalSize();

        // We create pinned memory in the host side, the device could be any
        ByteBuffer pinnedData = createHostPinnedMemoryForVarScope(totalSize);
        copyDataIntoPinnedBuffer(array, pinnedData, size);

        for (int i = 0; i < numDevices; i++) {
            // int sizeBuf = getSizeForPartition(totalSize, numDevices, i);
            createOpenCLBuffersForScopeVars(pinnedData, totalSize, i);
        }
    }

    private Object[] getParametersFromTheScope(PArray<inT> input) {
        Object[] parametersFromTheScope = null;
        try {
            if (GraalAcceleratorOptions.multiOpenCLDevice) {
                parametersFromTheScope = getScope();
            } else {
                parametersFromTheScope = getScopeWithFilterPArray(input);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Upload of scoped variables failed: " + e.getMessage());
        }
        return parametersFromTheScope;
    }

    private void createPinnedBuffersForScopedVariables(PArray<inT> input) {
        Object[] parametersFromTheScope = getParametersFromTheScope(input);

        int numDevices = getNumberOfCurrentDevices();
        if (parametersFromTheScope.length > 0) {
            initLists(numDevices);
        }

        int indexVar = 0;
        for (Object scopeVariable : parametersFromTheScope) {
            if (scopeVariable.getClass().isArray()) {
                createPinnedMemoryForJavaArray(scopeVariable, numDevices);
                indexVar++;
            } else if (scopeVariable.getClass() == Integer.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_int, indexVar++));
            } else if (scopeVariable.getClass() == Long.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_long, indexVar++));
            } else if (scopeVariable.getClass() == Double.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_double, indexVar++));
            } else if (scopeVariable.getClass() == Float.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_float, indexVar++));
            } else if (scopeVariable.getClass() == Short.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_short, indexVar++));
            } else if (scopeVariable.getClass() == Byte.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_char, indexVar++));
            } else if (scopeVariable.getClass() == Character.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_char, indexVar++));
            } else if (scopeVariable.getClass() == Boolean.class) {
                scalarVariableList.add(new ScalarVarInfo(scopeVariable, Sizeof.cl_char, indexVar++));
            } else {
                throw new UnsupportedOperationException("Scope var not supported. Only supported arrays." + scopeVariable.getClass());
            }
        }
    }

    private boolean isKernelCompilationFailed() {
        return this.compilationFailed;
    }

    private static GraalAcceleratorPlatform getGraalOCLPlatform() {
        // There is no guarantee the GraalAcceleratorPlatform is prepared. The check is needed
        try {
            OCLRuntimeUtils.waitForTheOpenCLInitialization();
        } catch (InterruptedException e) {
            throw new RuntimeException("OpenCL device not found");
        }
        GraalAcceleratorSystem system = GraalAcceleratorSystem.getInstance();
        GraalAcceleratorPlatform platform = system.getPlatform();
        return platform;
    }

    /**
     * For multiple device.
     */
    private static OCLGraalAcceleratorDevice getOpenCLDevice(int idx) {
        GraalAcceleratorPlatform platform = getGraalOCLPlatform();
        return (OCLGraalAcceleratorDevice) platform.getDevice(idx);
    }
}
