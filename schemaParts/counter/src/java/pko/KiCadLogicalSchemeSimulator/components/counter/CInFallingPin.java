/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.counter;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.FallingEdgePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class CInFallingPin extends FallingEdgePin {
    public final int countMask;
    public final Counter parent;
    public Bus out;
    public int oState;

    public CInFallingPin(String id, Counter parent, int countMask) {
        super(id, parent);
        this.parent = parent;
        out = parent.getOutBus("Q");
        this.countMask = countMask;
    }

    /*Optimiser constructor*/
    public CInFallingPin(CInFallingPin oldPin, String variantId) {
        super(oldPin, variantId);
        countMask = oldPin.countMask;
        out = oldPin.out;
        parent = oldPin.parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line r*/
        if (parent.enabled) {
            int lState;
            /*Optimiser bind countMask*/
            out.setState(oState = ((lState = oState) == countMask ? 0 : lState + 1));
            /*Optimiser line r*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<CInFallingPin> optimiser = new ClassOptimiser<>(this).bind("countMask", countMask);
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!parent.rPin.used) {
            optimiser.cut("r");
        }
        CInFallingPin build = optimiser.build();
        parent.nIn = build;
        build.withState = source == null;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
