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
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class DcRPin extends InPin {
    public final boolean reverse;
    public final DcTrigger parent;
    public InPin sPin;
    public Pin qOut;
    public Pin iqOut;

    public DcRPin(String id, DcTrigger parent, boolean reverse) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        state = reverse;
        sPin = parent.sPin;
        qOut = parent.qOut;
        iqOut = parent.iqOut;
    }

    /*Optimiser constructor*/
    public DcRPin(DcRPin oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        parent = oldPin.parent;
        state = oldPin.state;
        sPin = oldPin.sPin;
        qOut = oldPin.qOut;
        iqOut = oldPin.iqOut;
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
                        /*Optimiser line r bind false:sPin.state*/
                        sPin.state//
        ;

        /*Optimiser line o block nr*/
        if (!reverse) {
            /*Optimiser block q*/
            if (
                /*Optimiser line s*/
                    !sPin.state &&//
                            qOut.state) {
                qOut.setLo();
                /*Optimiser line s block nq*/
            }
            /*Optimiser line sq blockEnd q*/
            if (!iqOut.state) {
                /*Optimiser blockEnd s*/
                iqOut.setHi();
                /*Optimiser blockEnd nq*/
            }
            /*Optimiser block s block r blockEnd nr line o*/
        } else//
            if (!sPin.state) {
                /*Optimiser block q*/
                if (!qOut.state) {
                    qOut.setHi();
                }
                /*Optimiser blockEnd q block nq*/
                if (iqOut.state) {
                    iqOut.setLo();
                }
                /*Optimiser blockEnd nq*/
            }
        /*Optimiser blockEnd r blockEnd s*/
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
                        && !reverse &&
                        /*Optimiser line r bind true:!sPin.state*/
                        !sPin.state//
        ;
        /*Optimiser line o block r*/
        if (reverse) {
            /*Optimiser block q*/
            if (
                /*Optimiser line s*/
                    sPin.state &&//
                            qOut.state) {
                qOut.setLo();
                /*Optimiser line s block nq*/
            }
            /*Optimiser line sq blockEnd q*/
            if (!iqOut.state) {
                iqOut.setHi();
                /*Optimiser blockEnd nq*/
            }
            /*Optimiser line o block s blockEnd r block nr*/
        } else//
            if (sPin.state) {
                if (iqOut.state) {
                    iqOut.setLo();
                }
                if (!qOut.state) {
                    qOut.setHi();
                }
            }
        /*Optimiser blockEnd s blockEnd nr*/
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<DcRPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (!sPin.used) {
            optimiser.cut("s");
            if (qOut.used) {
                optimiser.cut("sq");
            }
            if (reverse) {
                optimiser.bind("true", "false");
                optimiser.bind("false", "true");
            } else {
                optimiser.bind("true", "true");
                optimiser.bind("false", "false");
            }
        }
        if (source != null && !sPin.used) {
            optimiser.cut("setter");
        }
        if (!qOut.used) {
            optimiser.cut("q");
        } else if (!iqOut.used) {
            optimiser.cut("nq");
        }
        DcRPin build = optimiser.build();
        parent.rPin = build;
        parent.sPin.rPin = build;
        parent.replaceIn(this, build);
        return build;
    }
}
