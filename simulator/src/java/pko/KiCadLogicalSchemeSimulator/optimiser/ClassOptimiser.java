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
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.bus.MaskGroupBus;
import pko.KiCadLogicalSchemeSimulator.net.bus.SimpleBusToWireAdapter;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static pko.KiCadLogicalSchemeSimulator.tools.Utils.countLeadingSpaces;
import static pko.KiCadLogicalSchemeSimulator.tools.Utils.regexEscape;

public class ClassOptimiser<T> {
    private final Map<String, String> binds = new HashMap<>();
    private final T oldInstance;
    List<String> cutList = new ArrayList<>();
    private String suffix = "";
    private List<String> source;
    private int unrollSize;
    private boolean noAssert = true;

    public ClassOptimiser(T oldInstance) {
        this.oldInstance = oldInstance;
        //noinspection AssertWithSideEffects,ConstantValue
        assert !(noAssert = false);
    }

    public static void main(String[] args) {
        SchemaPart schemaPart = new SchemaPart("Test", "") {
            @Override
            public void initOuts() {
            }
        };
        MaskGroupBus out = new MaskGroupBus(new OutBus("test", schemaPart, 4), "test");
        for (int i = 0; i < 5; i++) {
            out.addDestination(new SimpleBusToWireAdapter(out, schemaPart.addInPin("in" + i)));
        }
        IModelItem<?> opt = out.getOptimised(true);
    }

