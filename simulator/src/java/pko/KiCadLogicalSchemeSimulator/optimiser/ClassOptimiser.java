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
    private static final Map<String, Class<?>> dynamicClasses = new HashMap<>();
    final List<String> cutList = new ArrayList<>();
    private final Map<String, String> binds = new HashMap<>();
    private final T oldInstance;
    private final Class<?> sourceClass;
    private final Map<String, UnrollDescriptor> unrolls = new HashMap<>();
    private String suffix = "";
    private List<String> source;
    private boolean noAssert = true;

    public ClassOptimiser(T oldInstance) {
        this(oldInstance, oldInstance.getClass());
    }

    public ClassOptimiser(T oldInstance, Class<?> sourceClass) {
        this.oldInstance = oldInstance;
        this.sourceClass = sourceClass;
        //noinspection AssertWithSideEffects,ConstantValue
        assert !(noAssert = false);
    }

    public ClassOptimiser<T> unroll(int size) {
        unrolls.put("d", new UnrollDescriptor(size));
        suffix += "_ud" + size;
        return this;
    }

    public ClassOptimiser<T> unroll(String id, int size) {
        if (size > 0) {
            unrolls.put(id, new UnrollDescriptor(size));
            suffix += "_u" + id + size;
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ClassOptimiser<T> cut(String cutName) {
        cutList.add(cutName);
        suffix += "_c_" + cutName;
        return this;
    }

    public ClassOptimiser<T> bind(String bindName, int replacement) {
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
            if (suffix.isBlank() || Simulator.noOptimiser) {
                return oldInstance;
            }
            if (!noAssert) {
                suffix += "_ae";
            }
            String optimizedClassName = sourceClass.getSimpleName() + suffix;
            String optimizedFullClassName =
                    sourceClass.getPackageName().replace(Simulator.class.getPackageName(), Simulator.class.getPackageName() + ".optimised") + "." +
                            sourceClass.getSimpleName() + suffix;
            Class<?> dynamicClass = dynamicClasses.get(optimizedFullClassName);
            if (dynamicClass == null) {
                try {
                    dynamicClass = Class.forName(optimizedFullClassName);
                } catch (ClassNotFoundException ignored) {
                    loadSource();
                    Log.trace(JavaCompiler.class, "Process");
                    String optimisedSource = process();
                    Log.trace(JavaCompiler.class, "Compile");
                    storeSrc(optimizedFullClassName, optimisedSource);
                    dynamicClass = JavaCompiler.compileJavaSource(optimizedFullClassName, optimizedClassName, optimisedSource);
                    if (dynamicClass == null) {
                        Log.error(JavaCompiler.class, "Optimised class compile was not successful, fall back to generic class, file name:" + optimizedFullClassName);
                        return oldInstance;
                    } else {
                        Log.trace(JavaCompiler.class, "Return instance");
                        dynamicClasses.put(optimizedFullClassName, dynamicClass);
                    }
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

    private static void storeSrc(String className, String source) {
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

    //ToDo `if` support
    private String process() {
        Set<String> blocks = new HashSet<>();
        Set<String> cutLines = new HashSet<>();
        Map<String, String> bindPatterns = new HashMap<>();
        Map<String, String> iteratorPattern = new HashMap<>();
        String iteratorId = "d";
        boolean preserveFunction = false;
        boolean skipFunction = false;
        boolean inAsserts = false;
        boolean inComment = false;
        int functionOffset = -1;
        int iteratorOffset = -1;
        StringBuilder resultSource = new StringBuilder();
        StringBuilder iteratorSource = new StringBuilder();
        StringBuilder functionSource = new StringBuilder();
        StringBuilder addFunctionSource = new StringBuilder();
        try {
            for (ListIterator<String> lines = source.listIterator(); lines.hasNext(); ) {
                String line = lines.next();
                int lineOffset = countLeadingSpaces(line);
                //override getOptimised
                if (line.contains("getOptimised(ModelItem<?> source)")) {
                    skipFunction = true;
                    resultSource.append(line).append("\n").append("return this;\n}");
                } else if (line.startsWith("import ")) {
                    //Copy imports
                    resultSource.append(line).append("\n");
                } else if (line.startsWith("package ")) {
                    //skip asserts
                    line = line.replace(Simulator.class.getPackageName(), Simulator.class.getPackageName() + ".optimised");
                    resultSource.append(line).append("\n");
                    resultSource.append("import ").append(sourceClass.getPackageName()).append(".*;\n");
                } else if (noAssert && line.trim().startsWith("assert")) {
                    //skip asserts
                    if (!line.contains(";")) {
                        inAsserts = true; //multiline assert
                    }
                } else if (inAsserts) {
                    if (line.contains(";")) {
                        inAsserts = false; //multiline assert end
                    }
                    //Skip single row comments
                } else if (!line.trim().startsWith("//")) {
                    //process optimiser instructions
                    if (line.contains("/*Optimiser ")) {
                        int i = 0;
                        String oldItemName = null;
                        String[] params = line.substring(line.indexOf("/*Optimiser ") + 12, line.lastIndexOf("*/")).split(" ");
                        while (i < params.length) {
                            String command = params[i++];
                            switch (command) {
                                case "line" -> {
                                    String lineName = params[i++];
                                    cutLines.add(lineName);
                                }
                                case "block" -> {
                                    String blockName = params[i++];
                                    blocks.add(blockName);
                                }
                                case "blockEnd" -> {
                                    String blockName = params[i++];
                                    blocks.remove(blockName);
                                }
                                case "constructor" -> {
                                    preserveFunction = true;
                                    functionOffset = countLeadingSpaces(line);
                                    line = lines.next();//load constructor definition
                                    int paramNamePos = line.indexOf(' ', line.indexOf('('));
                                    oldItemName = line.substring(paramNamePos, line.indexOf(",", paramNamePos));
                                    functionSource =
                                            new StringBuilder(line.replace(sourceClass.getSimpleName() + "(", sourceClass.getSimpleName() + suffix + "(")).append(
                                                    "\n");
                                    String superLine = lines.next();//"super" here
                                    lineOffset = countLeadingSpaces(superLine);
                                    functionSource.append(superLine).append("\n");
                                    while (!(lineOffset <= functionOffset && !line.isBlank())) { //skip all constructor definition, all in `super`
                                        line = lines.next();
                                        lineOffset = countLeadingSpaces(line);
                                    }
                                    line = lines.previous();
                                }
                                case "unroll" -> {
                                    String[] iteratorParams = params[i++].split(":");
                                    if (iteratorParams.length == 0) {
                                        throw new RuntimeException("No iterator params specified in class " + sourceClass);
                                    }
                                    String id = (iteratorParams.length == 3) ? iteratorParams[2] : "d";
                                    String iteratorItemType = getField(sourceClass, iteratorParams[1]).getType().getComponentType().getSimpleName();
                                    iteratorPattern.put(id, "for (" + iteratorItemType + " " + iteratorParams[0] + " : " + iteratorParams[1] + ") {");
                                    String lineTab = " ".repeat(lineOffset);
                                    String blockTab = " ".repeat(functionOffset);
                                    if (unrolls.containsKey(id)) {
                                        unrolls.get(id).variable = iteratorParams[0];
                                        functionSource.append(lineTab)
                                                      .append(iteratorParams[1])
                                                      .append("=")
                                                      .append(oldItemName)
                                                      .append(".")
                                                      .append(iteratorParams[1])
                                                      .append(";\n");
                                        if (addFunctionSource.isEmpty()) {
                                            addFunctionSource.append(lineTab)
                                                             .append("splitDestinations();\n")
                                                             .append(blockTab)
                                                             .append("}\n\n")
                                                             .append(blockTab)
                                                             .append("public void splitDestinations () {\n");
                                        }
                                        for (int j = 0; j < unrolls.get(id).size; j++) {
                                            //add destination variables initialisation
                                            addFunctionSource.append(lineTab)
                                                             .append(iteratorParams[0])
                                                             .append(j)
                                                             .append(" = ")
                                                             .append(iteratorParams[1])
                                                             .append("[")
                                                             .append(j)
                                                             .append("];\n");
                                            //add destination variable definitions
                                            resultSource.append(blockTab)
                                                        .append("private ")
                                                        .append(iteratorItemType)
                                                        .append(" ")
                                                        .append(iteratorParams[0])
                                                        .append(j)
                                                        .append(";\n");
                                        }
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
                        //skip block comments
                    } else if (line.trim().startsWith("/*") || inComment) {
                        inComment = !line.contains("*/");
                        // class definition
                    } else if (line.contains("public class " + sourceClass.getSimpleName())) {
                        //rename class definition
                        resultSource.append("public class ")
                                    .append(sourceClass.getSimpleName())
                                    .append(suffix)
                                    .append(" extends ")
                                    .append(sourceClass.getSimpleName())
                                    .append(" {\n");
                        functionOffset = -1;
                        functionSource = new StringBuilder();
                        preserveFunction = false;
                    } else if (!line.trim().isBlank() && functionOffset < 0 && lineOffset > 0) {
                        //block begin
                        functionOffset = lineOffset;
                        functionSource = new StringBuilder(line).append("\n");
                        preserveFunction = false;
                    } else if (lineOffset <= functionOffset && !line.isBlank()) {
                        //function end
                        if (line.trim().equals("}")) {
                            if (!addFunctionSource.isEmpty()) {
                                functionSource.append(addFunctionSource);
                                addFunctionSource = new StringBuilder();
                            }
                            functionSource.append(line).append("\n");
                            if (preserveFunction && !skipFunction) {
                                resultSource.append("\n").append(functionSource);
                            }
                            functionOffset = -1;
                            functionSource = new StringBuilder();
                        } else {
                            //non function block — ignore and initialize new one.
                            functionOffset = lineOffset;
                            functionSource = new StringBuilder(line).append("\n");
                        }
                        preserveFunction = false;
                        skipFunction = false;
                    } else {
                        String finalLine = line;
                        //cut lines
                        if (cutLines.stream()
                                .anyMatch(cutList::contains)) {
                            preserveFunction = true;
                            //cut blocks
                        } else if (blocks.stream()
                                .anyMatch(cutList::contains)) {
                            preserveFunction = true;
                        } else if (iteratorPattern.entrySet()
                                .stream()
                                .anyMatch(e -> finalLine.contains(e.getValue()))) {
                            //find iterator body — skip iterator definition
                            iteratorId = iteratorPattern.entrySet()
                                    .stream()
                                    .filter(e -> finalLine.contains(e.getValue()))
                                    .map(Map.Entry::getKey).findFirst().orElseThrow();
                            iteratorOffset = lineOffset;
                            preserveFunction = true;
                            iteratorSource = new StringBuilder();
                        } else if (lineOffset == iteratorOffset && !functionSource.isEmpty()) {
                            //iterator block end — unroll it
                            UnrollDescriptor descriptor = unrolls.get(iteratorId);
                            for (int j = 0; j < descriptor.size; j++) {
                                functionSource.append(iteratorSource.toString()
                                                                    .replaceAll("(?<=\\W|^)" + descriptor.variable + "(?=\\W|$)", descriptor.variable + j)
                                                                    .replaceAll("unrollIndexPower", String.valueOf((int) Math.pow(2, j)))
                                                                    .replaceAll("unrollIndex", String.valueOf(j)));
                            }
                            iteratorOffset = -1;
                        } else {
                            //binds
                            if (!bindPatterns.isEmpty()) {
                                String oldLine = line;
                                for (Map.Entry<String, String> bind : bindPatterns.entrySet()) {
                                    if (binds.containsKey(bind.getValue())) {
                                        line = line.replaceAll("(?<=\\W|^)" + bind.getKey() + "(?=\\W|$)", binds.get(bind.getValue()));
                                    }
                                }
                                if (!line.equals(oldLine)) {
                                    preserveFunction = true;
                                }
                            }
                            if (iteratorOffset > -1) {
                                if (lineOffset > iteratorOffset) {
                                    //inside iterator block — accumulate whole block
                                    iteratorSource.append(line).append("\n");
                                }
                            } else {
                                functionSource.append(line).append("\n");
                            }
                        }
                        bindPatterns.clear();
                        cutLines.clear();
                    }
                }
            }
            return resultSource.append("}").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSource() {
        Log.trace(JavaCompiler.class, "Load source for {}", sourceClass.getSimpleName());
        try (InputStream is = sourceClass.getResourceAsStream(sourceClass.getSimpleName() + ".java")) {
            if (is == null) {
                throw new RuntimeException("Can't find source for class " + sourceClass.getName());
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

    private static class UnrollDescriptor {
        public int size;
        public String variable;

        public UnrollDescriptor(int size) {
            this.size = size;
        }
    }
}
