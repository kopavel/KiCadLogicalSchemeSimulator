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
package pko.KiCadLogicalSchemeSimulator.net;
import javassist.*;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static pko.KiCadLogicalSchemeSimulator.tools.Utils.countLeadingSpaces;
import static pko.KiCadLogicalSchemeSimulator.tools.Utils.regexEscape;

//ToDo Maski menjat' na konstanti
//ToDo iskljuchat' bloki celikom
public class ClassOptimiser {
    private final ClassPool pool = ClassPool.getDefault();
    private final StringBuilder init = new StringBuilder();
    private final Class<?> sourceJavaClass;
    private final CtClass sourceClass;
    private final Map<String, String> binds = new HashMap<>();
    CtClass optimizedClass = null;
    private String suffix = "";
    private List<String> source;
    private int unrollSize;

    public ClassOptimiser(Class<?> sourceJavaClass) {
        try {
            this.sourceJavaClass = sourceJavaClass;
            sourceClass = pool.get(sourceJavaClass.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        SchemaPart schemaPart = new SchemaPart("Test", "") {
            @Override
            public void initOuts() {
            }
        };
        OutPin source = new OutPin("out", schemaPart);
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
        ClassOptimiser optimizer = new ClassOptimiser(OutPin.class);
        optimizer.unroll(2);
        OutPin optimised = optimizer.build(source);
        System.out.println(optimised.getName());
    }

    public ClassOptimiser unroll(int size) {
        unrollSize = size;
        suffix += "$unroll" + size;
        return this;
    }

    public ClassOptimiser bind(String bindName, String replacement) {
        binds.put(bindName, replacement);
        suffix += "$" + bindName + ":" + replacement;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T, R extends T> R build(T originalInstance) {
        try {
            CtClass originalClass = pool.get(originalInstance.getClass().getName());
            String optimizedClassName = originalInstance.getClass().getName() + suffix;
            try {
                pool.get(optimizedClassName);
            } catch (NotFoundException ignored) {
                optimizedClass = pool.makeClass(optimizedClassName, originalClass);
                loadSource(sourceJavaClass);
                process();
            }
            Class<?> dynamicClass;
            if (optimizedClass != null) {
                // Create a new constructor
                if (!hasConstructor(sourceClass, sourceJavaClass, String.class)) {
                    throw new RuntimeException("Can't fins \"clone\" constructor for class" + sourceJavaClass.getName());
                }
                CtConstructor constructor = new CtConstructor(new CtClass[]{pool.get(originalInstance.getClass().getName())}, optimizedClass);
                String constructorBody = "{ super($1,\"" + suffix + "\"); " + init + "}";
                constructor.setBody(constructorBody);
                optimizedClass.addConstructor(constructor);
                // Use MethodHandles.Lookup to define the class in the current module
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                Method privateLookupInMethod = MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
                privateLookupInMethod.setAccessible(true);
                MethodHandles.Lookup privateLookup = (MethodHandles.Lookup) privateLookupInMethod.invoke(null, originalInstance.getClass(), lookup);
                Method defineClassMethod = MethodHandles.Lookup.class.getDeclaredMethod("defineClass", byte[].class);
                defineClassMethod.setAccessible(true);
                //noinspection PrimitiveArrayArgumentToVarargsMethod
                dynamicClass = (Class<?>) defineClassMethod.invoke(privateLookup, optimizedClass.toBytecode());
            } else {
                dynamicClass = Class.forName(optimizedClassName);
            }
            // Create an instance and invoke the overridden method
            return (R) dynamicClass.getDeclaredConstructor(originalInstance.getClass()).newInstance(originalInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasConstructor(CtClass sourceClass, Class<?>... parameterTypes) throws NotFoundException {
        CtConstructor[] constructors = sourceClass.getDeclaredConstructors();
        for (CtConstructor constructor : constructors) {
            CtClass[] ctParams = constructor.getParameterTypes();
            if (ctParams.length == parameterTypes.length) {
                if (IntStream.range(0, ctParams.length).allMatch(i -> ctParams[i].getName().equals(parameterTypes[i].getName()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void process() {
        String iteratorVariable = null;
        int methodOffset = -1;
        int iteratorOffset = -1;
        String bindPattern = "";
        String bindName = "";
        StringBuilder methodSource = new StringBuilder();
        StringBuilder iteratorSource = new StringBuilder();
        String methodName = "";
        try {
            for (String line : source) {
                int lineOffset = countLeadingSpaces(line);
                if (line.contains("/*Optimiser ")) {
                    String[] params = line.substring(line.indexOf("/*Optimiser ") + 12, line.lastIndexOf("*/")).split(" ");
                    switch (params[0]) {
                        case "iterator" -> {
                            if (params[1].contains("->")) {
                                if (unrollSize == 0) {
                                    throw new RuntimeException("iterator size not provided");
                                }
                                String[] split = params[1].split("->");
                                String iterator = split[0];
                                iteratorVariable = split[1];
                                CtField arrayField = sourceClass.getField(iterator);
                                CtClass arrayType = arrayField.getType();
                                CtClass variableType = arrayType.getComponentType();
                                init.append(iterator).append(" = $1.").append(iterator).append(";");
                                for (int j = 0; j < unrollSize; j++) {
                                    CtField publicField = new CtField(variableType, iteratorVariable + j, optimizedClass);
                                    publicField.setModifiers(Modifier.PUBLIC);
                                    optimizedClass.addField(publicField);
                                    init.append(iteratorVariable).append(j).append(" = ").append(iterator).append("[").append(j).append("];");
                                }
                            } else if (params[1].equals("unroll")) {
                                iteratorOffset = lineOffset;
                                iteratorSource = new StringBuilder();
                            }
                        }
                        case "override" -> {
                            methodOffset = lineOffset;
                            methodSource = new StringBuilder();
                        }
                        case "bind" -> {
                            bindPattern = regexEscape(params[1]);
                            bindName = bindPattern;
                            if (params.length == 3) {
                                bindName = params[2];
                            }
                        }
                    }
                } else if (methodOffset >= 0) {
                    if (lineOffset == methodOffset) {
                        if (!line.trim().startsWith("@") && !line.trim().startsWith("}")) {
                            methodName = line.substring(line.substring(0, line.indexOf('(')).lastIndexOf(' ') + 1, line.indexOf('('));
                        }
                    } else if (lineOffset > methodOffset) {
                        if (iteratorOffset >= 1) {
                            if (lineOffset > iteratorOffset) {
                                if (!line.trim().startsWith("assert ")) {
                                    iteratorSource.append(line).append("\n");
                                }
                            } else if (lineOffset == iteratorOffset && !iteratorSource.isEmpty()) {
                                if (unrollSize == 0) {
                                    throw new RuntimeException("iterator size not provided");
                                }
                                for (int j = 0; j < unrollSize; j++) {
                                    methodSource.append(iteratorSource.toString().replaceAll("\\b" + iteratorVariable + "\\b", iteratorVariable + j));
                                }
                                iteratorOffset = -1;
                            }
                        } else if (!line.trim().startsWith("assert ")) {
                            if (!bindPattern.isBlank()) {
                                line = line.replaceAll("(?<=\\W|^)" + bindPattern + "(?=\\W|$)", binds.get(bindName));
                                bindPattern = "";
                            }
                            //FixMe define all method params in annotation or read all params from source
                            line = line.replaceAll("\\bnewState\\b", "\\$1");
                            methodSource.append(line).append("\n");
                        }
                    } else {
                        methodOffset = -1;
                        overrideMethod(methodName, methodSource.toString());
                    }
                }
            }
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

    private void overrideMethod(String methodName, String methodSource) throws CannotCompileException, NotFoundException {
        // Override the method
        CtMethod originalMethod = sourceClass.getDeclaredMethod(methodName);
        CtMethod newMethod = new CtMethod(originalMethod, optimizedClass, null);
        newMethod.setBody("{\n" + methodSource + "\n}");
        // Add the new method to the subclass
        optimizedClass.addMethod(newMethod);
    }
}
