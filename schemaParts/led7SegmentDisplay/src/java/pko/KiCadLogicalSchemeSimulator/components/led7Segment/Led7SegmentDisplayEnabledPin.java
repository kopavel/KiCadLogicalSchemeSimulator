/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led7Segment;
import pko.KiCadLogicalSchemeSimulator.api.wire.TriStateInPin;

//ToDo optimiser
public class Led7SegmentDisplayEnabledPin extends TriStateInPin {
    private final Led7SegmentDisplay display;

    public Led7SegmentDisplayEnabledPin(String id, Led7SegmentDisplay parent) {
        super(id, parent);
        display = parent;
    }

    @Override
    public void setHiImpedance() {
        display.enabled = false;
    }

    @Override
    public void setHi() {
        display.enabled = display.reverse;
    }

    @Override
    public void setLo() {
        display.enabled = !display.reverse;
    }
}
