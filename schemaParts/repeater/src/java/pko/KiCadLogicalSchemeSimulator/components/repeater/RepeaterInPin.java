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
package pko.KiCadLogicalSchemeSimulator.components.repeater;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class RepeaterInPin extends InPin {
    private final Repeater parent;
    public Pin out;

    /*Optimiser constructor*/
    public RepeaterInPin(RepeaterInPin oldPin, String variantId) {
        super(oldPin, variantId);
        out = oldPin.out;
        parent = oldPin.parent;
    }

    public RepeaterInPin(String id, Repeater parent) {
        super(id, parent);
        out = parent.getOutPin("OUT");
        this.parent = parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            /*Optimiser line o block oe*/
            if (parent.params.containsKey("openEmitter")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oe block re*/
            } else {
                out.setLo();
                /*Optimiser line o blockEnd re*/
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser line o block oc*/
            if (parent.params.containsKey("openCollector")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oc block rc*/
            } else {
                out.setHi();
                /*Optimiser line o blockEnd rc*/
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            /*Optimiser line o block oc*/
            if (parent.params.containsKey("openCollector")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oc block rc*/
            } else {
                out.setHi();
                /*Optimiser line o blockEnd rc*/
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser line o block oe*/
            if (parent.params.containsKey("openEmitter")) {
                out.setHiImpedance();
                /*Optimiser line o blockEnd oe block re*/
            } else {
                out.setLo();
                /*Optimiser line o blockEnd re*/
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<RepeaterInPin> optimiser = new ClassOptimiser<>(this).cut("o");
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
        if (parent.params.containsKey("openEmitter")) {
            optimiser.cut("re");
        } else {
            optimiser.cut("oe");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        RepeaterInPin build = optimiser.build();
        parent.inPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
