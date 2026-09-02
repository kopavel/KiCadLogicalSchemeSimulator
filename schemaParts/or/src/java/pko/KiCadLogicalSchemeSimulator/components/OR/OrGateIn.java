/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.OR;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class OrGateIn extends InPin {
    public final OrGate orGate;
    public final int mask;
    public final int nMask;
    public Pin out;

    public OrGateIn(String id, OrGate parent, int mask) {
        super(id, parent);
        orGate = parent;
        this.mask = mask;
        nMask = ~mask;
        out = parent.getOutPin("OUT");
    }

    @SuppressWarnings("unused")
    /*Optimiser constructor*///
    public OrGateIn(OrGateIn oldPin, String variantId) {
        super(oldPin, variantId);
        orGate = oldPin.orGate;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        out = oldPin.out;
    }

    @Override
    public void setHi() {
        OrGate gate;
        /*Optimiser line setter*/
        state = true;
        int inState;
        if ((inState = (gate = orGate).inState) == 0) {
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
            /*Optimiser bind mask*/
            gate.inState = mask;
            return;
        } else {
            /*Optimiser bind mask*/
            gate.inState = inState | mask;
        }
    }

    @Override
    public void setLo() {
        OrGate gate;
        /*Optimiser line setter*/
        state = false;
        int inState;
        /*Optimiser bind mask*/
        if ((inState = (gate = orGate).inState) == mask) {
            /*Optimiser line o block r*/
            if (gate.reverse) {
                /*Optimiser line o block oc*/
                if (gate.params.containsKey("openCollector")) {
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
            gate.inState = 0;
            return;
        } else {
            /*Optimiser bind nMask*/
            gate.inState = inState & nMask;
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> inSource) {
        ClassOptimiser<OrGateIn> optimiser = new ClassOptimiser<>(this).bind("mask", mask).bind("nMask", nMask).cut("o");
        if (orGate.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (orGate.params.containsKey("openCollector")) {
            optimiser.cut("rc");
        } else {
            optimiser.cut("oc");
        }
        if (inSource != null) {
            optimiser.cut("setter");
        }
        OrGateIn build = optimiser.build();
        build.source = inSource;
        build.withState = inSource == null;
        orGate.replaceIn(this, build);
        return build;
    }
}
