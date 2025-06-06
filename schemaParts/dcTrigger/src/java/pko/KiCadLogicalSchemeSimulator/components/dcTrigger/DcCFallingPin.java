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
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.FallingEdgePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class DcCFallingPin extends FallingEdgePin {
    public final DcTrigger parent;
    public final InPin dPin;
    public Pin qOut;
    public Pin iqOut;

    public DcCFallingPin(String id, DcTrigger parent) {
        super(id, parent);
        this.parent = parent;
        dPin = parent.dPin;
        qOut = parent.qOut;
        iqOut = parent.iqOut;
    }

    /*Optimiser constructor*/
    public DcCFallingPin(DcCFallingPin oldPin, String variantId) {
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
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line anyRS*/
        if (parent.clockEnabled) {
            if (dPin.state) {
                /*Optimiser block q bind nq:!qOut*/
                if (!qOut.state) {
                    qOut.setHi();
                    /*Optimiser line bothRS block nq*/
                }
                /*Optimiser line bothRSQ blockEnd q*/
                if (iqOut.state) {
                    iqOut.setLo();
                    /*Optimiser blockEnd nq*/
                }
            } else {
                /*Optimiser block q*/
                if (qOut.state) {
                    qOut.setLo();
                    /*Optimiser line bothRS block nq*/
                }
                /*Optimiser line bothRSQ blockEnd q*/
                if (!iqOut.state) {
                    iqOut.setHi();
                    /*Optimiser blockEnd nq*/
                }
            }
            /*Optimiser line anyRS*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        boolean anyRs = parent.rPin.used || parent.sPin.used;
        boolean bothRs = parent.rPin.used && parent.sPin.used;
        ClassOptimiser<DcCFallingPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (!anyRs) {
            optimiser.cut("anyRS");
        }
        if (!bothRs) {
            optimiser.cut("bothRS");
            if (qOut.used) {
                optimiser.cut("bothRSQ");
                if (iqOut.used) {
                    optimiser.bind("nq", "iqOut");
                }
            }
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!iqOut.used) {
            optimiser.cut("nq");
        } else if (!qOut.used) {
            optimiser.cut("q");
        }
        DcCFallingPin build = optimiser.build();
        parent.ncPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
