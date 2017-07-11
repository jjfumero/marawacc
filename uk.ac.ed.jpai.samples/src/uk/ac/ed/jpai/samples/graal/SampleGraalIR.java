package uk.ac.ed.jpai.samples.graal;

import java.lang.reflect.Method;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.ed.accelerator.ocl.runtime.GraalIRConversion;
import uk.ac.ed.accelerator.ocl.runtime.GraalIRUtilities;
import uk.ac.ed.compiler.utils.JITGraalCompilerUtil;

import com.oracle.graal.nodes.StructuredGraph;
import com.oracle.graal.phases.util.Providers;

public class SampleGraalIR {

    // Test method to compile.
    public static int methodToCompile(int n) {
        if (n < 3) {
            n++;
        } else {
            n += 2;
        }
        return n;
    }

    // Test method to compile.
    public static int forLoop(int n) {
        int val = 0;
        for (int i = 0; i < n; i++) {
            val += i;
        }
        return val;
    }

    public static void graalIRForMethod(String methodName) {

        String nameMethod = methodName;
        Class<?> klass = SampleGraalIR.class;

        JITGraalCompilerUtil compiler = new JITGraalCompilerUtil();
        Method method = compiler.getMethodFromName(klass, nameMethod);

        Providers providers = JITGraalCompilerUtil.getProviders();
        MetaAccessProvider metaAccess = providers.getMetaAccess();
        ResolvedJavaMethod resolvedJavaMethod = metaAccess.lookupJavaMethod(method);

        StructuredGraph graph = GraalIRConversion.createCFGGraalIR(resolvedJavaMethod);
        GraalIRUtilities.dumpGraph(graph, "graph");

    }

    public static void main(String[] args) {
        graalIRForMethod("methodToCompile");
        graalIRForMethod("forLoop");
        System.out.println("done");
    }
}
