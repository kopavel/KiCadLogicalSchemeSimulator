/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
    public int outState;

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
