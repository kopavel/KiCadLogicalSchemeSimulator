/*
 * Copyright (c) 2024 Pavel Korzh
 * <p>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package lv.pko.KiCadLogicalSchemeSimulator.tools;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.Manipulable;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ClassManipulator extends SchemaPart {
    protected ClassManipulator(String id, String sParam) {
        super(id, sParam);
    }

    public static void main(String[] args) throws Exception {
        ClassManipulator instance = new ClassManipulator("test", "");
        instance.test();
    }

    public void test() throws Exception {
        InPin inPin = new InPin("1", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                long correctedState = correctState(newState);
            }
        };
        inPin.offset = -1;
        inPin.mask = 255;
        OutPin outPin = new OutPin("testOut", this, 12);
        outPin.addDest(inPin);
        long iterations = 100000000L;
        for (long i = 0; i < iterations; i++) {
            outPin.setState(i);
        }
        long time = System.nanoTime();
        for (long i = 0; i < iterations; i++) {
            outPin.setState(i);
        }
        System.out.println(System.nanoTime() - time);
        if (inPin.offset == 0) {
            inPin = replaceMethodCall(inPin, "correctState", "getCorrectedStateNoOffset");
        } else if (inPin.offset > 0) {
            inPin = replaceMethodCall(inPin, "correctState", "getCorrectedStatePositiveOffset");
        } else {
            inPin = replaceMethodCall(inPin, "correctState", "getCorrectedStateNegativeOffset");
        }
        inPin.offset = 1;
        inPin.mask = 255;
        outPin = replaceMethodCall(outPin, "setState", "setSingleState");
        outPin.addDest(inPin);
        for (long i = 0; i < iterations; i++) {
            outPin.setState(i);
        }
        time = System.nanoTime();
        for (long i = 0; i < iterations; i++) {
            outPin.setState(i);
        }
        System.out.println(System.nanoTime() - time);
    }

    public <T extends Manipulable, R extends T> R replaceMethodCall(T instance, String methodForReplace, String replacingMethod) throws Exception {
        // Get the original class bytecode
        byte[] originalClassBytes;
        String className = instance.getClass().getName().replace('.', '/') + ".class";
        try (InputStream resourceAsStream = instance.getClass().getClassLoader().getResourceAsStream(className)) {
            //noinspection DataFlowIssue
            originalClassBytes = resourceAsStream.readAllBytes();
        }
        // Define a new class loader to load the modified class bytecode
        Constructor<R> constructor = getConstructor(instance, methodForReplace, replacingMethod, originalClassBytes);
        // Create a new instance of the modified class using the constructor
        return constructor.newInstance(instance.getConstructorParameters(constructor.getParameterTypes()));
    }

    @Override
    public void initOuts() {
    }

    private <T extends Manipulable, R extends T> Constructor<R> getConstructor(T instance,
            String methodForReplace,
            String replacingMethod,
            byte[] originalClassBytes) throws ClassNotFoundException, NoSuchMethodException {
        // Get the ClassLoader of the original instance
        ClassLoader loader = instance.getClass().getClassLoader();
        if (loader == null) {
            throw new ClassNotFoundException("ClassLoader not found for the instance");
        }
        try {
            // Get the defineClass method using reflection
            Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClassMethod.setAccessible(true);
            // Load the modified class using the defineClass method
            // Create a new ClassWriter
            byte[] modifiedClassBytes = getModifiedBytes(methodForReplace, replacingMethod, originalClassBytes, instance.getClass());
            Class<?> modifiedClass =
                    (Class<?>) defineClassMethod.invoke(loader, instance.getClass().getName() + "$Modified", modifiedClassBytes, 0, modifiedClassBytes.length);
            // Get the constructor of the modified class
            return (Constructor<R>) Arrays.stream(modifiedClass.getConstructors()).findFirst().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getModifiedBytes(String methodForReplace, String replacingMethod, byte[] originalClassBytes, Class<?> instanceClass) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        // Create a ClassVisitor to modify the original class bytecode
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                access &= ~Opcodes.ACC_PRIVATE;
                access |= Opcodes.ACC_PUBLIC;                // Modify the class name here
                super.visit(version, access, name + "$Modified", signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("<init>")) {
                    return null;
                }
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                // Replace the bytecode of the method named "methodForReplace" with "replacingMethod"
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (name.equals(methodForReplace)) {
                            super.visitMethodInsn(opcode, owner, replacingMethod, descriptor, isInterface);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                // Generate constructors
                for (Constructor<?> constructor : instanceClass.getDeclaredConstructors()) {
                    generateConstructor(constructor);
                }
                super.visitEnd();
            }

            private void generateConstructor(Constructor<?> constructor) {
                // Get parameter types of the constructor
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                // Generate the descriptor for the constructor based on parameter types
                StringBuilder constructorDescriptor = new StringBuilder("(");
                for (Class<?> paramType : parameterTypes) {
                    constructorDescriptor.append(Type.getDescriptor(paramType));
                }
                constructorDescriptor.append(")V");
                // Generate bytecode to replicate the constructor
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "<init>", constructorDescriptor.toString(), null, null);
                mv.visitCode();
                // Load 'this' onto the stack
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                // Load constructor parameters onto the stack
                int localVarIndex = 1;
                for (Class<?> paramType : parameterTypes) {
                    mv.visitVarInsn(Type.getType(paramType).getOpcode(Opcodes.ILOAD), localVarIndex);
                    localVarIndex += Type.getType(paramType).getSize();
                }
                // Call superclass constructor
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(instanceClass), "<init>", constructorDescriptor.toString(), false);
                // Set max stack and max locals
                mv.visitMaxs(0, 0);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitEnd();
            }
        };
        // Modify the original class bytecode
        ClassReader cr = new ClassReader(originalClassBytes);
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        //make extending original class
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, instanceClass.getName().replace('.', '/') + "$Modified", null, instanceClass.getName().replace('.', '/'), null);
        byte[] withConstructors = cw.toByteArray();
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cr = new ClassReader(withConstructors);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        MethodNode replace = null;
        MethodNode replaceWith = null;
// Find the MethodNode objects for method A and method B
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(methodForReplace)) {
                replace = method;
            } else if (method.name.equals(replacingMethod)) {
                replaceWith = method;
            }
        }
// Check if both methods are found
        if (replace != null && replaceWith != null) {
            // Replace the instructions of method A with the instructions of method B
            replace.instructions = replaceWith.instructions;
            // Modify the access flags of method A to match method B if needed
            replace.access = replaceWith.access;
            // Generate bytecode for the modified class
            classNode.accept(cw);
            return cw.toByteArray();
        } else {
            return withConstructors;
        }
    }
}
