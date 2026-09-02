/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.AND;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.util.HashMap;
import java.util.Map;

public class AndGate extends SchemaPart {
    private final Map<String, AndGateIn> ins = new HashMap<>();
    public int inState;
    private Pin out;

    public AndGate(String id, String sParam) {
        super(id, sParam);
        if (params.containsKey("openCollector")){
            addTriStateOutPin("OUT", false);
        } else {
            addOutPin("OUT", false);
        }
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int pinAmount = Integer.parseInt(params.get("size"));
        for (int i = 0; i < pinAmount; i++) {
            int mask = 1 << i;
            ins.put("IN" + i, addInPin(new AndGateIn("IN" + i, this, mask)));
        }
    }

    @Override
    public void initOuts() {
        out = getOutPin("OUT");
        ins.values().forEach(pin -> {
            if (pin.isHiImpedance() || !pin.state) {
                inState |= (1 << Integer.parseInt(pin.getId().substring(2)));
            }
            pin.out = out;
        });
        out.state = inState == 0 ? nReverse : reverse;
    }

    @Override
    public <T> void replaceIn(ModelItem<T> oldIn, ModelItem<T> newIn) {
        super.replaceIn(oldIn, newIn);
        ins.put(oldIn.getId(), (AndGateIn) newIn);
    }

    @Override
    public String extraState() {
        return reverse ? "reverse" : null;
    }
}