    public ClassOptimiser<T> unroll(int size) {
        unrollSize = size;
        suffix += "_unroll" + size;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ClassOptimiser<T> cut(String cutName) {
        cutList.add(cutName);
        suffix += "_cut_" + cutName;
        return this;
    }

    public ClassOptimiser<T> bind(String bindName, long replacement) {
        bind(bindName, String.valueOf(replacement));
        return this;
    }

    public ClassOptimiser<T> bind(String bindName, boolean replacement) {
        bind(bindName, String.valueOf(replacement));
        return this;
    }

    public ClassOptimiser<T> bind(String bindName, String replacement) {
        binds.put(bindName, replacement);
        suffix += "_" + bindName + "_" + replacement.replaceAll("[^a-zA-Z0-9_$]", "_");
        return this;
    }

    @SuppressWarnings("unchecked")
    public T build() {
        try {
            if (suffix.isBlank()) {
                return oldInstance;
            }
            if (!noAssert) {
                suffix += "_ae";
            }
            String optimizedClassName = oldInstance.getClass().getSimpleName() + suffix;
            Class<?> dynamicClass;
            try {
                dynamicClass = Class.forName(oldInstance.getClass().getName() + suffix);
            } catch (ClassNotFoundException ignored) {
                Log.trace(JavaCompiler.class, "Load source for {}", oldInstance.getClass().getSimpleName());
                loadSource(oldInstance.getClass());
                Log.trace(JavaCompiler.class, "Process");
                String optimisedSource = process();
                Log.trace(JavaCompiler.class, "Compile");
                try {
                    storeSrc(oldInstance.getClass().getName() + suffix, optimisedSource);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (JavaCompiler.compileJavaSource(oldInstance.getClass(), optimizedClassName, optimisedSource)) {
                    Log.trace(JavaCompiler.class, "Return instance");
                    dynamicClass = Class.forName(oldInstance.getClass().getName() + suffix);
                } else {
                    Log.error(JavaCompiler.class,
                            "Optimised class compile was not successful, fall back to generic class, file name:" + oldInstance.getClass().getName() + suffix);
                    return oldInstance;
                }
            }
            // Create an instance and invoke the overridden method
            Constructor<?>[] constructors = dynamicClass.getDeclaredConstructors();
            if (constructors.length != 1) {
                Log.error(ClassOptimiser.class, "Constructor must be only one! has {}", constructors.length);
            }
            return (T) constructors[0].newInstance(oldInstance, suffix);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void storeSrc(String className, String source) throws IOException {
        Thread.ofVirtual().start(() -> {
            String srcPath = Simulator.optimisedDir + File.separator + className.replace(".", File.separator) + ".java";
            String dirPath = srcPath.substring(0, srcPath.lastIndexOf(File.separator));
            try {
                Files.createDirectories(Paths.get(dirPath));
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(srcPath))) {
                    os.write(source.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String process() {
        String iteratorPattern = null;
        String[] iteratorParams = null;
        boolean preserveBlock = false;
        boolean inAsserts = false;
        boolean inComment = false;
        int blockOffset = -1;
        int iteratorOffset = -1;
        StringBuilder resultSource = new StringBuilder();
        StringBuilder iteratorSource = new StringBuilder();
        StringBuilder blockSource = new StringBuilder();
        try {
            for (Iterator<String> lines = source.iterator(); lines.hasNext(); ) {
                String line = lines.next();
                if (line.contains("getOptimised(boolean keepSetters)")) {
                    resultSource.append(line).append("\n").append("return this;\n}");
                }
                int lineOffset = countLeadingSpaces(line);
                if (line.startsWith("import ") || line.startsWith("package ")) {
                    resultSource.append(line).append("\n");
                } else if (noAssert && line.trim().startsWith("assert")) {
                    if (!line.contains(";")) {
                        inAsserts = true; //multiline assert
                    }
                } else if (inAsserts) {
                    if (line.contains(";")) {
                        inAsserts = false; //multiline assert end
                    }
                } else {
                    if (!line.trim().startsWith("//")) {
                        if (line.contains("/*Optimiser ")) {
                            int i = 0;
                            String oldItemName = null;
                            Map<String, String> bindPatterns = new HashMap<>();
                            String[] params = line.substring(line.indexOf("/*Optimiser ") + 12, line.lastIndexOf("*/")).split(" ");
                            param_loop:
                            while (i < params.length) {
                                String command = params[i++];
                                switch (command) {
                                    case "block" -> {
                                        preserveBlock = true;
                                        String blockName = params[i++];
                                        if (cutList.contains(blockName)) {
                                            do {
                                                line = lines.next();
                                            } while (!line.contains("/*Optimiser ") ||
                                                    !(line.contains("blockend " + blockName + "*/") || line.contains("blockend " + blockName + " ")));
                                            break param_loop;//block removed — don't process more params
                                        }
                                    }
                                    case "constructor" -> {
                                        preserveBlock = true;
                                        blockOffset = countLeadingSpaces(line);
                                        line = lines.next();//load constructor definition
                                        int paramNamePos = line.indexOf(' ', line.indexOf('('));
                                        oldItemName = line.substring(paramNamePos, line.indexOf(",", paramNamePos));
                                        blockSource.append(line.replace(oldInstance.getClass().getSimpleName() + "(",
                                                oldInstance.getClass().getSimpleName() + suffix + "(")).append("\n");
                                        String superLine = lines.next();//"super" are here
                                        lineOffset = countLeadingSpaces(superLine);
                                        blockSource.append(superLine).append("\n");
                                    }
                                    case "unroll" -> {
                                        iteratorParams = params[i++].split(":");
                                        String iteratorItemType = getField(oldInstance.getClass(), iteratorParams[1]).getType().getComponentType().getSimpleName();
                                        iteratorPattern = "for (" + iteratorItemType + " " + iteratorParams[0] + " : " + iteratorParams[1] + ") {";
                                        String lineTab = " ".repeat(lineOffset);
                                        String blockTab = " ".repeat(blockOffset);
                                        for (int j = 0; j < unrollSize; j++) {
                                            blockSource.append(lineTab)
                                                       .append(iteratorParams[0])
                                                       .append(j)
                                                       .append(" = ")
                                                       .append(oldItemName)
                                                       .append(".")
                                                       .append(iteratorParams[1])
                                                       .append("[")
                                                       .append(j)
                                                       .append("];\n");
                                            //add before constructor;
                                            resultSource.append(blockTab).append(iteratorItemType).append(" ").append(iteratorParams[0]).append(j).append(";\n");
                                        }
                                    }
                                    case "bind" -> {
                                        String[] bindParams = params[i++].split(":");
                                        String bindName = bindParams[0];
                                        String bindPattern;
                                        if (bindParams.length == 2) {
                                            bindPattern = regexEscape(bindParams[1]);
                                        } else {
                                            bindPattern = regexEscape(bindName);
                                        }
                                        bindPatterns.put(bindPattern, bindName);
                                    }
                                }
                            }
                            if (!bindPatterns.isEmpty()) {
                                line = lines.next();
                                for (Map.Entry<String, String> bind : bindPatterns.entrySet()) {
                                    if (binds.containsKey(bind.getValue())) {
                                        line = line.replaceAll("(?<=\\W|^)" + bind.getKey() + "(?=\\W|$)", binds.get(bind.getValue()));
                                    }
                                }
                                if (iteratorOffset > -1) {
                                    iteratorSource.append(line).append("\n");
                                } else {
                                    blockSource.append(line).append("\n");
                                }
                                preserveBlock = true;
                            }
                        } else if (line.trim().startsWith("/*") || inComment) {
                            inComment = !line.contains("*/");
                        } else if (line.contains("public class " + oldInstance.getClass().getSimpleName())) {
                            //rename class definition
                            resultSource.append("public class ")
                                        .append(oldInstance.getClass().getSimpleName())
                                        .append(suffix)
                                        .append(" extends ")
                                        .append(oldInstance.getClass().getSimpleName())
                                        .append(" {\n");
                            blockOffset = -1;
                            blockSource = new StringBuilder();
                        } else if (!line.trim().isBlank() && blockOffset < 0 && lineOffset > 0) {
                            //block begin
                            blockOffset = lineOffset;
                            blockSource = new StringBuilder(line).append("\n");
                        } else if (lineOffset <= blockOffset && !line.isBlank()) {
                            //block end
                            if (line.trim().equals("}")) {
                                blockSource.append(line).append("\n");
                                if (preserveBlock) {
                                    resultSource.append("\n").append(blockSource);
                                }
                                blockOffset = -1;
                                blockSource = new StringBuilder();
                            } else {
                                //new block after "one line" block
                                blockOffset = lineOffset;
                                blockSource = new StringBuilder(line).append("\n");
                            }
                            preserveBlock = false;
                        } else if (iteratorPattern != null && line.contains(iteratorPattern)) {
                            //find iterator body - skip iterator definition
                            iteratorOffset = lineOffset;
                            preserveBlock = true;
                            iteratorSource = new StringBuilder();
                        } else if (iteratorOffset > -1) {
                            if (lineOffset > iteratorOffset) {
                                if (unrollSize == 0) {
                                    throw new RuntimeException("iterator size not provided");
                                }
                                //inside iterator block — accumulate whole block
                                iteratorSource.append(line).append("\n");
                            } else if (lineOffset == iteratorOffset && !blockSource.isEmpty()) {
                                //iterator block end — unroll it
                                for (int j = 0; j < unrollSize; j++) {
                                    blockSource.append(iteratorSource.toString().replaceAll("(?<=\\W|^)" + iteratorParams[0] + "(?=\\W|$)", iteratorParams[0] + j));
                                }
                                iteratorOffset = -1;
                            }
                        } else {
                            blockSource.append(line).append("\n");
                        }
                    }
                }
            }
            return resultSource.append("}").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSource(Class<?> sourceClass) {
        try (InputStream is = sourceClass.getResourceAsStream(sourceClass.getSimpleName() + ".java")) {
            if (is == null) {
                throw new RuntimeException("Can't fins source for class " + sourceClass.getName());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                source = reader.lines().toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass(); // Move to the superclass
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }
}
