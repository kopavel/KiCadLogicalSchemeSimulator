/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut.MultiOutDecoder.Part;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiOutDecoderCsPin extends InPin {
    public final MultiOutDecoder parent;
    public final boolean csRevert;
    public final Part part;
    public final int mask;
    public final int nMask;
    public final Pin[] outs;
    public MultiOutDecoderABus aBus;
    private int csNo;

    //FixMe nonReverse modes
    public MultiOutDecoderCsPin(String id, Part part, int csNo, MultiOutDecoder parent) {
        super(id, parent);
        this.csNo = csNo;
        this.parent = parent;
        this.part = part;
        csRevert = part.CSs[csNo];
        mask = 1 << csNo;
        int fullMask = Utils.getMaskForSize(part.CSs.length);
        nMask = ~mask & fullMask;
        aBus = parent.aBus;
        outs = part.outs;
    }

    /*Optimiser constructor*/
    public MultiOutDecoderCsPin(MultiOutDecoderCsPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        csRevert = oldPin.csRevert;
        part = oldPin.part;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        outs = oldPin.outs;
        aBus = oldPin.aBus;
    }

    @Override
    public void setHi() {
        state = true;
        /*Optimiser line o block cr*/
        if (csRevert) {
            /*Optimiser bind m:mask*/
            part.csState |= mask;
            /*Optimiser line o block r*/
            if (parent.reverse) {
                /*Optimiser line o block oc*/
                if (parent.params.containsKey("openCollector")) {
                    outs[aBus.state].setHiImpedance();
                    /*Optimiser line o blockEnd oc block noc*/
                } else {
                    outs[aBus.state].setHi();
                    /*Optimiser line o blockEnd noc*/
                }
                /*Optimiser line o blockEnd r block nr*/
            } else {
                outs[aBus.state].setLo();
                /*Optimiser line o blockEnd nr*/
            }
            /*Optimiser line o blockEnd cr block cnr*/
        } else {
            /*Optimiser bind m:mask*/
            if (part.csState == mask) {
                part.csState = 0;
                /*Optimiser line o block r*/
                if (parent.reverse) {
                    outs[aBus.state].setLo();
                    /*Optimiser line o blockEnd r block nr*/
                } else {
                    /*Optimiser line o block oc*/
                    if (parent.params.containsKey("openCollector")) {
                        outs[aBus.state].setHiImpedance();
                        /*Optimiser line o blockEnd oc block noc*/
                    } else {
                        outs[aBus.state].setHi();
                        /*Optimiser line o blockEnd noc*/
                    }
                    /*Optimiser line o blockEnd nr*/
                }
            } else {
                /*Optimiser bind nm:nMask*/
                part.csState &= nMask;
            }
            /*Optimiser line o blockEnd cnr*/
        }
    }

    @Override
    public void setLo() {
        state = false;
        /*Optimiser line o block cr*/
        if (csRevert) {
            /*Optimiser bind m:mask*/
            if (part.csState == mask) {
                part.csState = 0;
                /*Optimiser line o block r*/
                if (parent.reverse) {
                    outs[aBus.state].setLo();
                    /*Optimiser line o blockEnd r block nr*/
                } else {
                    /*Optimiser line o block oc*/
                    if (parent.params.containsKey("openCollector")) {
                        outs[aBus.state].setHiImpedance();
                        /*Optimiser line o blockEnd oc block noc*/
                    } else {
                        outs[aBus.state].setHi();
                        /*Optimiser line o blockEnd noc*/
                    }
                    /*Optimiser line o blockEnd nr*/
                }
            } else {
                /*Optimiser bind nm:nMask*/
                part.csState &= nMask;
            }
            /*Optimiser line o blockEnd cr block cnr*/
        } else {
            /*Optimiser bind m:mask*/
            part.csState |= mask;
            /*Optimiser line o block r*/
            if (parent.reverse) {
                /*Optimiser line o block oc*/
                if (parent.params.containsKey("openCollector")) {
                    outs[aBus.state].setHiImpedance();
                    /*Optimiser line o blockEnd oc block noc*/
                } else {
                    outs[aBus.state].setHi();
                    /*Optimiser line o blockEnd noc*/
                }
                /*Optimiser line o blockEnd r block nr*/
            } else {
                outs[aBus.state].setLo();
                /*Optimiser line o blockEnd nr*/
            }
            /*Optimiser line o blockEnd cnr*/
        }
    }

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiOutDecoderCsPin> optimiser = new ClassOptimiser<>(this).cut("o").bind("m", mask).bind("nm", nMask);
        optimiser.cut(csRevert ? "cnr" : "cr");
        optimiser.cut(parent.reverse ? "nr" : "r");
        optimiser.cut(parent.params.containsKey("openCollector") ? "noc" : "oc");
        MultiOutDecoderCsPin build = optimiser.build();
        build.source = source;
        part.csPins[csNo] = build;
        parent.replaceIn(this, build);
        return build;
    }
}

