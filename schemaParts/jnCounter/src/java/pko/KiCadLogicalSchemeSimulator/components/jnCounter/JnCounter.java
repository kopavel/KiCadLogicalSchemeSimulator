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
package pko.KiCadLogicalSchemeSimulator.components.jnCounter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.EdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.FallingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.RisingEdgeInPin;

public class JnCounter extends SchemaPart {
    private final long coMax;
    private Bus outBus;
    private Pin carryOutPin;
    private long countMax = 1;
    private boolean clockEnabled = true;

    protected JnCounter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int pinAmount;
        try {
            pinAmount = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must positive >=2 number");
        }
        if (pinAmount < 2) {
            throw new RuntimeException("Component " + id + " size must positive >=2 number");
        }
        for (int i = 1; i < pinAmount; i++) {
            countMax = countMax << 1;
        }
        coMax = countMax / 2;
        addOutBus("Q", pinAmount);
        addOutBus("CO", pinAmount);
        addInPin(new EdgeInPin("CI", this) {
            @Override
            public void onFallingEdge() {
                clockEnabled = true;
            }

            @Override
            public void onRisingEdge() {
                clockEnabled = false;
            }
        });
        if (reverse) {
            addInPin(new FallingEdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    if (clockEnabled) {
                        if (outBus.state >= countMax) {
                            outBus.state = 1;
                            if (carryOutPin.state) {
                                carryOutPin.state = false;
                                carryOutPin.setState(false);
                            }
                        } else {
                            outBus.state = outBus.state << 1;
                            if (carryOutPin.state != outBus.state >= coMax) {
                                carryOutPin.state = outBus.state >= coMax;
                                carryOutPin.setState(carryOutPin.state);
                            }
                        }
                        outBus.setState(outBus.state);
                    }
                }
            });
            addInPin(new EdgeInPin("R", this) {
                @Override
                public void onFallingEdge() {
                    clockEnabled = false;
                    if (outBus.state != 1) {
                        outBus.state = 1;
                        outBus.setState(outBus.state);
                    }
                }

                @Override
                public void onRisingEdge() {
                    clockEnabled = true;
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("C", this) {
                @Override
                public void onRisingEdge() {
                    if (clockEnabled) {
                        if (outBus.state >= countMax) {
                            outBus.state = 1;
                            if (carryOutPin.state) {
                                carryOutPin.state = false;
                                carryOutPin.setState(false);
                            }
                        } else {
                            outBus.state = outBus.state << 1;
                            if (carryOutPin.state != outBus.state >= coMax) {
                                carryOutPin.state = outBus.state >= coMax;
                                carryOutPin.setState(carryOutPin.state);
                            }
                        }
                        outBus.setState(outBus.state);
                    }
                }
            });
            addInPin(new EdgeInPin("R", this) {
                @Override
                public void onFallingEdge() {
                    clockEnabled = true;
                }

                @Override
                public void onRisingEdge() {
                    clockEnabled = false;
                    if (outBus.state != 1) {
                        outBus.state = 1;
                        outBus.setState(outBus.state);
                    }
                }
            });
        }
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        outBus.useBitPresentation = true;
        carryOutPin = getOutPin("CO");
    }

    @Override
    public void reset() {
        outBus.state = 1;
        outBus.setState(1);
        outBus.hiImpedance = false;
    }
}
