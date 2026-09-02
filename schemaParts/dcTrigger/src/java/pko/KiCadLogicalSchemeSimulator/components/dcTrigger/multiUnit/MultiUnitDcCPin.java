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

public class MultiUnitDcCPin extends InPin {
    public final MultiUnitDcTrigger parent;
    public final Pins[] pins;

    public MultiUnitDcCPin(String id, MultiUnitDcTrigger parent, InPin[] dPin, Pin[] qOut, Pin[] iqOut) {
        super(id, parent);
        this.parent = parent;
        pins = new Pins[dPin.length];
        for (int i = 0; i < dPin.length; i++) {
            pins[i] = new Pins(dPin[i], qOut[i], iqOut[i]);
        }
    }

    /*Optimiser constructor unroll pin:pins*/
    public MultiUnitDcCPin(MultiUnitDcCPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        pins = oldPin.pins;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser block nr*/
        if (
            /*Optimiser line o*/
                !parent.reverse && //
                        parent.clockEnabled) {
            Pins lPin;
            Pin dPin;
            for (Pins pin : pins) {
                if ((lPin = pin).dPin.state) {
                    if ((dPin = lPin.iqOut).state) {
                        dPin.setLo();
                        lPin.qOut.setHi();
                    }
                } else if ((dPin = lPin.qOut).state) {
                    dPin.setLo();
                    lPin.iqOut.setHi();
                }
            }
        }
        /*Optimiser blockEnd nr*/
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser block r*/
        if (
            /*Optimiser line o*/
                parent.reverse && //
                        parent.clockEnabled) {
            Pins lPin;
            Pin dPin;
            for (Pins pin : pins) {
                if ((lPin = pin).dPin.state) {
                    if ((dPin = lPin.iqOut).state) {
                        dPin.setLo();
                        lPin.qOut.setHi();
                    }
                } else if ((dPin = lPin.qOut).state) {
                    dPin.setLo();
                    lPin.iqOut.setHi();
                }
            }
        }
        /*Optimiser blockEnd r*/
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiUnitDcCPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        optimiser.unroll(pins.length);
        MultiUnitDcCPin build = optimiser.build();
        build.withState = source == null;
        parent.cPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
