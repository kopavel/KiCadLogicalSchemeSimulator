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
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static pko.KiCadLogicalSchemeSimulator.tools.Utils.countLeadingSpaces;
import static pko.KiCadLogicalSchemeSimulator.tools.Utils.regexEscape;

//ToDo iskljuchat' bloki celikom
//Fixme - vikinut' vsje konstruktori i vjse polja (oni extendjaca)
public class ClassOptimiser<T> {
    private final Map<String, String> binds = new HashMap<>();
    private final T oldInstance;
    private String suffix = "";
    private List<String> source;
    private int unrollSize;

    public ClassOptimiser(T oldInstance) {
        this.oldInstance = oldInstance;
    }

    public static void main(String[] args) {
        SchemaPart schemaPart = new SchemaPart("Test", "") {
            @Override
            public void initOuts() {
            }
        };
        OutBus source = new OutBus("out", schemaPart, 5);
        source.addDestination(new InBus("in1", schemaPart, 5) {
            @Override
            public void setHiImpedance() {
            }

            @Override
            public void setState(long newState) {
            }
        }, 31, (byte) 0);
        source.addDestination(new InBus("in2", schemaPart, 5) {
            @Override
            public void setHiImpedance() {
            }

            @Override
            public void setState(long newState) {
            }
        }, 31, (byte) 0);
        IModelItem<?> opt = source.getOptimised();
    }

    public ClassOptimiser<T> unroll(int size) {
        unrollSize = size;
        suffix += "$unroll" + size;
        return this;
    }

    public ClassOptimiser<T> bind(String bindName, String replacement) {
        binds.put(bindName, replacement);
        suffix += "$" + bindName + "_" + replacement;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T build() {
        try {
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
                JavaCompiler.compileJavaSource(oldInstance.getClass(), optimizedClassName, optimisedSource);
                Log.trace(JavaCompiler.class, "Return instance");
                dynamicClass = Class.forName(oldInstance.getClass().getName() + suffix);
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

    private String process() {
        String iteratorPattern = null;
        String[] iteratorParams = null;
        boolean preserveBlock = false;
        int blockOffset = -1;
        int iteratorOffset = -1;
        Map<String, String> bindPatterns = new HashMap<>();
        StringBuilder resultSource = new StringBuilder();
        StringBuilder iteratorSource = new StringBuilder();
        StringBuilder blockSource = new StringBuilder();
        try {
            for (Iterator<String> lines = source.iterator(); lines.hasNext(); ) {
                String line = lines.next();
                int lineOffset = countLeadingSpaces(line);
                if (line.startsWith("import ") || line.startsWith("package ")) {
                    resultSource.append(line).append("\n");
                }
                if (line.contains("/*Optimiser ")) {
                    String[] params = line.substring(line.indexOf("/*Optimiser ") + 12, line.lastIndexOf("*/")).split(" ");
                    int i = 0;
                    String oldItemName = null;
                    while (i < params.length) {
                        String command = params[i++];
                        switch (command) {
                            case "constructor" -> {
                                preserveBlock = true;
                                blockOffset = countLeadingSpaces(line);
                                line = lines.next();//load constructor definition
                                int paramNamePos = line.indexOf(' ', line.indexOf('('));
                                oldItemName = line.substring(paramNamePos, line.indexOf(",", paramNamePos));
                                blockSource.append(line.replace(oldInstance.getClass().getSimpleName() + "(", oldInstance.getClass().getSimpleName() + suffix + "("))
                                           .append("\n");
                                String superLine = lines.next();//"super" are here
                                lineOffset = countLeadingSpaces(superLine);
                                blockSource.append(superLine).append("\n");
                            }
                            case "unroll" -> {
                                if (unrollSize == 0) {
                                    throw new RuntimeException("iterator size not provided");
                                }
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
                                line = lines.next();
                                for (Map.Entry<String, String> bind : bindPatterns.entrySet()) {
                                    line = line.replaceAll("(?<=\\W|^)" + bind.getKey() + "(?=\\W|$)", binds.get(bind.getValue()));
                                }
                                blockSource.append(line).append("\n");
                                preserveBlock = true;
                            }
                        }
                    }
                } else if (line.contains("public class " + oldInstance.getClass().getSimpleName())) {
                    //rename class definition
                    resultSource.append("public class ")
                                .append(oldInstance.getClass().getSimpleName())
                                .append(suffix)
                                .append(" extends ")
                                .append(oldInstance.getClass().getSimpleName())
                                .append(" {\n");
                } else if (!line.trim().isBlank() && blockOffset < 0) {
                    //block begin
                    blockOffset = lineOffset;
                    blockSource = new StringBuilder(line).append("\n");
                } else if (lineOffset <= blockOffset) {
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
                        //inside iterator block - accumulate whole block
                        iteratorSource.append(line).append("\n");
                    } else if (lineOffset == iteratorOffset && !blockSource.isEmpty()) {
                        //iterator block end - unroll it
                        for (int j = 0; j < unrollSize; j++) {
                            blockSource.append(iteratorSource.toString().replaceAll("\\b" + iteratorParams[0] + "\\b", iteratorParams[0] + j));
                        }
                        iteratorOffset = -1;
                    }
                } else {
                    blockSource.append(line).append("\n");
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
                throw new RuntimeException("Can't fins source for class" + sourceClass.getName());
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
