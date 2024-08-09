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
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.net.bus.BusToWiresAdapter;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static pko.KiCadLogicalSchemeSimulator.tools.Utils.countLeadingSpaces;
import static pko.KiCadLogicalSchemeSimulator.tools.Utils.regexEscape;

//ToDo iskljuchat' bloki celikom
//Fixme - vikinut' vsje konstruktori i vjse polja (oni extendjaca)
public class JavaCompilerClassOptimiser<T> {
    private final Map<String, String> binds = new HashMap<>();
    private final T oldInstance;
    private String suffix = "";
    private List<String> source;
    private int unrollSize;

    public JavaCompilerClassOptimiser(T oldInstance) {
        this.oldInstance = oldInstance;
    }

    public static void main(String[] args) {
        SchemaPart schemaPart = new SchemaPart("Test", "") {
            @Override
            public void initOuts() {
            }
        };
        BusToWiresAdapter source = new BusToWiresAdapter(new OutBus("out", schemaPart, 5), 31);
        source.addDestination(new InPin("in1", schemaPart) {
            @Override
            public void setHiImpedance() {
            }

            @Override
            public void setState(boolean newState) {
            }
        });
        source.addDestination(new InPin("in2", schemaPart) {
            @Override
            public void setHiImpedance() {
            }

            @Override
            public void setState(boolean newState) {
            }
        });
        BusToWiresAdapter opt = source.getOptimised();
    }

    public JavaCompilerClassOptimiser<T> unroll(int size) {
        unrollSize = size;
        suffix += "$unroll" + size;
        return this;
    }

    public JavaCompilerClassOptimiser<T> bind(String bindName, String replacement) {
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
                if (!hasConstructor(oldInstance.getClass(), oldInstance.getClass(), String.class)) {
                    throw new RuntimeException("Can't find \"clone\" constructor for class" + oldInstance.getClass().getName());
                }
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
            return (T) dynamicClass.getDeclaredConstructor(oldInstance.getClass(), String.class).newInstance(oldInstance, suffix);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends Class<?>> boolean hasConstructor(T clazz, Class<?>... parameterTypes) {
        @SuppressWarnings("unchecked")
        Constructor<T>[] constructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
        for (Constructor<T> constructor : constructors) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length == parameterTypes.length) {
                if (IntStream.range(0, params.length).allMatch(i -> params[i].getName().equals(parameterTypes[i].getName()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String process() {
        String iteratorPattern = null;
        String constructorPattern = "public " + oldInstance.getClass().getSimpleName() + "(";
        String cloneConstructorPattern = constructorPattern + oldInstance.getClass().getSimpleName() + " ";
        String[] iteratorParams = null;
        String iteratorType = null;
        String oldPinParamName = null;
        int iteratorOffset = -1;
        int constructorOffset = -1;
        Map<String, String> bindPatterns = new HashMap<>();
        StringBuilder processedSource = new StringBuilder();
        StringBuilder blockSource = new StringBuilder();
        try {
            for (String line : source) {
                int lineOffset = countLeadingSpaces(line);
                if (line.contains("/*Optimiser ")) {
                    String[] params = line.substring(line.indexOf("/*Optimiser ") + 12, line.lastIndexOf("*/")).split(" ");
                    int i = 0;
                    while (i < params.length) {
                        String command = params[i++];
                        switch (command) {
                            case "unroll" -> {
                                iteratorParams = params[i++].split(":");
                                iteratorType = getField(oldInstance.getClass(), iteratorParams[1]).getType().getComponentType().getSimpleName();
                                iteratorPattern = "for (" + iteratorType + " " + iteratorParams[0] + " : " + iteratorParams[1] + ") {";
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
                } else {
                    if (line.contains("public class " + oldInstance.getClass().getSimpleName())) {
                        //rename class definition
                        processedSource.append("public class ")
                                       .append(oldInstance.getClass().getSimpleName())
                                       .append(suffix)
                                       .append(" extends ")
                                       .append(oldInstance.getClass().getSimpleName())
                                       .append(" {\n");
                        if (unrollSize > 0) {
                            for (int i = 0; i < unrollSize; i++) {
                                //noinspection DataFlowIssue
                                processedSource.append(iteratorType).append(" ").append(iteratorParams[0]).append(i).append(";\n");
                            }
                        }
                    } else if (unrollSize > 0 && line.contains(cloneConstructorPattern)) {
                        //find "clone" constructor in unroll case - rename it
                        int paramNamePos = line.indexOf(constructorPattern) + cloneConstructorPattern.length();
                        oldPinParamName = line.substring(paramNamePos, line.indexOf(",", paramNamePos));
                        constructorOffset = lineOffset;
                        processedSource.append(line.replace(oldInstance.getClass().getSimpleName() + "(", oldInstance.getClass().getSimpleName() + suffix + "("))
                                       .append("\n");
                    } else if (constructorOffset > -1) {
                        if (lineOffset > constructorOffset) {
                            //inside "clone" constructor block
                            processedSource.append(line).append("\n");
                        } else if (lineOffset == constructorOffset) {
                            //append clone constructor
                            if (unrollSize == 0) {
                                throw new RuntimeException("iterator size not provided");
                            }
                            //noinspection DataFlowIssue
                            processedSource.append(iteratorParams[1]).append(" = ").append(oldPinParamName).append(".").append(iteratorParams[1]).append(";\n");
                            for (int j = 0; j < unrollSize; j++) {
                                processedSource.append(iteratorParams[0]).append(j).append(" = ").append(iteratorParams[1]).append("[").append(j).append("];\n");
                            }
                            constructorOffset = -1;
                            processedSource.append("}\n");
                        }
                    } else if (line.contains(constructorPattern)) {
                        //find others constructor - rename it
                        processedSource.append(line.replace(oldInstance.getClass().getSimpleName() + "(", oldInstance.getClass().getSimpleName() + suffix + "("))
                                       .append("\n");
                    } else if (!bindPatterns.isEmpty()) {
                        //next line after bind command
                        for (Map.Entry<String, String> bind : bindPatterns.entrySet()) {
                            line = line.replaceAll("(?<=\\W|^)" + bind.getKey() + "(?=\\W|$)", binds.get(bind.getValue()));
                        }
                        processedSource.append(line).append("\n");
                        bindPatterns = new HashMap<>();
                    } else if (iteratorPattern != null && line.contains(iteratorPattern)) {
                        //find iterator body - skip iterator definition
                        iteratorOffset = lineOffset;
                    } else if (iteratorOffset > -1) {
                        if (lineOffset > iteratorOffset) {
                            //inside iterator block - accumulate whole block
                            blockSource.append(line).append("\n");
                        } else if (lineOffset == iteratorOffset && !blockSource.isEmpty()) {
                            //iterator block end - unroll it
                            if (unrollSize == 0) {
                                throw new RuntimeException("iterator size not provided");
                            }
                            for (int j = 0; j < unrollSize; j++) {
                                processedSource.append(blockSource.toString().replaceAll("\\b" + iteratorParams[0] + "\\b", iteratorParams[0] + j));
                            }
                            iteratorOffset = -1;
                            blockSource = new StringBuilder();
                        }
                    } else {
                        processedSource.append(line).append("\n");
                    }
                }
            }
            return processedSource.toString();
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
