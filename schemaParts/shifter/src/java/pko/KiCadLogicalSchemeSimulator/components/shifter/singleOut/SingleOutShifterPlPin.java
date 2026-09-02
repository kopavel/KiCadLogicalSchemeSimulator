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

public class SingleOutShifterPlPin extends InPin {
    public final SingleOutShifter parent;
    private boolean plReverse;

    public SingleOutShifterPlPin(String id, SingleOutShifter parent, boolean plReverse) {
        super(id, parent);
        this.parent = parent;
        this.plReverse = plReverse;
    }

    /*Optimiser constructor*/
    public SingleOutShifterPlPin(SingleOutShifterPlPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        parent.parallelLoad =
                /*Optimiser line o*/
                !
                        /*Optimiser bind r:plReverse*/
                        plReverse;
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        parent.parallelLoad =
                /*Optimiser bind nr:plReverse*/
                plReverse;
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<SingleOutShifterPlPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (source != null) {
            optimiser.cut("setter");
        }
        if (plReverse) {
            optimiser.bind("r", "false");
            optimiser.bind("nr", "true");
        } else {
            optimiser.bind("nr", "false");
            optimiser.bind("r", "true");
        }
        SingleOutShifterPlPin build = optimiser.build();
        build.withState = source == null;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
