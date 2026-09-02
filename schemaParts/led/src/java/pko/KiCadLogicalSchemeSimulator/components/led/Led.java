/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.components.diode.Diode;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import java.awt.*;

public class Led extends Diode implements InteractiveSchemaPart {
    private final LedUiComponent ledUiComponent;

    protected Led(String id, String sParams) {
        super(id, sParams);
        int size = Integer.parseInt(params.getOrDefault("size", "20"));
        Color on = UiTools.getColor(params.getOrDefault("onColor", "#ff0000"));
        Color off = UiTools.getColor(params.getOrDefault("offColor", "#808080"));
        ledUiComponent = new LedUiComponent(size, on, off, id, this::getState);
    }

    @Override
    public AbstractUiComponent getComponent() {
        return ledUiComponent;
    }

    private boolean getState() {
        boolean anodeState = !anode.otherImpedance && anode.otherState;
        boolean cathodeState = !cathode.otherImpedance && !cathode.otherState;
        return anodeState && cathodeState;
    }
}
