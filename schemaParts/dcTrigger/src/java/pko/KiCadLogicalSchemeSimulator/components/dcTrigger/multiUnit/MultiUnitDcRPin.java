/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger.multiUnit;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class MultiUnitDcRPin extends InPin {
    public final boolean reverse;
    public final MultiUnitDcTrigger parent;
    public final Pins[] pins;

    public MultiUnitDcRPin(String id, MultiUnitDcTrigger parent, boolean reverse, Pin[] qOut, Pin[] iqOut) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        state = reverse;
        pins = new Pins[qOut.length];
        for (int i = 0; i < qOut.length; i++) {
            pins[i] = new Pins(qOut[i], iqOut[i]);
        }
    }

    /*Optimiser constructor unroll pin:pins*/
    public MultiUnitDcRPin(MultiUnitDcRPin oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        parent = oldPin.parent;
        state = oldPin.state;
        pins = oldPin.pins;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        //noinspection PointlessBooleanExpression
        parent.clockEnabled =
                /*Optimiser line nr*///
                false
                        /*Optimiser line o*///
                        || reverse &&
                        /*Optimiser line r*/
                        true//
        ;
        /*Optimiser block nr line o*/
        if (!reverse) {
            for (Pins pin : pins) {
                if (pin.qOut.state) {
                    pin.iqOut.setHi();
                    pin.qOut.setLo();
                }
            }
            /*Optimiser blockEnd nr line o*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        //noinspection PointlessBooleanExpression
        parent.clockEnabled =
                /*Optimiser line nr*///
                true
                        /*Optimiser line o*///
                        && (!reverse ||
                        /*Optimiser line r*/
                        false //
                        /*Optimiser line o*///
                )//
        ;
        /*Optimiser block r line o*/
        if (reverse) {
            for (Pins pin : pins) {
                if (pin.qOut.state) {
                    pin.iqOut.setHi();
                    pin.qOut.setLo();
                }
            }
            /*Optimiser blockEnd r line o*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiUnitDcRPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        optimiser.unroll(pins.length);
        MultiUnitDcRPin build = optimiser.build();
        build.withState = source == null;
        parent.rPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
