/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class OscilloscopePin extends InPin {
    public final Pin wrapped;
    private final Oscilloscope oscilloscope;

    public OscilloscopePin(Pin wrapped, Oscilloscope oscilloscope) {
        super(wrapped, "oscilloscopeWrapper");
        this.wrapped = wrapped;
        this.oscilloscope = oscilloscope;
    }

    @Override
    public void setHi() {
        state = true;
        wrapped.setHi();
        oscilloscope.diagram.tick();
    }

    @Override
    public void setLo() {
        state = false;
        wrapped.setLo();
        oscilloscope.diagram.tick();
    }
}
