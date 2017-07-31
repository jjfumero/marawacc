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

package uk.ac.ed.accelerator.ocl;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.jocl.Pointer;

import uk.ac.ed.accelerator.common.ErrorMessages;
import uk.ac.ed.accelerator.ocl.ParamInfoDirection.Direction;
import uk.ac.ed.accelerator.ocl.helper.MetaInfoInput;
import uk.ac.ed.accelerator.ocl.helper.MetaInfoOutput;
import uk.ac.ed.accelerator.ocl.helper.MetaInfoOutputUnroll;
import uk.ac.ed.accelerator.ocl.helper.MetaInfoParameters;
import uk.ac.ed.accelerator.ocl.helper.TypeUtil;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorOCLInfo;
import uk.ac.ed.accelerator.ocl.runtime.AcceleratorType;
import uk.ac.ed.accelerator.ocl.runtime.GlobalDataPipeline;
import uk.ac.ed.accelerator.profiler.Profiler;
import uk.ac.ed.accelerator.profiler.ProfilerType;
import uk.ac.ed.accelerator.utils.ArrayPackage;
import uk.ac.ed.accelerator.utils.PipelineIndexInfo;
import uk.ac.ed.accelerator.wocl.GraalOCLConstants;
import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.RuntimeObjectTypeInfo;

public class DataTypeRuntimePreparation {

    public static Class<?>[] createAndSetClassTypes(RuntimeObjectTypeInfo[] input, RuntimeObjectTypeInfo[] output, AcceleratorType[] scope) {

        int length = input.length + output.length + scope.length;

        Class<?>[] klassTypes = new Class[length];

        int i = 0;
        for (; i < input.length; i++) {
            klassTypes[i] = input[i].getClassObject();
        }

        i += scope.length;
        int j = 0;
        for (; j < output.length; i++) {
            klassTypes[i] = output[j].getClassObject();
            j++;
        }
        return klassTypes;
    }

    @SuppressWarnings("unused")
    public static void processWrapperScopeArray(Object[] inputParameters, Object[] inputSimple, String type, int[] sizes, boolean[] is1D, boolean partialCopy, int from, int to, int k, int i)
                    throws Exception {
        if (type.equals("[I")) {
            int size = ((Integer[]) (inputParameters[i])).length;
            int[] copyArrayPrimitive = new int[size];
            from = 0;
            to = copyArrayPrimitive.length;
            int idx = 0;
            for (int j = from; j < to; j++) {
                copyArrayPrimitive[idx] = ((Integer[]) inputParameters[i])[j];
                idx++;
            }
            inputSimple[k] = copyArrayPrimitive;
            sizes[k] = size;
            is1D[k] = true;
        } else if (type.equals("[F")) {
            int size = ((Float[]) (inputParameters[i])).length;

            float[] copyArrayPrimitive = new float[size];

            from = 0;
            to = copyArrayPrimitive.length;

            int idx = 0;
            for (int j = from; j < to; j++) {
                copyArrayPrimitive[idx] = ((Float[]) inputParameters[i])[j];
                idx++;
            }
            inputSimple[k] = copyArrayPrimitive;
            sizes[k] = size;
            is1D[k] = true;
        } else if (type.equals("[D")) {
            int size = ((Double[]) (inputParameters[i])).length;
            double[] copyArrayPrimitive = new double[size];

            from = 0;
            to = copyArrayPrimitive.length;

            int idx = 0;
            for (int j = from; j < to; j++) {
                copyArrayPrimitive[idx] = ((Double[]) inputParameters[i])[j];
                idx++;
            }
            inputSimple[k] = copyArrayPrimitive;
            sizes[k] = size;
            is1D[k] = true;
        } else if (type.equals("[S")) {
            int size = ((Short[]) (inputParameters[i])).length;
            short[] copyArrayPrimitive = new short[size];

            from = 0;
            to = copyArrayPrimitive.length;

            int idx = 0;
            for (int j = from; j < to; j++) {
                copyArrayPrimitive[idx] = ((Short[]) inputParameters[i])[j];
                idx++;
            }
            inputSimple[k] = copyArrayPrimitive;
            sizes[k] = size;
            is1D[k] = true;
        } else {
            throw new Exception(ErrorMessages.DATA_NOT_SUPPORTED);
        }
    }

