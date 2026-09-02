/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led7Segment;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import java.awt.*;

public class Led7SegmentDisplay extends SchemaPart implements InteractiveSchemaPart {
    private final Led7SegmentDisplayUiComponent led7SegmentDisplayUiComponent;
    public int segmentsOn;
    public int segmentsOff;
    public boolean enabled;

    protected Led7SegmentDisplay(String id, String sParams) {
        super(id, sParams);
        addInPin(new Led7SegmentDisplayInPin("A", this, 0));
        addInPin(new Led7SegmentDisplayInPin("B", this, 4));
        addInPin(new Led7SegmentDisplayInPin("C", this, 6));
        addInPin(new Led7SegmentDisplayInPin("D", this, 2));
        addInPin(new Led7SegmentDisplayInPin("E", this, 5));
        addInPin(new Led7SegmentDisplayInPin("F", this, 3));
        addInPin(new Led7SegmentDisplayInPin("G", this, 1));
        if (params.containsKey("commonAnode") || params.containsKey("commonCathode")) {
            reverse = params.containsKey("commonAnode");
            addInPin(new Led7SegmentDisplayEnabledPin("CS", this));
            enabled=!reverse;
        } else {
            enabled = true;
        }
        addInPin(new Led7SegmentDisplayInPin("DP", this, 7));
        int size = Integer.parseInt(params.getOrDefault("size", "60"));
        Color on = UiTools.getColor(params.getOrDefault("onColor", "#ff0000"));
        Color off = UiTools.getColor(params.getOrDefault("offColor", "#bbbbbb"));
        led7SegmentDisplayUiComponent = new Led7SegmentDisplayUiComponent(this, size, on, off, id);
    }

    @Override
    public void initOuts() {
    }

    @Override
    public AbstractUiComponent getComponent() {
        return led7SegmentDisplayUiComponent;
    }
}
