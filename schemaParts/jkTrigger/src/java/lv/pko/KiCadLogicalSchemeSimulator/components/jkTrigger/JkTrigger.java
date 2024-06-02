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
package lv.pko.KiCadLogicalSchemeSimulator.components.jkTrigger;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FallingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class JkTrigger extends SchemaPart {
    private final InPin jPin;
    private final InPin kPin;
    private final InPin rPin;
    private final InPin sPin;
    private OutPin qOut;
    private OutPin iqOut;
    private boolean clockEnabled = true;

    public JkTrigger(String id, String sParam) {
        super(id, sParam);
        jPin = addInPin("J", 1);
        kPin = addInPin("K", 1);
        rPin = addInPin(new InPin("R", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                clockEnabled = (newState | sPin.state) == 0;
                if (newState > 0) {
                    iqOut.setState(hiState);
                    qOut.setState(sPin.state > 0 ? hiState : loState);
                }
            }
        });
        sPin = addInPin(new InPin("S", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                clockEnabled = (newState | sPin.state) == 0;
                if (newState > 0) {
                    qOut.setState(hiState);
                    iqOut.setState(rPin.state > 0 ? hiState : loState);
                }
            }
        });
        if (reverse) {
            addInPin(new FallingEdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    if (clockEnabled) {
                        boolean jState = jPin.state > 0;
                        boolean kState = kPin.state > 0;
                        if (jState || kState) {
                            if (jState && !kState) {
                                qOut.setState(1);
                                iqOut.setState(0);
                            } else if (!jState) {
                                qOut.setState(1);
                                iqOut.setState(0);
                            } else {
                                qOut.setState(qOut.state ^ 1);
                                iqOut.setState(iqOut.state ^ 1);
                            }
                        }
                    }
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("C", this) {
                @Override
                public void onRisingEdge() {
                    if (clockEnabled) {
                        boolean jState = jPin.state > 0;
                        boolean kState = kPin.state > 0;
                        if (jState || kState) {
                            if (jState && !kState) {
                                qOut.setState(1);
                                iqOut.setState(0);
                            } else if (!jState) {
                                qOut.setState(1);
                                iqOut.setState(0);
                            } else {
                                qOut.setState(qOut.state ^ 1);
                                iqOut.setState(iqOut.state ^ 1);
                            }
                        }
                    }
                }
            });
        }
        addOutPin("Q", 1);
        addOutPin("~{Q}", 1);
    }

    @Override
    public void initOuts() {
        qOut = getOutPin("Q");
        qOut.state = 0;
        iqOut = getOutPin("~{Q}");
        iqOut.state = 1;
    }
}
