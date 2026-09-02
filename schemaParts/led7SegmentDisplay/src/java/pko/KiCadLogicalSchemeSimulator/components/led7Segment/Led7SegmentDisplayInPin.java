/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led7Segment;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;

//ToDo optimiser
public class Led7SegmentDisplayInPin extends InPin {
    private final Led7SegmentDisplay display;
    private final int mask;

    public Led7SegmentDisplayInPin(String id, Led7SegmentDisplay parent, int bit) {
        super(id, parent);
        display = parent;
        mask = 1 << bit;
    }

    @Override
    public void setHi() {
        state = true;
        if (!display.reverse && display.enabled) {
            display.segmentsOn |= mask;
            display.segmentsOff &= ~mask;
        } else {
            display.segmentsOff |= mask;
        }
    }

    @Override
    public void setLo() {
        state = false;
        if (display.reverse && display.enabled) {
            display.segmentsOn |= mask;
            display.segmentsOff &= ~mask;
        } else {
            display.segmentsOff |= mask;
        }
    }
}
