/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.power;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class Power extends SchemaPart {
    protected Power(String id, String sParams) {
        super(id, sParams);
        boolean powerState = params.containsKey("hi");
        if (params.containsKey("strong")) {
            addOutPin("OUT", powerState);
        } else {
            addPullPin("OUT", powerState);
        }
    }

    @Override
    public void initOuts() {
    }
}
