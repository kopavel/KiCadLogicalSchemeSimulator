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

public class MaskedMultiplexerOEPin extends InPin {
    public final MaskedMultiplexer parent;
    public final int mask;
    public final int nMask;
    public final Bus[] inBuses;
    public Bus outBus;

    public MaskedMultiplexerOEPin(String id, MaskedMultiplexer parent, int mask) {
        super(id, parent);
        this.parent = parent;
        this.mask = mask;
        nMask = ~mask;
        outBus = parent.getOutBus("Q");
        inBuses = parent.inBuses;
    }

    /*Optimiser constructor*/
    public MaskedMultiplexerOEPin(MaskedMultiplexerOEPin oldPin, String variantId) {
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
        int state;
        int lMask;
        MaskedMultiplexer parent;
        /*Optimiser block r line o*/
        if (this.parent.reverse) {
            /*Optimiser bind nm:nMask*/
            lMask = ((parent = this.parent).outMask &= nMask);
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser bind m:mask*/
            lMask = ((parent = this.parent).outMask |= mask);
            /*Optimiser line o blockEnd nr*/
        }
        if (outBus.state != (state = (inBuses[parent.nState].state & lMask))) {
            outBus.setState(state);
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = true;
        int state;
        int lMask;
        MaskedMultiplexer parent;
        /*Optimiser block r line o*/
        if (this.parent.reverse) {
            /*Optimiser bind m:mask*/
            lMask = ((parent = this.parent).outMask |= mask);
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser bind nm:nMask*/
            lMask = (parent = this.parent).outMask &= nMask;
            /*Optimiser line o blockEnd nr*/
        }
        if (outBus.state != (state = (inBuses[parent.nState].state & lMask))) {
            outBus.setState(state);
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MaskedMultiplexerOEPin> optimiser = new ClassOptimiser<>(this).bind("m", mask).bind("nm", nMask).cut("o");
        if (source != null) {
            optimiser.cut("setter");
        }
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        MaskedMultiplexerOEPin build = optimiser.build();
        build.withState = source == null;
        build.source = source;
        parent.replaceIn(this, build);
        if (parent.oePin == this) {
            parent.oePin = build;
        } else {
            parent.oePins.remove(this);
            parent.oePins.add(build);
        }
        return build;
    }
}
