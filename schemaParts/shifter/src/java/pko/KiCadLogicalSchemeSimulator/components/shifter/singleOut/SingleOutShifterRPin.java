/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.shifter.singleOut;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class SingleOutShifterRPin extends InPin {
    public final SingleOutShifter parent;
    public final boolean inhibitReverse;
    public final boolean clearReverse;
    public SingleOutShifterCIPin ciPin;

    public SingleOutShifterRPin(String id, SingleOutShifter parent, boolean inhibitReverse, boolean clearReverse) {
        super(id, parent);
        this.parent = parent;
        this.inhibitReverse = inhibitReverse;
        this.clearReverse = clearReverse;
    }

    /*Optimiser constructor*/
    public SingleOutShifterRPin(SingleOutShifterRPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        inhibitReverse = oldPin.inhibitReverse;
        clearReverse = oldPin.clearReverse;
        ciPin = oldPin.ciPin;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o*/
        if (clearReverse) {
            /*Optimiser line r*/
            parent.clockEnabled =
                    /*Optimiser line ir*/
                    !//
                            ciPin.state//
                            /*Optimiser line o*///
                            ^ !inhibitReverse//
            ;
            /*Optimiser line o*/
        } else {
            /*Optimiser block n*/
            parent.clockEnabled = false;
            parent.latch = 0;
            /*Optimiser line o blockEnd n*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o*/
        if (clearReverse) {
            /*Optimiser block r*/
            parent.clockEnabled = false;
            parent.latch = 0;
            /*Optimiser line o blockEnd r*/
        } else {
            /*Optimiser block n*/
            parent.clockEnabled =
                    /*Optimiser line ir*/
                    !//
                            ciPin.state//
                            /*Optimiser line o*///
                            ^ !inhibitReverse//
            ;
            /*Optimiser line o blockEnd n*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<SingleOutShifterRPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (!inhibitReverse) {
            optimiser.cut("ir");
        }
        if (clearReverse) {
            optimiser.cut("n");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        SingleOutShifterRPin build = optimiser.build();
        build.withState = source == null;
        parent.ciPin.rPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
