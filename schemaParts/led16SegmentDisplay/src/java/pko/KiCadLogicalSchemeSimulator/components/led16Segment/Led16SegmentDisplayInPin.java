/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led16Segment;
import pko.KiCadLogicalSchemeSimulator.api.wire.TriStateInPin;

public class Led16SegmentDisplayInPin extends TriStateInPin {
    private final Led16SegmentDisplay display;
    private final int mask;
    private final int nMask;

    public Led16SegmentDisplayInPin(String id, Led16SegmentDisplay parent, int bit) {
        super(id, parent);
        display = parent;
        mask = 1 << bit;
        nMask = ~mask;
    }

    @Override
    public void setHiImpedance() {
        display.segmentsOff |= mask;
    }

    @Override
    public void setHi() {
        if (display.reverse) {
            display.segmentsOff |= mask;
        } else {
            display.segmentsOn |= mask;
            display.segmentsOff &= nMask;
        }
    }

    @Override
    public void setLo() {
        if (display.reverse) {
            display.segmentsOn |= mask;
            display.segmentsOff &= nMask;
        } else {
            display.segmentsOff |= mask;
        }
    }
}
