/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.AND;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class AndGateIn extends InPin {
    public final AndGate andGate;
    public final int mask;
    public final int nMask;
    public Pin out;

    public AndGateIn(String id, AndGate andGate, int mask) {
        super(id, andGate);
        this.andGate = andGate;
        this.mask = mask;
        nMask = ~mask;
        out = andGate.getOutPin("OUT");
    }

    /*Optimiser constructor*/
    public AndGateIn(AndGateIn oldPin, String variantId) {
        super(oldPin, variantId);
        andGate = oldPin.andGate;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        out = oldPin.out;
    }

    @Override
    public void setHi() {
        AndGate gate;
        /*Optimiser line setter*/
        state = true;
        int state;
        /*Optimiser bind mask*/
        if ((state = (gate = andGate).inState) == mask) {
            gate.inState = 0;
            /*Optimiser line o block r*/
            if (gate.reverse) {
                out.setLo();
                /*Optimiser line o blockEnd r block nr*/
            } else {
                /*Optimiser line o block oc*/
                if (gate.params.containsKey("openCollector")) {
                    out.setHiImpedance();
                    /*Optimiser line o blockEnd oc block rc*/
                } else {
                    out.setHi();
                    /*Optimiser line o blockEnd rc*/
                }
                /*Optimiser line o blockEnd nr*/
            }
        } else {
            /*Optimiser bind nMask*/
            gate.inState = state & nMask;
        }
    }

    @Override
    public void setLo() {
        AndGate lParent;
        /*Optimiser line setter*/
        state = false;
        int state;
        if ((state = (lParent = andGate).inState) == 0) {
            /*Optimiser bind mask*/
            lParent.inState = mask;
            /*Optimiser line o block r*/
            if (lParent.reverse) {
                /*Optimiser line o block oc*/
                if (lParent.params.containsKey("openCollector")) {
                    out.setHiImpedance();
                    /*Optimiser line o blockEnd oc block rc*/
                } else {
                    out.setHi();
                    /*Optimiser line o blockEnd rc*/
                }
                /*Optimiser line o blockEnd r block nr*/
            } else {
                out.setLo();
                /*Optimiser line o blockEnd nr*/
            }
        } else {
            /*Optimiser bind mask*/
            lParent.inState = state | mask;
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<AndGateIn> optimiser = new ClassOptimiser<>(this).bind("mask", mask).bind("nMask", nMask).cut("o");
        if (andGate.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (andGate.params.containsKey("openCollector")) {
            optimiser.cut("rc");
        } else {
            optimiser.cut("oc");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        AndGateIn build = optimiser.build();
        build.source = source;
        build.withState = source == null;
        andGate.replaceIn(this, build);
        return build;
    }
}
