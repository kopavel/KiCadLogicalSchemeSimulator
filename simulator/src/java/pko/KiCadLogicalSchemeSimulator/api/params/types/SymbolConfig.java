/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.params.types;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SymbolConfig {
    public final String clazz;
    public final Map<String, String> symbolParams = new HashMap<>();
    public final Collection<Integer> ignoredUnits = new ArrayList<>();
    public final Map<Integer, PinConfig> pinMap = new HashMap<>();
    public int unitAmount;

    public SymbolConfig(String clazz, String params) {
        this.clazz = clazz;
        if (params != null) {
            ParameterResolver.setParams(params, symbolParams);
        }
    }

    public void addUnit(String unit) {
        if (unit.startsWith("ignore;")) {
            ignoredUnits.add(unitAmount);
        }
        for (String pin : unit.split(";")) {
            if (!"ignore".equals(pin)) {
                String[] pinConf = pin.split("=");
                int pinNo = Integer.parseInt(pinConf[0]);
                pinMap.put(pinNo, new PinConfig(unitAmount, pinConf.length == 1 ? null : pinConf[1], this));
            }
        }
        unitAmount++;
    }
}

