/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.repeater;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class RepeaterInPin extends InPin {
    private final Repeater parent;
    public Pin out;

    /*Optimiser constructor*/
    public RepeaterInPin(RepeaterInPin oldPin, String variantId) {
        super(oldPin, variantId);
        out = oldPin.out;
        parent = oldPin.parent;
    }

    public RepeaterInPin(String id, Repeater parent) {
        super(id, parent);
        out = parent.getOutPin("OUT");
        this.parent = parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            /*Optimiser line o block oe*/
            if (parent.params.containsKey("openEmitter")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oe block re*/
            } else {
                out.setLo();
                /*Optimiser line o blockEnd re*/
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser line o block oc*/
            if (parent.params.containsKey("openCollector")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oc block rc*/
            } else {
                out.setHi();
                /*Optimiser line o blockEnd rc*/
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            /*Optimiser line o block oc*/
            if (parent.params.containsKey("openCollector")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oc block rc*/
            } else {
                out.setHi();
                /*Optimiser line o blockEnd rc*/
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser line o block oe*/
            if (parent.params.containsKey("openEmitter")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oe block re*/
            } else {
                out.setLo();
                /*Optimiser line o blockEnd re*/
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<RepeaterInPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (parent.params.containsKey("openCollector")) {
            optimiser.cut("rc");
        } else {
            optimiser.cut("oc");
        }
        if (parent.params.containsKey("openEmitter")) {
            optimiser.cut("re");
        } else {
            optimiser.cut("oe");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        RepeaterInPin build = optimiser.build();
        build.withState = source == null;
        parent.inPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
