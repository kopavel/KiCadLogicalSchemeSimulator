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

public class SingleOutShifterCIPin extends InPin {
    public final SingleOutShifter parent;
    public final boolean inhibitReverse;
    public final boolean clearReverse;
    public SingleOutShifterRPin rPin;

    public SingleOutShifterCIPin(String id, SingleOutShifter parent, boolean inhibitReverse, boolean clearReverse) {
        super(id, parent);
        this.parent = parent;
        this.inhibitReverse = inhibitReverse;
        this.clearReverse = clearReverse;
    }

    /*Optimiser constructor*/
    public SingleOutShifterCIPin(SingleOutShifterCIPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        inhibitReverse = oldPin.inhibitReverse;
        clearReverse = oldPin.clearReverse;
        rPin = oldPin.rPin;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o*/
        if (inhibitReverse) {
            /*Optimiser line r*/
            parent.clockEnabled = false;
            /*Optimiser line o*/
        } else {
            /*Optimiser block n*/
            parent.clockEnabled =
                    /*Optimiser line cr*///
                    !//
                            rPin.state
                            /*Optimiser line o*///
                            ^ clearReverse//
            ;
            /*Optimiser line o blockEnd n*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o*/
        if (inhibitReverse) {
            /*Optimiser block r*/
            parent.clockEnabled =
                    /*Optimiser line cr*///
                    !//
                            rPin.state
                            /*Optimiser line o*///
                            ^ clearReverse//
            ;
            /*Optimiser line o blockEnd r*/
        } else {
            /*Optimiser line n*/
            parent.clockEnabled = false;
            /*Optimiser line o*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<SingleOutShifterCIPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (clearReverse) {
            optimiser.cut("cr");
        }
        if (inhibitReverse) {
            optimiser.cut("n");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        SingleOutShifterCIPin build = optimiser.build();
        build.withState = source == null;
        parent.rPin.ciPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
