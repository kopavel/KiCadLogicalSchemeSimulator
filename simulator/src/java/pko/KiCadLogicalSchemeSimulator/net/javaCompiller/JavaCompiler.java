/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
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
 */
package pko.KiCadLogicalSchemeSimulator.net.javaCompiller;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JavaCompiler {
    private static final Method defineClassMethod;
    private static final Method privateLookupInMethod;
    private static final MethodHandles.Lookup lookup;
    private static final javax.tools.JavaCompiler compiler;
    private static final List<String> optionList;
    static {
        try {
            lookup = MethodHandles.lookup();
            privateLookupInMethod = MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
            privateLookupInMethod.setAccessible(true);
            defineClassMethod = MethodHandles.Lookup.class.getDeclaredMethod("defineClass", byte[].class);
            defineClassMethod.setAccessible(true);
            compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("No Java compiler available");
            }
            String path = JavaCompiler.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            if (System.getProperty("os.name").toLowerCase().contains("win") && path.startsWith("/")) {
                path = path.substring(1);
            }
            Log.info(JavaCompiler.class, "Use class path for compiler: {}", path);
            optionList = new ArrayList<>();
            optionList.add("-cp");
            optionList.add(path);
            optionList.add("-proc:none");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        OutPin scr = new OutPin("test", new SchemaPart("testPsrt", "") {
            @Override
            public void initOuts() {
            }
        });
        String suffix = "$test";
        String simpleClassName = scr.getClass().getSimpleName();
        String fullClassName = scr.getClass().getName() + suffix;
        String sourceCode;
        try (InputStream is = new FileInputStream(
                "D:\\soft_a\\verilog\\KiCadLogicalSchemeSimulator\\simulator\\src\\java\\pko\\KiCadLogicalSchemeSimulator\\api\\wire\\OutPin.java")) {
            sourceCode = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            sourceCode = sourceCode.replace("destinations", "dest");
            sourceCode = sourceCode.replace("public class " + simpleClassName, "public class " + simpleClassName + suffix);
            sourceCode = sourceCode.replace("public " + simpleClassName, "public " + simpleClassName + suffix);
            sourceCode = sourceCode.replace("extends Pin", "extends OutPin");
            boolean compiled = compileJavaSource(scr.getClass(), fullClassName, sourceCode);
            if (compiled) {
                Class<?> clazz = Class.forName(fullClassName);
                scr = (OutPin) clazz.getDeclaredConstructor(scr.getClass(), String.class).newInstance(scr, suffix);
            } else {
                throw new RuntimeException("can't compile");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean compileJavaSource(Class<?> srcClass,
            String className,
            String sourceCode) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Log.trace(JavaCompiler.class, "Compile source \n{}", sourceCode);
        InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(compiler.getStandardFileManager(null, null, null));
        JavaFileObject javaFileObject = new InMemoryJavaFileObject(className, sourceCode);
        List<JavaFileObject> javaFileObjects = Collections.singletonList(javaFileObject);
        javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, optionList, null, javaFileObjects);
        if (task.call()) {
            fileManager.getClassBytes().entrySet()
                    .stream().sorted(Map.Entry.<String, ByteArrayOutputStream>comparingByKey().reversed()).forEach(entry -> {
                           try {
                               Log.debug(JavaCompiler.class, "Load class {}", entry.getKey());
                               loadClass(srcClass, entry.getValue().toByteArray());
                           } catch (Exception e) {
                               throw new RuntimeException(e);
                           }
                       });
            return true;
        } else {
            return false;
        }
    }

    private static void loadClass(Class<?> srcClass, byte[] byteCode) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodHandles.Lookup privateLookup = (MethodHandles.Lookup) privateLookupInMethod.invoke(null, srcClass, lookup);
        //noinspection PrimitiveArrayArgumentToVarargsMethod
        defineClassMethod.invoke(privateLookup, byteCode);
    }

    static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        protected InMemoryJavaFileObject(String className, String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    }

    @Getter
    static class InMemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, ByteArrayOutputStream> classBytes = new HashMap<>();

        public InMemoryJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            classBytes.put(className, outputStream);
            return new SimpleJavaFileObject(URI.create("file:///" + className.replace('.', '/') + kind.extension), kind) {
                @Override
                public OutputStream openOutputStream() {
                    return outputStream;
                }
            };
        }
    }
}
