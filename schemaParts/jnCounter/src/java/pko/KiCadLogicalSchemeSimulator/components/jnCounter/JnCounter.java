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
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class JnCounter extends SchemaPart {
    public final long coMax;
    public long countMax = 1;
    private Bus outBus;
    private Pin carryOutPin;
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
        countMax = (long) Math.pow(2, pinAmount);
        coMax = (long) Math.pow(2, ((double) pinAmount / 2));
        addOutBus("Q", pinAmount);
        addOutPin("CO");
        addInPin(new InPin("CI", this) {
            @Override
            public void setState(boolean newState) {
                state = newState;
                clockEnabled = !state;
            }
        });
        if (reverse) {
            addInPin(new InPin("C", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (!state && clockEnabled) {
                        if (outBus.state >= countMax) {
                            outBus.state = 1;
                            if (carryOutPin.state) {
                                carryOutPin.setState(false);
                            }
                        } else {
                            outBus.state = outBus.state << 1;
                            if (carryOutPin.state != (outBus.state < coMax)) {
                                carryOutPin.setState(outBus.state < coMax);
                            }
                        }
                        outBus.setState(outBus.state);
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    clockEnabled = state;
                    if (!state) {
                        if (outBus.state != 1) {
                            outBus.setState(1);
                        }
                    }
                }
            });
        } else {
            addInPin(new InPin("C", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state && clockEnabled) {
                        if (outBus.state >= countMax) {
                            outBus.state = 1;
                            if (carryOutPin.state) {
                                carryOutPin.setState(false);
                            }
                        } else {
                            outBus.state = outBus.state << 1;
                            if (carryOutPin.state != outBus.state < coMax) {
                                carryOutPin.setState(outBus.state < coMax);
                            }
                        }
                        outBus.setState(outBus.state);
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        clockEnabled = false;
                        if (outBus.state != 1) {
                            outBus.setState(1);
                        }
                    } else {
                        clockEnabled = true;
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
        outBus.setState(1);
        carryOutPin.setState(true);
    }
}
