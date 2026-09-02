/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.repeater;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Repeater extends SchemaPart {
    public RepeaterInPin inPin;

    public Repeater(String id, String sParam) {
        super(id, sParam);
        if (params.containsKey("openCollector") || params.containsKey("openEmitter")) {
            addTriStateOutPin("OUT", false);
        } else {
            addOutPin("OUT", false);
        }
        inPin = addInPin(new RepeaterInPin("IN", this));
    }

    @Override
    public void initOuts() {
        Pin out = getOutPin("OUT");
        inPin.out = out;
        out.state = reverse != inPin.state;
    }

    @Override
    public String extraState() {
        return reverse ? "reverse" : null;
    }
}
