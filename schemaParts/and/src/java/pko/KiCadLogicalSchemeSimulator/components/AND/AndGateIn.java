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
package pko.KiCadLogicalSchemeSimulator.components.AND;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class AndGateIn extends InPin {
    public final AndGate parent;
    public final long mask;
    public final long nMask;
    public Pin out;

    public AndGateIn(String id, AndGate parent, long mask) {
        super(id, parent);
        this.parent = parent;
        this.mask = mask;
        this.nMask = ~mask;
        out = parent.getOutPin("OUT");
    }

    /*Optimiser constructor*/
    public AndGateIn(AndGateIn oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        out = oldPin.out;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        long state;
        AndGate parent;
        /*Optimiser bind mask*/
        if ((state = (parent = this.parent).inState) == mask) {
            parent.inState = 0;
            /*Optimiser line o block r*/
            if (parent.reverse) {
                out.setLo();
                /*Optimiser line o blockEnd r block nr*/
            } else {
                /*Optimiser line o block oc*/
                if (parent.params.containsKey("openCollector")){
                    out.setHiImpedance();
                    /*Optimiser line o blockEnd oc block rc*/
                } else {
                    out.setHi();
                    /*Optimiser line o blockEnd rc*/
                }
                /*Optimiser line o blockEnd nr*/
            }
            return;
        } else {
            /*Optimiser bind nMask*/
            parent.inState = state & nMask;
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        long state;
        AndGate parent;
        if ((state = (parent = this.parent).inState) == 0) {
            /*Optimiser bind mask*/
            parent.inState = mask;
            /*Optimiser line o block r*/
            if (parent.reverse) {
                /*Optimiser line o block oc*/
                if (parent.params.containsKey("openCollector")){
                    out.setHiImpedance();
                    /*Optimiser line o blockEnd oc block rc*/
                } else {
                    out.setHi();
                    /*Optimiser line o blockEnd rc*/
                }
                /*Optimiser line o blockEnd r block nr*/
            } else {
                out.setLo();
                /*Optimiser line o blockEnd nr*/
            }
            return;
        } else {
            /*Optimiser bind mask*/
            parent.inState = state | mask;
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<AndGateIn> optimiser = new ClassOptimiser<>(this).bind("mask", mask).bind("nMask", nMask).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (parent.params.containsKey("openCollector")) {
            optimiser.cut("rc");
        } else {
            optimiser.cut("oc");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        AndGateIn build = optimiser.build();
        build.source = source;
        parent.replaceIn(this, build);
        return build;
    }
}
