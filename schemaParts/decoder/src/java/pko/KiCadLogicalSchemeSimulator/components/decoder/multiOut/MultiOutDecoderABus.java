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
package pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

import static pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut.MultiOutDecoder.Part;

public class MultiOutDecoderABus extends InBus {
    public final MultiOutDecoder parent;
    public Part[] parts;

    public MultiOutDecoderABus(String id, MultiOutDecoder parent, int size, String... names) {
        super(id, parent, size, names);
        this.parent = parent;
        parts = parent.parts;
    }

    /*Optimiser constructor unroll part:parts*/
    public MultiOutDecoderABus(MultiOutDecoderABus oldBus, String variantId) {
        super(oldBus, variantId);
        parent = oldBus.parent;
        parts = oldBus.parts;
    }

    @Override
    public void setState(int newState) {
        for (Part part : parts) {
            if (part.csState == 0) {
                /*Optimiser line o block r*/
                if (parent.reverse) {
                    /*Optimiser line o block oc*/
                    if (parent.params.containsKey("openCollector")) {
                        part.outs[newState].setHiImpedance();
                        /*Optimiser line o blockEnd oc block noc*/
                    } else {
                        part.outs[newState].setHi();
                        /*Optimiser line o blockEnd noc*/
                    }
                    part.outs[state].setLo();
                    /*Optimiser line o blockEnd r block nr*/
                } else {
                    /*Optimiser line o block oc*/
                    if (parent.params.containsKey("openCollector")) {
                        part.outs[state].setHiImpedance();
                        /*Optimiser line o blockEnd oc block noc*/
                    } else {
                        part.outs[state].setHi();
                        /*Optimiser line o blockEnd noc*/
                    }
                    part.outs[newState].setLo();
                    /*Optimiser line o blockEnd nr*/
                }
            }
        }
        state = newState;
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiOutDecoderABus> optimiser = new ClassOptimiser<>(this).cut("o").bind("l", parent.partAmount).unroll(parts.length);
        optimiser.cut(parent.reverse ? "nr" : "r");
        optimiser.cut(parent.params.containsKey("openCollector") ? "noc" : "oc");
        MultiOutDecoderABus build = optimiser.build();
        build.source = source;
        parent.aBus = build;
        parent.replaceIn(this, build);
        for (Part part : parent.parts) {
            for (MultiOutDecoderCsPin csPin : part.csPins) {
                csPin.aBus = build;
            }
        }
        return build;
    }
}