    public static void processScopeVariables(int jdx, Object[] inputParameters, Object[] inputSimple, int[] sizes, boolean[] is1D, boolean partialCopy, int from, int to) throws Exception {
        // External variables from the scope

        for (int i = 1, k = jdx; i < inputParameters.length; i++, k++) {

            Class<?> inputClass = inputParameters[i].getClass();

            if (!inputClass.isArray()) {
                inputSimple[k] = inputParameters[i];
                is1D[k] = true;
                sizes[k] = 1;
            } else {

                String type = TypeUtil.getTypeFromArrayNonPrimitive(inputClass);

                if (type == null) {
                    // simple data type
                    type = inputClass.getName();
                    if (type.equals("[F")) {

                        // Assumption. For varialbes inserted via scope, we assume we need to whole
                        // array to process. We cant split it
                        int size = ((float[]) (inputParameters[i])).length;
                        inputSimple[k] = inputParameters[i];
                        sizes[k] = size;
                        is1D[k] = true;
                    } else {
                        type = TypeUtil.getTypeFromArray2DNonPrimitive(inputClass);
                        if (type != null) {
                            // The marshaller will manage this later on
                            inputSimple[k] = inputParameters[i];
                            sizes[k] = -1;
                        } else {
                            throw new RuntimeException("[Data Type not supported] : " + type);
                        }
                    }
                } else {
                    processWrapperScopeArray(inputParameters, inputSimple, type, sizes, is1D, partialCopy, from, to, k, i);
                }
                if (!ParallelOptions.UseSimplePipeline.getValue()) {
                    inputParameters[i] = null;
                }
            }
        }
    }

    public static Object getOutputReferencesFromGlobalStorage(ArrayPackage arrayPackage, boolean[] flatten, int[] totalSize, int idxReference, boolean partialCopy, int from, int to) {
        Object outputSimple = ((PArray<?>) arrayPackage.getArrayOutput()).getArrayReference(idxReference);
        flatten[0] = true;
        totalSize[0] = partialCopy ? (to - from) : ((PArray<?>) arrayPackage.getArrayOutput()).size();
        return outputSimple;
    }

