/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led.indicator;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.TriStateInPin;
import pko.KiCadLogicalSchemeSimulator.components.led.LedUiComponent;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import java.awt.*;

public class LedIndicator extends SchemaPart implements InteractiveSchemaPart {
    private final LedUiComponent ledUiComponent;

    protected LedIndicator(String id, String sParams) {
        super(id, sParams);
        InPin inPin = addInPin(new TriStateInPin("IN", this) {
            @Override
            public void setHiImpedance() {
                hiImpedance=true;
                state = false;
            }

            @Override
            public void setHi() {
                hiImpedance=false;
                state = true;
            }

            @Override
            public void setLo() {
                hiImpedance=false;
                state = false;
            }
        });
        int size = Integer.parseInt(params.getOrDefault("size", "20"));
        Color on = UiTools.getColor(params.getOrDefault("onColor", "#ff0000"));
        Color off = UiTools.getColor(params.getOrDefault("offColor", "#808080"));
        ledUiComponent = new LedUiComponent(size, on, off, id,()-> reverse^inPin.state);
    }

    @Override
    public void initOuts() {
    }

    @Override
    public AbstractUiComponent getComponent() {
        return ledUiComponent;
    }
}
