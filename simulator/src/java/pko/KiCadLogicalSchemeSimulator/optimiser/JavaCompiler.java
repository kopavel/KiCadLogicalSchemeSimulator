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
package pko.KiCadLogicalSchemeSimulator.optimiser;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import javax.tools.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
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
//            optionList.add("-g:lines");
            optionList.add("-Xlint:none");
            optionList.add("-cp");
            optionList.add(path);
            optionList.add("-proc:none");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static boolean compileJavaSource(Class<?> srcClass, String className, String sourceCode) {
        Log.trace(JavaCompiler.class, "Compile source \n{}", sourceCode);
        InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(compiler.getStandardFileManager(null, null, null));
        JavaFileObject javaFileObject = new InMemoryJavaFileObject(className, sourceCode);
        List<JavaFileObject> javaFileObjects = Collections.singletonList(javaFileObject);
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList, null, javaFileObjects);
        if (task.call()) {
            fileManager.getClassBytes().entrySet()
                    .stream().sorted(Map.Entry.<String, ByteArrayOutputStream>comparingByKey().reversed()).forEach(entry -> {
                           try {
                               try {
                                   Class.forName(entry.getKey());
                                   Log.warn(JavaCompiler.class, "Class {} already loaded", srcClass.getName());
                               } catch (ClassNotFoundException ignore) {
                                   Log.debug(JavaCompiler.class, "Load class {}", entry.getKey());
                                   loadClass(srcClass, entry.getValue().toByteArray());
                               }
                               storeClass(entry.getKey(), entry.getValue().toByteArray());
                           } catch (Exception e) {
                               throw new RuntimeException(e);
                           }
                       });
            return true;
        } else {
            Log.error(JavaCompiler.class, "Can't compile source");
            for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                Log.error(JavaCompiler.class,
                        "{} at {}:{} -> {}",
                        diagnostic.getKind(),
                        diagnostic.getLineNumber(),
                        diagnostic.getColumnNumber(),
                        diagnostic.getMessage(Locale.getDefault()));
            }
            return false;
        }
    }

    private static void storeClass(String className, byte[] byteArray) throws IOException {
        Thread.ofVirtual().start(() -> {
            String path = Simulator.optimisedDir + File.separator + className.replace(".", File.separator) + ".class";
            String dirPath = path.substring(0, path.lastIndexOf(File.separator));
            try {
                Files.createDirectories(Paths.get(dirPath));
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path))) {
                    os.write(byteArray);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void loadClass(Class<?> srcClass, byte[] byteCode) throws InvocationTargetException, IllegalAccessException {
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
