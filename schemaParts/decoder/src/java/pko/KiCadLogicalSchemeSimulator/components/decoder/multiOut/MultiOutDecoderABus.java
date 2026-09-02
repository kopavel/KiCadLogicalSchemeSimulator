/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

import static pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut.MultiOutDecoder.Part;

public class MultiOutDecoderABus extends InBus {
    public final MultiOutDecoder parent;
    public final Part[] parts;
    final boolean hasCs;

    public MultiOutDecoderABus(String id, MultiOutDecoder parent, int size, String... names) {
        super(id, parent, size, names);
        this.parent = parent;
        parts = parent.parts;
        hasCs = parent.hasCs;
    }

    /*Optimiser constructor unroll part:parts*/
    public MultiOutDecoderABus(MultiOutDecoderABus oldBus, String variantId) {
        super(oldBus, variantId);
        parent = oldBus.parent;
        parts = oldBus.parts;
        hasCs = oldBus.hasCs;
    }

    @Override
    public void setState(int newState) {
        Pin oldPin;
        Pin newPin;
        for (Part part : parts) {
            oldPin = part.outs[state];
            newPin = part.outs[newState];
            /*Optimiser block hasCs*/
            if (part.csState == 0
                    /*Optimiser line o*///
                    && hasCs//
            ) {
                /*Optimiser line o block r blockEnd hasCs*/
                if (parent.reverse) {
                    /*Optimiser line o block oc*/
                    if (parent.params.containsKey("openCollector")) {
                        if (!oldPin.hiImpedance) {
                            oldPin.setHiImpedance();
                        }
                        /*Optimiser line o blockEnd oc block noc*/
                    } else {
                        if (!oldPin.state) {
                            oldPin.setHi();
                        }
                        /*Optimiser line o blockEnd noc*/
                    }
                    if (
                        /*Optimiser line noc*///
                            newPin.state//
                                    /*Optimiser line o*///
                                    ||
                                    /*Optimiser line oc*/
                                    newPin.hiImpedance//
                    ) {
                        newPin.setLo();
                    }
                    /*Optimiser line o blockEnd r block nr*/
                } else {
                    /*Optimiser line o block oc*/
                    if (parent.params.containsKey("openCollector")) {
                        oldPin.setHiImpedance();
                        /*Optimiser line o blockEnd oc block noc*/
                    } else {
                        if (oldPin.state) {
                            oldPin.setLo();
                        }
                        /*Optimiser line o blockEnd noc*/
                    }
                    if (
                        /*Optimiser line noc*///
                            !newPin.state//
                                    /*Optimiser line o*///
                                    ||
                                    /*Optimiser line oc*/
                                    newPin.hiImpedance//
                    ) {
                        newPin.setHi();
                    }
                    /*Optimiser line o blockEnd nr*/
                }
                /*Optimiser line hasCs*/
            }
        }
        state = newState;
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiOutDecoderABus> optimiser = new ClassOptimiser<>(this).cut("o").bind("l", parent.partAmount).unroll(parts.length);
        optimiser.cut(parent.reverse ? "nr" : "r");
        optimiser.cut(parent.params.containsKey("openCollector") ? "noc" : "oc");
        if (!hasCs) {
            optimiser.cut("hasCs");
        }
        MultiOutDecoderABus build = optimiser.build();
        build.source = source;
        parent.aBus = build;
        parent.replaceIn(this, build);
        if (hasCs) {
            for (Part part : parent.parts) {
                for (MultiOutDecoderCsPin csPin : part.csPins) {
                    csPin.aBus = build;
                }
            }
        }
        return build;
    }
}