    // XXX Refactor this method
    public static Object processOutput(Object[] outputParameter, int[] fromTo, int[] totalSize, boolean[] is1D, ArrayPackage arrayPackage, boolean[] flatten, AcceleratorOCLInfo acceleratorOCLInfo)
                    throws Exception {

        if (ParallelOptions.UseSimplePipeline.getValue()) {
            outputParameter = GlobalDataPipeline.getInstance().getOutput();
        }

        PipelineIndexInfo info = PipelineIndexInfo.getInstance();
        int from = -1;
        int to = -1;
        boolean partialCopy = false;
        if (fromTo != null) {
            from = fromTo[0];
            to = fromTo[1];
            partialCopy = true;
        } else if (!info.isEmpty()) {
            // Provide from-to
            long tid = Thread.currentThread().getId();
            int[] fromToA = info.getFromTo(tid);
            from = fromToA[0];
            to = fromToA[1];
            partialCopy = true;
        }

        Object outputSimple = null;
        long start = System.nanoTime();
        String type = TypeUtil.getTypeFromArrayNonPrimitive(outputParameter[0].getClass());  // XXX:
        long end = System.nanoTime();
        Profiler.getInstance().put(ProfilerType.M_TYPEFROMARRAY, (end - start));

        // Improve this control type. [, [[, and so on
        if (type == null) {
            switch (acceleratorOCLInfo.getClassOutput().getArrayDataType()) {
            // outputDataType represents output from the lambda function, not the map
            // Therefore, if the output of the lambda is an array, the output of the
            // map itself is a 2-dimensional array
                case SHORT:
                    if (acceleratorOCLInfo.getClassOutput().getArrayDim() == 1) {
                        int size = (partialCopy) ? (to - from) : ((Short[][]) (outputParameter[0])).length;
                        int ncolumns = acceleratorOCLInfo.getClassOutput().getNewSizeforMultidevice() == -1 ? ((Short[][]) (outputParameter[0])).length
                                        : acceleratorOCLInfo.getClassOutput().getNewSizeforMultidevice();
                        short[][] copyArrayPrimitive = new short[size][ncolumns];
                        outputSimple = copyArrayPrimitive;
                    }
                    break;
                case FLOAT:
                    if (acceleratorOCLInfo.getClassOutput().getArrayDim() == 1) {
                        int size = (partialCopy) ? (to - from) : ((Float[][]) (outputParameter[0])).length;
                        int ncolumns = acceleratorOCLInfo.getClassOutput().getNewSizeforMultidevice() == -1 ? ((Float[][]) (outputParameter[0])).length
                                        : acceleratorOCLInfo.getClassOutput().getNewSizeforMultidevice();
                        float[][] copyArrayPrimitive = new float[size][ncolumns];
                        outputSimple = copyArrayPrimitive;
                    }
                    break;
                default:
                    break;
            }
        } else if (type.equals("[I")) {
            if ((arrayPackage != null) /* && (arrayPackage.isInternalBufferAllocationOutput())) */) {
                outputSimple = getOutputReferencesFromGlobalStorage(arrayPackage, flatten, totalSize, 0, partialCopy, from, to);
            } else {
                int size = (partialCopy) ? (to - from) : ((Integer[]) (outputParameter[0])).length;
                int[] copyArrayPrimitive = new int[size];
                outputSimple = copyArrayPrimitive;
                totalSize[0] = size;
            }

            is1D[0] = true;
        } else if (type.equals("[F")) {

            if ((arrayPackage != null) /* && (arrayPackage.isInternalBufferAllocationOutput())) */) {
                outputSimple = getOutputReferencesFromGlobalStorage(arrayPackage, flatten, totalSize, 0, partialCopy, from, to);
            } else {
                int size = (partialCopy) ? (to - from) : ((Float[]) (outputParameter[0])).length;
                start = System.nanoTime();
                float[] copyArrayPrimitive = new float[size];
                end = System.nanoTime();
                Profiler.getInstance().put(ProfilerType.M_OUTPUTJAVAALLOCATION, (end - start));
                outputSimple = copyArrayPrimitive;
            }
            is1D[0] = true;

        } else if (type.equals("[D")) {

            if ((arrayPackage != null) /* && (arrayPackage.isInternalBufferAllocationOutput())) */) {
                outputSimple = getOutputReferencesFromGlobalStorage(arrayPackage, flatten, totalSize, 0, partialCopy, from, to);
            } else {
                int size = (partialCopy) ? (to - from) : ((Double[]) (outputParameter[0])).length;
                start = System.nanoTime();
                double[] copyArrayPrimitive = new double[size];
                end = System.nanoTime();
                Profiler.getInstance().put(ProfilerType.M_OUTPUTJAVAALLOCATION, (end - start));
                outputSimple = copyArrayPrimitive;
            }
            is1D[0] = true;
            return outputSimple;

        } else if (type.equals("[S")) {
            if ((arrayPackage != null) /* && (arrayPackage.isInternalBufferAllocationOutput())) */) {
                outputSimple = getOutputReferencesFromGlobalStorage(arrayPackage, flatten, totalSize, 0, partialCopy, from, to);
            } else {
                int size = (partialCopy) ? (to - from) : ((Short[]) (outputParameter[0])).length;
                short[] copyArrayPrimitive = new short[size];
                outputSimple = copyArrayPrimitive;
                totalSize[0] = size;
            }
            is1D[0] = true;
        }
        return outputSimple;
    }

