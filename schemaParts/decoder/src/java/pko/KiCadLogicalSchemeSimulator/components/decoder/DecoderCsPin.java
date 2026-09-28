/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

@SuppressWarnings("ConditionalExpressionWithNegatedCondition")
public class DecoderCsPin extends InPin {
    public final Decoder parent;
    public Bus outBus;
    public DecoderABus aBus;
    protected int mask;

    public DecoderCsPin(String id, Decoder parent, int outSize) {
        super(id, parent);
        this.parent = parent;
        outBus = parent.outBus;
        aBus = parent.aBus;
        mask = Utils.getMaskForSize(outSize);
    }

    /*Optimiser constructor*/
    public DecoderCsPin(DecoderCsPin oldBus, String variantId) {
        super(oldBus, variantId);
        outBus = oldBus.outBus;
        parent = oldBus.parent;
        aBus = oldBus.aBus;
        mask = oldBus.mask;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            aBus.csState = false;
            if (!outBus.hiImpedance) {
                outBus.setHiImpedance();
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            aBus.csState = true;
            outBus.setState(
                    /*Optimiser line o block or*/
                    parent.params.containsKey("outReverse") ?//
                            /*Optimiser bind mask*/
                    mask ^ (1 << aBus.state)
                            /*Optimiser block d*///
                            % (//
                            /*Optimiser line o*///
                            !parent.params.containsKey("decimal") ? mask ://
                            10)
                            /*Optimiser blockEnd d line o blockEnd or block onr*///
                                                            ://
                    (1 << aBus.state)
                            /*Optimiser block d*///
                            % (//
                            /*Optimiser line o*///
                            !parent.params.containsKey("decimal") ? mask ://
                            10)
                    /*Optimiser blockEnd d blockEnd onr*///
                           );
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            aBus.csState = true;
            outBus.setState(
                    /*Optimiser line o block or*/
                    parent.params.containsKey("outReverse") ?//
                            /*Optimiser bind mask*/
                    mask ^ (1 << aBus.state)
                            /*Optimiser block d*///
                            % (//
                            /*Optimiser line o*///
                            !parent.params.containsKey("decimal") ? mask ://
                            10)
                            /*Optimiser blockEnd d line o blockEnd or block onr*///
                                                            ://
                    (1 << aBus.state)
                            /*Optimiser block d*///
                            % (//
                            /*Optimiser line o*///
                            !parent.params.containsKey("decimal") ? mask ://
                            10)
                    /*Optimiser blockEnd d blockEnd onr*///
                           );
            /*Optimiser line o blockEnd r block nr*/
        } else {
            aBus.csState = false;
            if (!outBus.hiImpedance) {
                outBus.setHiImpedance();
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public DecoderCsPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<DecoderCsPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!parent.params.containsKey("decimal")) {
            optimiser.cut("d");
        }
        optimiser.cut(parent.reverse ? "nr" : "r");
        optimiser.cut(parent.params.containsKey("outReverse") ? "onr" : "or");
        optimiser.bind("mask", mask);
        DecoderCsPin build = optimiser.build();
        build.withState = source == null;
        build.source = source;
        parent.replaceIn(this, build);
        parent.csPin = build;
        return build;
    }
}
