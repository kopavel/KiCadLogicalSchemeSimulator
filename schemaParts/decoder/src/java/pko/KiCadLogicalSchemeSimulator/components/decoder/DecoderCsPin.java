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

public class DecoderCsPin extends InPin {
    public final Decoder parent;
    public Bus outBus;
    public DecoderABus aBus;

    public DecoderCsPin(String id, Decoder parent) {
        super(id, parent);
        this.parent = parent;
        outBus = parent.outBus;
        aBus = parent.aBus;
    }

    /*Optimiser constructor*/
    public DecoderCsPin(DecoderCsPin oldBus, String variantId) {
        super(oldBus, variantId);
        outBus = oldBus.outBus;
        parent = oldBus.parent;
        aBus = oldBus.aBus;
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
                    /*Optimiser line o*/
                    parent.params.containsKey("outReverse") ? (
                            /*Optimiser line or*/
                            ~(1 << aBus.state)
                            /*Optimiser line o*///
                    ) : (
                            /*Optimiser line onr*/
                            1 << aBus.state
                            /*Optimiser line o*///
                    )//
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
                    /*Optimiser line o*/
                    parent.params.containsKey("outReverse") ?
                            /*Optimiser block or*/
                    ~((1 << aBus.state)
                      /*Optimiser line d*///
                      % 10
                            /*Optimiser line o block onr blockEnd or*///
                    ) : (//
                            ((1 << aBus.state)
                             /*Optimiser line d*///
                             % 10
                                    /*Optimiser line o blockEnd onr*///
                            )//
                    ));
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
        DecoderCsPin build = optimiser.build();
        build.withState = source == null;
        build.source = source;
        parent.replaceIn(this, build);
        parent.csPin = build;
        return build;
    }
}
