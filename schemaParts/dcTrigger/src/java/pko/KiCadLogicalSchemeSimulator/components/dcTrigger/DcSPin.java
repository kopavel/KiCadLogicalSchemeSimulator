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

public class DcSPin extends InPin {
    public final boolean reverse;
    public final DcTrigger parent;
    public InPin rPin;
    public Pin qOut;
    public Pin iqOut;

    public DcSPin(String id, DcTrigger parent, boolean reverse) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        state = reverse;
        rPin = parent.rPin;
        qOut = parent.qOut;
        iqOut = parent.iqOut;
    }

    /*Optimiser constructor*/
    public DcSPin(DcSPin oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        parent = oldPin.parent;
        state = oldPin.state;
        rPin = oldPin.rPin;
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
                        /*Optimiser line r bind false:rPin.state*///
                        rPin.state//
        ;

        /*Optimiser line o block nr*/
        if (!reverse) {
            /*Optimiser block nq*/
            if (
                /*Optimiser line rp*/
                    !rPin.state &&//
                            iqOut.state) {
                iqOut.setLo();
                /*Optimiser line rp block q*/
            }
            /*Optimiser line rq blockEnd nq*/
            if (!qOut.state) {
                /*Optimiser blockEnd rp*/
                qOut.setHi();
                /*Optimiser blockEnd q*/
            }
            /*Optimiser block rp block r blockEnd nr blockEnd q line o*/
        } else//
            if (!rPin.state) {
                /*Optimiser block nq*/
                if (qOut.state) {
                    qOut.setLo();
                }
                /*Optimiser blockEnd nq block q*/
                if (!iqOut.state) {
                    iqOut.setHi();
                }
                /*Optimiser blockEnd q*/
            }
        /*Optimiser blockEnd r blockEnd rp*/
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
                        /*Optimiser line r bind true:!rPin.state*///
                        !rPin.state//
        ;
        /*Optimiser line o block r*/
        if (reverse) {
            /*Optimiser block nq*/
            if (
                /*Optimiser line rp*/
                    rPin.state &&//
                            iqOut.state) {
                iqOut.setLo();
                /*Optimiser line rp block q*/
            }
            /*Optimiser line rq blockEnd nq*/
            if (!qOut.state) {
                qOut.setHi();
                /*Optimiser blockEnd q*/
            }
            /*Optimiser line o block rp blockEnd r block nr*/
        } else//
            if (rPin.state) {
                if (qOut.state) {
                    qOut.setLo();
                }
                if (!iqOut.state) {
                    iqOut.setHi();
                }
            }
        /*Optimiser blockEnd rp blockEnd nr*/
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<DcSPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (!rPin.used) {
            optimiser.cut("rp");
            if (iqOut.used) {
                optimiser.cut("rq");
            }
            if (reverse) {
                optimiser.bind("true", "false");
                optimiser.bind("false", "true");
            } else {
                optimiser.bind("true", "true");
                optimiser.bind("false", "false");
            }
        }
        if (source != null && !rPin.used) {
            optimiser.cut("setter");
        }
        if (!qOut.used) {
            optimiser.cut("q");
        } else if (!iqOut.used) {
            optimiser.cut("nq");
        }
        DcSPin build = optimiser.build();
        parent.sPin = build;
        parent.rPin.sPin = build;
        parent.replaceIn(this, build);
        if (source != null && !rPin.used) {
            build.source = source;
        }
        return build;
    }
}
