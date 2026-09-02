/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.tester;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.TriStateInPin;

public class TestInPin extends SchemaPart {
    public final InPin pin;

    protected TestInPin(String id, String sParam) {
        super(id, sParam);
        TriStateInPin inPin = new TriStateInPin("In", this) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setHi() {
                hiImpedance = false;
                state = true;
            }

            @Override
            public void setLo() {
                hiImpedance = false;
                state = false;
            }
        };
        inPin.hiImpedance=true;
        pin = addInPin(inPin);
    }

    @Override
    public void initOuts() {
        if (pin.isTriState(pin.source)) {
            pin.hiImpedance = true;
        }
    }
}