    public static MetaInfoOutputUnroll getUnrolledOutputNewAPI(MetaInfoOutput meta, int[] fromTo, ArrayPackage arrayPackage, AcceleratorOCLInfo acceleratorOCLInfo) throws Exception {

        AcceleratorType[] datatypeOutputList = acceleratorOCLInfo.getOCLOutput();

        int realDim = datatypeOutputList.length;
        if (realDim != 0) {

            meta.hideTuple();

            int index = 0;
            Object[] out = new Object[realDim];
            int[] sizes = new int[realDim];
            boolean[] flatten = new boolean[realDim];
            int[] totalSize = new int[1];

            for (int i = 0; i < datatypeOutputList.length; i++) {
                AcceleratorType t = datatypeOutputList[i];

                long start = System.nanoTime();
                if (fromTo != null) {
                    out[index] = getOutputReferencesFromGlobalStorage(arrayPackage, flatten, totalSize, i, true, fromTo[0], fromTo[1]);
                } else {
                    out[index] = getOutputReferencesFromGlobalStorage(arrayPackage, flatten, totalSize, i, false, 0, 0);
                }
                long stop = System.nanoTime();
                Profiler.getInstance().put(ProfilerType.M_PROCESS_GET_OUT_REFERENCES, (stop - start));
                sizes[index] = totalSize[0];
                meta.incNumArrays();
                index++;
            }
            return new MetaInfoOutputUnroll(out, sizes);
        } else {
            return null;
        }
    }

    public static MetaInfoParameters processParametersSimpleDataType(HashMap<Direction, Object[]> parameters, int[] fromTo, ArrayPackage arrayPackage, AcceleratorOCLInfo acceleratorOCLInfo)
                    throws Exception {

        long start = System.nanoTime();
        MetaInfoInput info = processInput(parameters.get(Direction.INPUT), fromTo, arrayPackage, acceleratorOCLInfo);
        long end = System.nanoTime();
        Profiler.getInstance().put(ProfilerType.M_PROCESS_INPUT, (end - start));

        Object[] inputSimpleDataType = info.getObject();
        int finalSize = inputSimpleDataType.length;

        MetaInfoOutput meta = new MetaInfoOutput();

        start = System.nanoTime();
        MetaInfoOutputUnroll infoUnroll = getUnrolledOutputNewAPI(meta, fromTo, arrayPackage, acceleratorOCLInfo);
        end = System.nanoTime();
        Profiler.getInstance().put(ProfilerType.M_UNROLL_OUTPUT, (end - start));

        Object[] outputUnrolled = null;
        if (infoUnroll != null) {
            outputUnrolled = infoUnroll.getObject();
        }
        if (outputUnrolled != null && meta.isTupleHidden()) {
            finalSize += meta.getNumArrays();
        } else if (outputUnrolled != null && !meta.isTupleHidden()) {
            finalSize += meta.getNumArrays() + 1;
        } else {
            finalSize += 1;
        }

        Object[] finalParameters = new Object[finalSize];

        boolean[] ioMask = new boolean[finalSize];
        boolean[] isPrimitive = new boolean[finalSize];
        int[] sizes = new int[finalSize];
        boolean[] flatten = new boolean[finalSize];
        int[] direction = new int[finalSize];

        for (int i = 0; i < inputSimpleDataType.length; i++) {
            ioMask[i] = true;
            isPrimitive[i] = info.getIs1D()[i];
            sizes[i] = info.getSizes()[i];
            flatten[i] = info.getFlatten()[i];
            direction[i] = GraalOCLConstants.COPY_IN;
        }

        start = System.nanoTime();
        System.arraycopy(inputSimpleDataType, 0, finalParameters, 0, inputSimpleDataType.length);

        end = System.nanoTime();
        Profiler.getInstance().put(ProfilerType.M_ARRAY_COPY, (end - start));

        int idx = inputSimpleDataType.length;
        int[] outputSize = new int[1];
        boolean[] is1DOut = new boolean[1];
        boolean[] isOutFlatten = new boolean[1];
        int outputIndex = idx;
        int indexUnrolledStructs = 0;

        if (!(meta.isTupleHidden())) {

            start = System.nanoTime();
            Object ouput = processOutput(parameters.get(Direction.OUTPUT), fromTo, outputSize, is1DOut, arrayPackage, isOutFlatten, acceleratorOCLInfo);
            end = System.nanoTime();
            Profiler.getInstance().put(ProfilerType.M_PROCESS_OUTPUT, (end - start));

            finalParameters[idx] = ouput;
            sizes[idx] = outputSize[0];
            isPrimitive[idx] = is1DOut[0];
            flatten[idx] = isOutFlatten[0];
            direction[idx] = GraalOCLConstants.COPY_OUT;
            outputIndex = idx;
            idx++;
        }

        if (outputUnrolled != null) {

            indexUnrolledStructs = idx;
            int i = 0;
            for (Object o : outputUnrolled) {

                finalParameters[idx] = o;
                sizes[idx] = infoUnroll.sizes[i];
                isPrimitive[idx] = false;
                direction[idx] = GraalOCLConstants.COPY_OUT;
                flatten[idx] = true;
                idx++;
                i++;
            }
        }

        MetaInfoParameters ioParameters = new MetaInfoParameters(finalParameters, ioMask, isPrimitive, sizes, flatten, arrayPackage, direction, outputIndex, indexUnrolledStructs);
        return ioParameters;
    }

