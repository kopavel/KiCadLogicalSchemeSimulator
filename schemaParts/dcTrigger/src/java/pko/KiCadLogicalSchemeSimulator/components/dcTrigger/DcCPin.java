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
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class DcCPin extends InPin {
    public final DcTrigger parent;
    public final InPin dPin;
    public Pin qOut;
    public Pin iqOut;

    public DcCPin(String id, DcTrigger parent) {
        super(id, parent);
        this.parent = parent;
        dPin = parent.dPin;
        qOut = parent.qOut;
        iqOut = parent.iqOut;
    }

    /*Optimiser constructor*/
    public DcCPin(DcCPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        dPin = oldPin.dPin;
        qOut = oldPin.qOut;
        iqOut = oldPin.iqOut;
    }

    @Override
    public void setHi() {
        state = true;
        /*Optimiser block nr block anyRS*/
        if (
            /*Optimiser line o*/
                !parent.reverse && //
                        parent.clockEnabled) {
            /*Optimiser blockEnd anyRS*/
            if (dPin.state) {
                if (iqOut.state) {
                    iqOut.setLo();
                    /*Optimiser block bothRS block anyRS*/
                }
                if (!qOut.state) {
                    /*Optimiser blockEnd bothRS  blockEnd anyRS*/
                    qOut.setHi();
                }
            } else {
                if (qOut.state) {
                    qOut.setLo();
                    /*Optimiser block bothRS  block anyRS*/
                }
                if (!iqOut.state) {
                    /*Optimiser blockEnd bothRS  blockEnd anyRS*/
                    iqOut.setHi();
                }
            }
            /*Optimiser line anyRS*/
        }
        /*Optimiser blockEnd nr*/
    }

    @Override
    public void setLo() {
        state = false;
        /*Optimiser block r*/
        if (
            /*Optimiser line o*/
                parent.reverse &&//
                        /*Optimiser line anyRS*///
                        parent.clockEnabled//
        ) {
            if (dPin.state) {
                if (iqOut.state) {
                    iqOut.setLo();
                    /*Optimiser block bothRS block anyRS*/
                }
                if (!qOut.state) {
                    /*Optimiser blockEnd bothRS  blockEnd anyRS*/
                    qOut.setHi();
                }
            } else {
                if (qOut.state) {
                    qOut.setLo();
                    /*Optimiser block bothRS  block anyRS*/
                }
                if (!iqOut.state) {
                    /*Optimiser blockEnd bothRS  blockEnd anyRS*/
                    iqOut.setHi();
                }
            }
        }
        /*Optimiser blockEnd r*/
    }

    @Override
    public InPin getOptimised(boolean keepSetters) {
        boolean anyRs = parent.rPin.used || parent.sPin.used;
        boolean bothRs = parent.rPin.used && parent.sPin.used;
        ClassOptimiser<DcCPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (!anyRs) {
            optimiser.cut("anyRS");
        } else if (!bothRs) {
            optimiser.cut("bothRS");
        }
        DcCPin build = optimiser.build();
        parent.cPin = build;
        parent.inPins.put(id, build);
        return build;
    }
}
