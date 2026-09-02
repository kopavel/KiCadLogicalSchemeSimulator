/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.multiplexer.masked;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class MaskedMultiplexerNPin extends InPin {
    public final MaskedMultiplexer parent;
    public final int mask;
    public final int nMask;
    public final Bus[] inBuses;
    public Bus outBus;

    public MaskedMultiplexerNPin(String id, MaskedMultiplexer parent, int mask) {
        super(id, parent);
        this.parent = parent;
        this.mask = mask;
        nMask = ~mask;
        outBus = parent.getOutBus("Q");
        inBuses = parent.inBuses;
    }

    /*Optimiser constructor*/
    public MaskedMultiplexerNPin(MaskedMultiplexerNPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        outBus = oldPin.outBus;
        inBuses = oldPin.inBuses;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser bind m:mask*/
        int nState = (parent.nState |= mask);
        int state;
        if (outBus.state != (state = (inBuses[nState].state
                /*Optimiser line oe*///
                & parent.outMask//
        ))) {
            outBus.setState(state);
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser bind nm:nMask*/
        int nState = (parent.nState &= nMask);
        int state;
        if (outBus.state != (state = (inBuses[nState].state
                /*Optimiser line oe*///
                & parent.outMask//
        ))) {
            outBus.setState(state);
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MaskedMultiplexerNPin> optimiser = new ClassOptimiser<>(this).bind("m", mask).bind("nm", nMask);
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!parent.oePin.used && parent.oePins.stream()
                .noneMatch(e -> e.used)) {
            optimiser.cut("oe");
        }
        MaskedMultiplexerNPin build = optimiser.build();
        build.withState = source == null;
        build.source = source;
        parent.replaceIn(this, build);
        parent.nPins.remove(this);
        parent.nPins.add(build);
        return build;
    }
}