    public static MetaInfoInput processInput(Object[] inputParameters, int[] fromTo, ArrayPackage arrayPackage, AcceleratorOCLInfo typeOCLDynamicInfo) throws Exception {

        int from = -1;
        int to = -1;
        boolean partialCopy = false;

        if (fromTo != null) {
            from = fromTo[0];
            to = fromTo[1];
            partialCopy = true;
        }

        int requiredInputs = ((PArray<?>) arrayPackage.getArrayInput()).grade();

        for (AcceleratorType type : typeOCLDynamicInfo.getOCLScope()) {
            if (type != null) {
                requiredInputs += type.getNumberOfComponents();
            }
        }

        Object[] inputSimple = new Object[requiredInputs];
        int[] sizes = new int[requiredInputs];
        boolean[] is1D = new boolean[requiredInputs];
        boolean[] flatten = new boolean[requiredInputs];

        int jdx = 0;
        if ((arrayPackage != null) /* && (arrayPackage.isInternalBufferAllocationOutput())) */) {
            if (inputParameters[jdx] instanceof PArray) {
                int grade = ((PArray<?>) arrayPackage.getArrayInput()).grade();
                for (int i = 0; i < grade; i++) {
                    inputSimple[jdx] = ((PArray<?>) arrayPackage.getArrayInput()).getArrayReference(i);
                    sizes[i] = (partialCopy) ? (to - from) : ((PArray<?>) arrayPackage.getArrayInput()).size();
                    jdx++;
                    is1D[i] = true;
                    flatten[i] = true;
                }
            }
        }

        if (requiredInputs != jdx) {
            processScopeVariables(jdx, inputParameters, inputSimple, sizes, is1D, partialCopy, from, to);
        }

        return new MetaInfoInput(inputSimple, sizes, is1D, flatten);
    }

    public static OutputManager serializeOutput(ArrayPackage arrayPackage) {
        OutputManager manager = new OutputManager();
        if ((arrayPackage != null)) {
            manager.setArrayInGlobalStorage(arrayPackage);
            return manager;
        }

        return manager;
    }

    public static Pointer getPointer(Object obj) throws Exception {
        Method m;
        final String METHOD_NAME_TO_LOOK_UP = "to";
        try {
            m = Pointer.class.getMethod(METHOD_NAME_TO_LOOK_UP, new Class[]{obj.getClass()});
            return (Pointer) m.invoke(null, obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
