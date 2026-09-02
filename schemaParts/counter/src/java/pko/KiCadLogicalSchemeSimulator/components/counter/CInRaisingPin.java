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
import pko.KiCadLogicalSchemeSimulator.api.wire.RaisingEdgePin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class CInRaisingPin extends RaisingEdgePin {
    public final int countMask;
    public final Counter parent;
    public Bus out;
    public int oState;

    public CInRaisingPin(String id, Counter parent, int countMask) {
        super(id, parent);
        out = parent.getOutBus("Q");
        this.countMask = countMask;
        this.parent = parent;
    }

    /*Optimiser constructor*/
    public CInRaisingPin(CInRaisingPin oldPin, String variantId) {
        super(oldPin, variantId);
        countMask = oldPin.countMask;
        out = oldPin.out;
        parent = oldPin.parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line r*/
        if (parent.enabled) {
            int lState;
            /*Optimiser bind countMask*/
            out.setState(oState = ((lState = oState) == countMask ? 0 : lState + 1));
            /*Optimiser line r*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<CInRaisingPin> optimiser = new ClassOptimiser<>(this).bind("countMask", countMask);
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!parent.rPin.used) {
            optimiser.cut("r");
        }
        CInRaisingPin build = optimiser.build();
        parent.in = build;
        parent.replaceIn(this, build);
        build.withState = source == null;
        build.source = source;
        return build;
    }
}
