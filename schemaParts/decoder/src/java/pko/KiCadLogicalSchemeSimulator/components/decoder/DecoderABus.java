/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class DecoderABus extends InBus {
    public final Decoder parent;
    public Bus outBus;
    public boolean csState;

    public DecoderABus(String id, Decoder parent, int size, String... aliases) {
        super(id, parent, size, aliases);
        this.parent = parent;
        outBus = parent.outBus;
    }

    /*Optimiser constructor*/
    public DecoderABus(DecoderABus oldBus, String variantId) {
        super(oldBus, variantId);
        outBus = oldBus.outBus;
        parent = oldBus.parent;
    }

    @Override
    public void setState(int newState) {
        state = newState;
        int outState;
        if (
            /*Optimiser line cs */
                csState && //
                        outBus.state != (
                                /*Optimiser line o block r*/
                                parent.params.containsKey("outReverse") ?//
                                (outState = ~((1 << newState)
                                        /*Optimiser line d*///
                                        % 10//
                                ))
                                        /*Optimiser line o blockEnd r block nr*///
                                                                        ://
                                (outState = (1 << newState)
                                        /*Optimiser line d*///
                                        % 10//
                                )
                                /*Optimiser blockEnd nr*///
                        )) {
            outBus.setState(outState);
        }
    }

    @Override
    public DecoderABus getOptimised(ModelItem<?> source) {
        ClassOptimiser<DecoderABus> optimiser = new ClassOptimiser<>(this).cut("o");
        optimiser.cut(parent.params.containsKey("outReverse") ? "nr" : "r");
        if (!parent.params.containsKey("decimal")) {
            optimiser.cut("d");
        }
        if (!parent.csPin.used) {
            optimiser.cut("cs");
        }
        DecoderABus build = optimiser.build();
        build.source = source;
        parent.replaceIn(this, build);
        parent.aBus = build;
        parent.csPin.aBus = build;
        return build;
    }
}
