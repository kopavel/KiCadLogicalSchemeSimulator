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
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.FallingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.RisingEdgeInPin;

public class DcTrigger extends SchemaPart {
    private final InPin dPin;
    private final InPin rPin;
    private final InPin sPin;
    private Pin qOut;
    private Pin iqOut;
    private boolean clockEnabled = true;

    protected DcTrigger(String id, String sParam) {
        super(id, sParam);
        dPin = addInPin("D");
        rPin = addInPin(new InPin("R", this) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setState(boolean newState, boolean newStrong) {
                hiImpedance = false;
                state = newState;
                strong = newStrong;
                clockEnabled = !(newState | sPin.state);
                if (!newState) {
                    if (!iqOut.state) {
                        iqOut.state = true;
                        iqOut.setState(true, true);
                    }
                    if (qOut.state != sPin.state) {
                        qOut.state = sPin.state;
                        qOut.setState(qOut.state, true);
                    }
                } else if (!sPin.state) {
                    if (!qOut.state) {
                        qOut.state = true;
                        qOut.setState(true, true);
                    }
                    if (iqOut.state) {
                        iqOut.state = false;
                        iqOut.setState(false, true);
                    }
                }
            }
        });
        sPin = addInPin(new InPin("S", this) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setState(boolean newState, boolean strong) {
                state = newState;
                hiImpedance = false;
                clockEnabled = !(newState | rPin.state);
                if (!newState) {
                    if (!qOut.state) {
                        qOut.state = true;
                        qOut.setState(true, true);
                    }
                    if (iqOut.state != rPin.state) {
                        iqOut.state = rPin.state;
                        iqOut.setState(rPin.state, true);
                    }
                } else if (rPin.state) {
                    if (!qOut.state) {
                        qOut.state = true;
                        qOut.setState(false, true);
                    }
                    if (!iqOut.state) {
                        iqOut.state = true;
                        iqOut.setState(true, true);
                    }
                }
            }
        });
        if (reverse) {
            addInPin(new FallingEdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    store();
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("C", this) {
                @Override
                public void onRisingEdge() {
                    store();
                }
            });
        }
        addOutPin("Q", true, true);
        addOutPin("~{Q}", false, true);
    }

    @Override
    public void initOuts() {
        qOut = getOutPin("Q");
        iqOut = getOutPin("~{Q}");
    }

    private void store() {
        if (clockEnabled) {
            if (dPin.state) {
                if (!qOut.state) {
                    qOut.state = true;
                    qOut.setState(true, true);
                }
                if (iqOut.state) {
                    iqOut.state = false;
                    iqOut.setState(false, true);
                }
            } else {
                if (qOut.state) {
                    qOut.state = false;
                    qOut.setState(false, true);
                }
                if (!iqOut.state) {
                    iqOut.state = true;
                    iqOut.setState(true, true);
                }
            }
        }
    }
}
