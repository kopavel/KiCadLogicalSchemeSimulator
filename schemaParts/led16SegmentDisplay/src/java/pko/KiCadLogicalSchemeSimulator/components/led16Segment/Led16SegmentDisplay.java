/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led16Segment;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import java.awt.*;

public class Led16SegmentDisplay extends SchemaPart implements InteractiveSchemaPart {
    private final Led16SegmentDisplayUiComponent led16SegmentDisplayUiComponent;
    public int segmentsOn;
    public int segmentsOff;
    protected Led16SegmentDisplay(String id, String sParams) {
        super(id, sParams);
        addInPin(new Led16SegmentDisplayInPin("A1", this, 0));
        addInPin(new Led16SegmentDisplayInPin("A2", this, 1));
        addInPin(new Led16SegmentDisplayInPin("B", this, 8));
        addInPin(new Led16SegmentDisplayInPin("C", this, 11));
        addInPin(new Led16SegmentDisplayInPin("D1", this, 2));
        addInPin(new Led16SegmentDisplayInPin("D2", this, 3));
        addInPin(new Led16SegmentDisplayInPin("E", this, 9));
        addInPin(new Led16SegmentDisplayInPin("F", this, 6));
        addInPin(new Led16SegmentDisplayInPin("G1", this, 4));
        addInPin(new Led16SegmentDisplayInPin("G2", this, 5));
        addInPin(new Led16SegmentDisplayInPin("H", this, 12));
        addInPin(new Led16SegmentDisplayInPin("I", this, 7));
        addInPin(new Led16SegmentDisplayInPin("J", this, 13));
        addInPin(new Led16SegmentDisplayInPin("K", this, 14));
        addInPin(new Led16SegmentDisplayInPin("L", this, 10));
        addInPin(new Led16SegmentDisplayInPin("M", this, 15));
        addInPin(new Led16SegmentDisplayInPin("DP", this, 16));
        int size = Integer.parseInt(params.getOrDefault("size", "60"));
        Color on = UiTools.getColor(params.getOrDefault("onColor", "#ff0000"));
        Color off = UiTools.getColor(params.getOrDefault("offColor", "#808080"));
        led16SegmentDisplayUiComponent = new Led16SegmentDisplayUiComponent(this,size, on, off, id);
    }

    @Override
    public void initOuts() {
    }

    @Override
    public AbstractUiComponent getComponent() {
        return led16SegmentDisplayUiComponent;
    }
}
