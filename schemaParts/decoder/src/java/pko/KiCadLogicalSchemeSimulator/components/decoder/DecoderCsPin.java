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
                            aBus.outState = ~(1L << aBus.state)
                            /*Optimiser line o*///
                    ) : (
                            /*Optimiser line onr*/
                            aBus.outState = 1L << aBus.state
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
                    aBus.outState = ~((1L << aBus.state)
                            /*Optimiser line d*///
                            % 10
                            /*Optimiser line o block onr blockEnd or*///
                    ) : (//
                            aBus.outState = ((1L << aBus.state)
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
        build.source = source;
        parent.replaceIn(this, build);
        parent.csPin = build;
        return build;
    }
}
