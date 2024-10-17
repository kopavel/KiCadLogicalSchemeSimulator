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

public class MultiUnitDcCPin extends InPin {
    public final MultiUnitDcTrigger parent;
    public final InPin[] dPin;
    public Pin[] qOut;
    public Pin[] iqOut;

    public MultiUnitDcCPin(String id, MultiUnitDcTrigger parent, InPin[] dPin, Pin[] qOut, Pin[] iqOut) {
        super(id, parent);
        this.parent = parent;
        this.dPin = dPin;
        this.qOut = qOut;
        this.iqOut = iqOut;
    }

    /*Optimiser constructor*/
    public MultiUnitDcCPin(MultiUnitDcCPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        dPin = oldPin.dPin;
        qOut = oldPin.qOut;
        iqOut = oldPin.iqOut;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser block nr*/
        if (
            /*Optimiser line o*/
                !parent.reverse && //
                        parent.clockEnabled) {
            for (int i = 0; i < dPin.length; i++) {
                if (dPin[i].state) {
                    if (iqOut[i].state) {
                        iqOut[i].setLo();
                        qOut[i].setHi();
                    }
                } else {
                    if (qOut[i].state) {
                        qOut[i].setLo();
                        iqOut[i].setHi();
                    }
                }
            }
        }
        /*Optimiser blockEnd nr*/
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser block r*/
        if (
            /*Optimiser line o*/
                parent.reverse && //
                        parent.clockEnabled) {
            for (int i = 0; i < dPin.length; i++) {
                if (dPin[i].state) {
                    if (iqOut[i].state) {
                        iqOut[i].setLo();
                        qOut[i].setHi();
                    }
                } else {
                    if (qOut[i].state) {
                        qOut[i].setLo();
                        iqOut[i].setHi();
                    }
                }
            }
        }
        /*Optimiser blockEnd r*/
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiUnitDcCPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        MultiUnitDcCPin build = optimiser.build();
        parent.cPin = build;
        parent.inPins.put(id, build);
        return build;
    }
}
