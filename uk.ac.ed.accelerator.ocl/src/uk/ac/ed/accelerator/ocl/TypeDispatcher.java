package uk.ac.ed.accelerator.ocl;

import java.util.HashMap;

import jdk.vm.ci.meta.JavaKind;

import org.jocl.CL;

import uk.ac.ed.accelerator.common.GraalAcceleratorDevice;
import uk.ac.ed.accelerator.common.GraalAcceleratorOptions;
import uk.ac.ed.accelerator.wocl.OCLGraalAcceleratorDevice;

public class TypeDispatcher {

    private TypeDispatcher() {

    }

    private static String getVectorType(JavaKind kind, GraalAcceleratorDevice device) {
        OCLGraalAcceleratorDevice oclDevice = (OCLGraalAcceleratorDevice) device;
        HashMap<Integer, Integer> deviceVectorTypes = oclDevice.getDeviceInfo().getDeviceVectorTypes();
        if (kind.equals(JavaKind.Char)) {
            Integer value = deviceVectorTypes.get(CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR);
            return "char" + value + " ";
        } else if (kind.equals(JavaKind.Short)) {
            Integer value = deviceVectorTypes.get(CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT);
            return "short" + value + " ";
        } else if (kind.equals(JavaKind.Int)) {
            Integer value = deviceVectorTypes.get(CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT);
            return "int" + value + " ";
        } else if (kind.equals(JavaKind.Long)) {
            Integer value = deviceVectorTypes.get(CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG);
            return "long" + value + " ";
        } else if (kind.equals(JavaKind.Float)) {
            Integer value = deviceVectorTypes.get(CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT);
            return "float" + value + " ";
        } else if (kind.equals(JavaKind.Double)) {
            Integer value = deviceVectorTypes.get(CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE);
            return "double" + value + " ";
        }
        return null;
    }

    private static String getScalarType(JavaKind kind) {
        if (kind.equals(JavaKind.Char)) {
            return "char ";
        } else if (kind.equals(JavaKind.Short)) {
            return "short ";
        } else if (kind.equals(JavaKind.Int)) {
            return "int ";
        } else if (kind.equals(JavaKind.Long)) {
            return "long ";
        } else if (kind.equals(JavaKind.Float)) {
            return "float ";
        } else if (kind.equals(JavaKind.Double)) {
            return "double ";
        }
        return null;
    }

    public static String getType(JavaKind kind, GraalAcceleratorDevice device) {
        if (GraalAcceleratorOptions.useVectorTypes) {
            return getVectorType(kind, device);
        } else {
            return getScalarType(kind);
        }
    }
}
