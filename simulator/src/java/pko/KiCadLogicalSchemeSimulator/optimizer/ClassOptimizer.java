package pko.KiCadLogicalSchemeSimulator.optimizer;
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
import java.util.List;

import static pko.KiCadLogicalSchemeSimulator.tools.Utils.countLeadingSpaces;

public class ClassOptimizer<T> {
    private final ClassPool pool = ClassPool.getDefault();
    private final CtClass originalClass;
    private final T originalInstance;
    private final StringBuilder init = new StringBuilder();
    private final String suffix;
    String optimizedClassName;
    private CtClass optimizedClass;
    private List<String> source;
    private boolean useOld;

    public ClassOptimizer(Class<?> sourceClass, T originalInstance, String suffix) {
        this.suffix = suffix;
        try {
            this.originalInstance = originalInstance;
            originalClass = pool.get(originalInstance.getClass().getName());
            optimizedClassName = originalInstance.getClass().getName() + "$" + suffix;
            try {
                optimizedClass = pool.get(optimizedClassName);
                useOld = true;
            } catch (NotFoundException ignored) {
                optimizedClass = pool.makeClass(optimizedClassName, originalClass);
                loadSource(sourceClass);
            }
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
        ClassOptimizer<OutPin> optimizer = new ClassOptimizer<>(OutPin.class, source, "unwrap2");
        optimizer.unwrapIterators(2);
        OutPin optimised = optimizer.getInstance();
        System.out.println(optimised.getName());
    }

    @SuppressWarnings("unchecked")
    public <R extends T> R getInstance() {
        try {
            Class<?> dynamicClass;
            if (!useOld) {
                // Create a new constructor
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

    public ClassOptimizer<T> unwrapIterators(int size) {
        if (!useOld) {
            String iterator = null;
            String variable = null;
            int methodOffset = -1;
            int iteratorOffset = -1;
            StringBuilder methodSource = new StringBuilder();
            StringBuilder iteratorSource = new StringBuilder();
            String methodName = "";
            try {
                for (String line : source) {
                    int lineOffset = countLeadingSpaces(line);
                    if (line.contains("/*Optimiser ")) {
                        String[] params = line.substring(line.indexOf("/*Optimiser ") + 12, line.lastIndexOf("*/")).split(" ");
                        if (params[0].equals("iterator")) {
                            if (params[1].contains("->")) {
                                String[] split = params[1].split("->");
                                iterator = split[0];
                                variable = split[1];
                            } else if (params[1].equals("unwrap")) {
                                iteratorOffset = lineOffset;
                                iteratorSource = new StringBuilder();
                            }
                        }
                        if (params[0].equals("override")) {
                            methodOffset = lineOffset;
                            methodSource = new StringBuilder();
                        }
                    } else {
                        if (iterator != null && line.contains("[] " + iterator + " ")) {
                            CtField arrayField = originalClass.getField(iterator);
                            CtClass arrayType = arrayField.getType();
                            CtClass variableType = arrayType.getComponentType();
                            for (int j = 0; j < size; j++) {
                                CtField publicField = new CtField(variableType, variable + j, optimizedClass);
                                publicField.setModifiers(Modifier.PUBLIC);
                                optimizedClass.addField(publicField);
                                init.append(variable).append(j).append(" = ").append(iterator).append("[").append(j).append("];");
                            }
                        }
                        if (methodOffset >= 0) {
                            if (lineOffset == methodOffset) {
                                if (!line.trim().startsWith("@") && !line.trim().startsWith("}")) {
                                    methodName = line.substring(line.substring(0, line.indexOf('(')).lastIndexOf(' ') + 1, line.indexOf('('));
                                }
                            } else if (lineOffset > methodOffset) {
                                if (iteratorOffset >= 1) {
                                    if (lineOffset > iteratorOffset) {
                                        iteratorSource.append(line).append("\n");
                                    } else if (lineOffset == iteratorOffset && !iteratorSource.isEmpty()) {
                                        for (int j = 0; j < size; j++) {
                                            methodSource.append(iteratorSource.toString().replaceAll("\\b" + variable + "\\b", variable + j));
                                        }
                                        iteratorOffset = -1;
                                    }
                                } else {
                                    methodSource.append(line).append("\n");
                                }
                            } else {
                                methodOffset = -1;
                                overrideMethod(methodName, methodSource.toString());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this;
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
        CtMethod originalMethod = getMethod(originalClass, methodName);
        CtMethod newMethod = new CtMethod(originalMethod, optimizedClass, null);
        newMethod.setBody("{\n" + methodSource + "\n}");
        // Add the new method to the subclass
        optimizedClass.addMethod(newMethod);
    }

    private CtMethod getMethod(CtClass ctClass, String methodName) throws NotFoundException {
        while (ctClass != null) {
            try {
                return ctClass.getDeclaredMethod(methodName);
            } catch (NotFoundException e) {
                ctClass = ctClass.getSuperclass();
            }
        }
        throw new NotFoundException("Method " + methodName + " not found in class hierarchy.");
    }
}
