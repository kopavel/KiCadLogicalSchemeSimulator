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
package pko.KiCadLogicalSchemeSimulator.api.params;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SymbolConfig;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Comp;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.param.Params;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.param.Part;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.param.Unit;

import java.util.*;
import java.util.stream.Collectors;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.all;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

public class ParameterResolver {
    public final Map<String, Map<Integer, SchemaPartConfig>> schemaParts = new HashMap<>();
    public final Map<String, SchemaPartConfig> schemaPartsById = new HashMap<>();
    public final Map<String, Map<String, SymbolConfig>> symbols = new HashMap<>();
    private final Map<String, Comp> compMap = new HashMap<>();
    public RecursionMode recursionMode = warn;

    public static void setParams(String params, Map<String, String> paramMap) {
        for (String param : params.split(";")) {
            int equalPos = param.indexOf("=");
            String paramName;
            String value;
            if (equalPos >= 0) {
                paramName = param.substring(0, equalPos);
                value = param.substring(equalPos + 1);
            } else {
                paramName = param;
                value = "true";
            }
            paramMap.put(paramName, value);
        }
    }

    public SymbolConfig addSymbol(String libId, String symbolId, String clazz, String params) {
        SymbolConfig symbolConfig = new SymbolConfig(clazz, params);
        symbols.computeIfAbsent(libId, e -> new HashMap<>()).put(symbolId, symbolConfig);
        return symbolConfig;
    }

    public void processNetFile(Export export, Params params, String[] recursiveOut) {
        Map<String, List<String>> cliRecursivePin = new HashMap<>();
        if (recursiveOut != null) {
            for (String pin : recursiveOut) {
                String pinName = pin.substring(pin.lastIndexOf("_") + 1);
                String partId = pin.substring(0, pin.lastIndexOf("_") - 1);
                cliRecursivePin.computeIfAbsent(partId, e -> new ArrayList<>()).add(pinName);
            }
        }
        if (params != null) {
            recursionMode = params.recursion;
        }
        compMap.putAll(export.getComponents().getComp()
                .stream()
                .collect(Collectors.toMap(i -> i.ref, i -> i)));
        for (Comp comp : export.getComponents().getComp()) {
            Map<String, SymbolConfig> libSymbols = symbols.get(comp.libsource.lib);
            Map<Integer, SchemaPartConfig> config = schemaParts.computeIfAbsent(comp.ref, e -> new HashMap<>());
            if (libSymbols == null) {
                throw new RuntimeException("Unmapped library " + comp.libsource.lib);
            } else {
                SymbolConfig symbolConfig = libSymbols.get(comp.libsource.part);
                if (symbolConfig == null) {
                    throw new RuntimeException("Unmapped symbol " + comp.libsource.lib + "." + comp.libsource.part);
                } else {
                    if ((symbolConfig.unitAmount - symbolConfig.ignoredUnits.size()) > 1) {
                        for (int i = 0; i < symbolConfig.unitAmount; i++) {
                            SchemaPartConfig newConfig = new SchemaPartConfig(symbolConfig, i);
                            config.put(i, newConfig);
                            schemaPartsById.put(comp.ref + "#" + ((char) ('A' + i)), newConfig);
                        }
                    } else {
                        SchemaPartConfig newConfig = new SchemaPartConfig(symbolConfig, 0);
                        config.put(0, newConfig);
                        schemaPartsById.put(comp.ref, newConfig);
                    }
                }
            }
            if (params != null && params.part != null) {
                for (Part part : params.part) {
                    if (part.id.equals(comp.ref)) {
                        for (SchemaPartConfig conf : config.values()) {
                            if (part.param != null) {
                                conf.setParams(part.param);
                            }
                            if (part.ignore != null) {
                                conf.ignore = part.ignore;
                            }
                            if (part.recursive != null) {
                                conf.recursivePins.addAll(Arrays.asList(part.recursive.split(";")));
                            }
                            if (cliRecursivePin.containsKey(comp.getRef())) {
                                conf.recursivePins.addAll(cliRecursivePin.get(comp.getRef()));
                            }
                        }
                        if (part.unit != null) {
                            for (Unit unit : part.unit) {
                                SchemaPartConfig conf = config.get(unit.name.getBytes()[0] - 'A');
                                if (conf == null) {
                                    throw new RuntimeException("Unmapped unit " + (unit.name.getBytes()[0] - 'A') + " in part " + part.id);
                                }
                                if (unit.params != null) {
                                    setParams(unit.params, conf.params);
                                }
                                if (unit.ignore != null) {
                                    conf.ignore = part.ignore;
                                }
                                if (unit.recursive != null) {
                                    conf.recursivePins.addAll(Arrays.asList(unit.recursive.split(";")));
                                }
                                if (cliRecursivePin.containsKey(comp.getRef() + "#" + unit.name)) {
                                    conf.recursivePins.addAll(cliRecursivePin.get(comp.getRef() + "#" + unit.name));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public SchemaPartConfig getSchemaPartConfig(Node node) {
        Comp comp = compMap.get(node.getRef());
        if (!symbols.containsKey(comp.libsource.lib) || !symbols.get(comp.libsource.lib).containsKey(comp.libsource.part)) {
            throw new RuntimeException(
                    "Unmapped SchemaPart id:" + comp.ref + "(lib:" + comp.getLibsource().getLib() + " part:" + comp.getLibsource().getPart() + ")");
        }
        PinConfig pinConfig = getPinConfig(node);
        if (pinConfig != null) {
            return schemaParts.get(node.getRef()).get(pinConfig.unitNo);
        } else {
            return schemaParts.get(node.getRef()).get(0);
        }
    }

    public PinConfig getPinConfig(Node node) {
        if (node.getPin() == null || node.getPin().isEmpty()) {
            return null;
        } else {
            return getPinMap(node).get(Integer.parseInt(node.getPin()));
        }
    }

    public Map<Integer, PinConfig> getPinMap(Node node) {
        Comp comp = compMap.get(node.getRef());
        return symbols.get(comp.libsource.lib).get(comp.libsource.getPart()).pinMap;
    }

    public String getId(Node node) {
        Comp comp = compMap.get(node.getRef());
        String symbolId = node.getRef();
        SymbolConfig symbolConfig = symbols.get(comp.libsource.lib).get(comp.libsource.getPart());
        PinConfig pinConfig = getPinConfig(node);
        if ((symbolConfig.unitAmount - symbolConfig.ignoredUnits.size() > 1) && pinConfig != null) {
            symbolId = symbolId + '#' + ((char) ('A' + pinConfig.unitNo));
        }
        return symbolId;
    }

    public RecursionMode getRecursionMode(String partId, String pinName) {
        return schemaPartsById.get(partId).recursivePins.contains(pinName) ? all : recursionMode;
    }
}
