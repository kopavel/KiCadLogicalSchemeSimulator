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
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger.multiUnit;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class MultiUnitDcRPin extends InPin {
    public final boolean reverse;
    public final MultiUnitDcTrigger parent;
    public Pins[] pins;

    public MultiUnitDcRPin(String id, MultiUnitDcTrigger parent, boolean reverse, Pin[] qOut, Pin[] iqOut) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        state = reverse;
        pins = new Pins[qOut.length];
        for (int i = 0; i < qOut.length; i++) {
            pins[i] = new Pins(qOut[i], iqOut[i]);
        }
    }

    /*Optimiser constructor unroll pin:pins*/
    public MultiUnitDcRPin(MultiUnitDcRPin oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        parent = oldPin.parent;
        state = oldPin.state;
        pins = oldPin.pins;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        //noinspection PointlessBooleanExpression
        parent.clockEnabled =
                /*Optimiser line nr*///
                false
                        /*Optimiser line o*///
                        || reverse &&
                        /*Optimiser line r*/
                        true//
        ;
        /*Optimiser block nr line o*/
        if (!reverse) {
            for (Pins pin : pins) {
                if (pin.qOut.state) {
                    pin.iqOut.setHi();
                    pin.qOut.setLo();
                }
            }
            /*Optimiser blockEnd nr line o*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        //noinspection PointlessBooleanExpression
        parent.clockEnabled =
                /*Optimiser line nr*///
                true
                        /*Optimiser line o*///
                        && (!reverse ||
                        /*Optimiser line r*/
                        false //
                        /*Optimiser line o*///
                )//
        ;
        /*Optimiser block r line o*/
        if (reverse) {
            for (Pins pin : pins) {
                if (pin.qOut.state) {
                    pin.iqOut.setHi();
                    pin.qOut.setLo();
                }
            }
            /*Optimiser blockEnd r line o*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiUnitDcRPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        optimiser.unroll(pins.length);
        MultiUnitDcRPin build = optimiser.build();
        parent.rPin = build;
        parent.inPins.put(id, build);
        return build;
    }
}
