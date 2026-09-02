/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.counter;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

class CRInPin extends InPin {
    private final Counter counter;
    Bus out;

    CRInPin(Counter counter) {
        super("R", counter);
        this.counter = counter;
        out = counter.outBus;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        Counter lCounter;
        (lCounter = counter).enabled = false;
        out.setState(0);
        if (lCounter.reverse) {
            lCounter.nIn.oState = 0;
        } else {
            lCounter.in.oState = 0;
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        counter.enabled = true;
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<CRInPin> optimiser = new ClassOptimiser<>(this);
        if (source != null) {
            optimiser.cut("setter");
        }
        CRInPin build = optimiser.build();
        build.withState = source == null;
        counter.rPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
