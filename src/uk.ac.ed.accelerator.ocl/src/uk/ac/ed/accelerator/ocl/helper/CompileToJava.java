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
package uk.ac.ed.accelerator.ocl.helper;

import jdk.vm.ci.meta.JavaMethod;

import com.sun.xml.internal.ws.org.objectweb.asm.AnnotationVisitor;
import com.sun.xml.internal.ws.org.objectweb.asm.ClassWriter;
import com.sun.xml.internal.ws.org.objectweb.asm.MethodVisitor;
import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;

public class CompileToJava {

    private static AnnotationVisitor getAnnotation(ClassWriter cw, String originalName) {
        AnnotationVisitor av0 = cw.visitAnnotation("Lcom/oracle/graal/api/replacements/ClassSubstitution;", true);
        av0.visit("className", originalName);
        av0.visit("optional", Boolean.TRUE);
        av0.visitEnd();
        return av0;
    }

    private static MethodVisitor getMethodVisitor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        return mv;
    }

    public static byte[] generateBytecode(String newClazzName, String originalName, JavaMethod jm, String signature) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(51, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, newClazzName, null, "java/lang/Object", null);

        av0 = getAnnotation(cw, originalName);
        mv = getMethodVisitor(cw);

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, jm.getName(), signature, null, null);

        av0 = mv.visitAnnotation("Lcom/oracle/graal/api/replacements/MethodSubstitution;", true);
        av0.visit("forced", Boolean.TRUE);
        av0.visitEnd();

        mv.visitCode();

        // number of parameters.
        int paramCount = jm.getSignature().getParameterCount(false);
        System.out.println("param count: " + paramCount);
        // create object array to hold parameters.
        mv.visitInsn(Opcodes.ICONST_0 + paramCount);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitVarInsn(Opcodes.ASTORE, paramCount);

        // add parameters to object array.
        for (int i = 0; i < paramCount; i++) {
            mv.visitVarInsn(Opcodes.ALOAD, paramCount);
            mv.visitInsn(Opcodes.ICONST_0 + i);
            mv.visitVarInsn(Opcodes.ALOAD, i);
            mv.visitInsn(Opcodes.AASTORE);
        }

        int nextIndex = paramCount + 1;

        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/edinburgh/parallel/opencl/ParallelMethods", "methods", "Ljava/util/concurrent/ConcurrentHashMap;");
        mv.visitLdcInsn(newClazzName + "." + jm.getName());
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/oracle/graal/nodes/StructuredGraph");
        mv.visitVarInsn(Opcodes.ASTORE, nextIndex);
        mv.visitVarInsn(Opcodes.ALOAD, nextIndex);
        mv.visitVarInsn(Opcodes.ALOAD, paramCount);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/edinburgh/parallel/opencl/ParallelUtil", "run", "(Lcom/oracle/graal/nodes/StructuredGraph;[Ljava/lang/Object;)V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, nextIndex + 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
