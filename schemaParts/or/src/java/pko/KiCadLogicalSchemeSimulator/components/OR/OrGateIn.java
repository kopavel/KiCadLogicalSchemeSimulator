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
package pko.KiCadLogicalSchemeSimulator.components.OR;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class OrGateIn extends InPin {
    public final OrGate orGate;
    public final int mask;
    public final int nMask;
    public Pin out;

    public OrGateIn(String id, OrGate parent, int mask) {
        super(id, parent);
        orGate = parent;
        this.mask = mask;
        nMask = ~mask;
        out = parent.getOutPin("OUT");
    }

    @SuppressWarnings("unused")
    /*Optimiser constructor*/
    public OrGateIn(OrGateIn oldPin, String variantId) {
        super(oldPin, variantId);
        orGate = oldPin.orGate;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        out = oldPin.out;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        int inState = orGate.inState;
        if (inState == 0) {
            /*Optimiser bind mask*/
            inState = mask;
            /*Optimiser line o block r*/
            if (orGate.reverse) {
                out.setLo();
                /*Optimiser line o blockEnd r block nr*/
            } else {
                /*Optimiser line o block oc*/
                if (orGate.params.containsKey("openCollector")) {
                    out.setHiImpedance();
                    /*Optimiser line o blockEnd oc block rc*/
                } else {
                    out.setHi();
                    /*Optimiser line o blockEnd rc*/
                }
                /*Optimiser line o blockEnd nr*/
            }
        } else {
            /*Optimiser bind mask*/
            inState |= mask;
        }
        orGate.inState = inState;
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        int inState = orGate.inState;
        /*Optimiser bind mask*/
        if (inState == mask) {
            inState = 0;
            /*Optimiser line o block r*/
            if (orGate.reverse) {
                /*Optimiser line o block oc*/
                if (orGate.params.containsKey("openCollector")) {
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
        } else {
            /*Optimiser bind nMask*/
            inState &= nMask;
        }
        orGate.inState = inState;
    }

    @Override
    public InPin getOptimised(ModelItem<?> inSource) {
        ClassOptimiser<OrGateIn> optimiser = new ClassOptimiser<>(this).bind("mask", mask).bind("nMask", nMask).cut("o");
        if (orGate.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (orGate.params.containsKey("openCollector")) {
            optimiser.cut("rc");
        } else {
            optimiser.cut("oc");
        }
        if (inSource != null) {
            optimiser.cut("setter");
        }
        OrGateIn build = optimiser.build();
        orGate.replaceIn(this, build);
        build.source = inSource;
        return build;
    }
}
