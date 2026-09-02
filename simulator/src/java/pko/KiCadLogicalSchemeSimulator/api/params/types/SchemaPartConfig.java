/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.params.types;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaPartConfig {
    public final Map<String, String> params;
    public Map<String, Boolean> priority;
    public String clazz;
    public boolean ignore;
    public final Collection<String> recursivePins = new HashSet<>();

    public SchemaPartConfig(SymbolConfig symbolConfig, int unitNo) {
        clazz = symbolConfig.clazz;
        params = new HashMap<>(symbolConfig.symbolParams);
        ignore = symbolConfig.ignoredUnits.contains(unitNo);
    }

    public void setParams(String param) {
        ParameterResolver.setParams(param, params);
    }

    public String getParamString() {
        return params.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
    }
}
