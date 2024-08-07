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
package pko.KiCadLogicalSchemeSimulator.components.dCounter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.NoFloatingInBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;

public class DCounter extends SchemaPart {
    private final InBus jBus;
    private final boolean carryHi;
    private final boolean carryLo;
    private final InPin udPin;
    public long maxCount;
    private Bus outBus;
    private Pin cOutPin;
    private boolean ciState;
    private boolean cState = true;
    private boolean eState;
    private boolean presetDisabled = true;
    private boolean resetInactive = true;

    protected DCounter(String id, String sParam) {
        super(id, sParam);
        boolean eReverse = params.containsKey("eReverse");
        addOutBus("Q", 4);
        addOutPin("CO");
        if (params.containsKey("carryReverse")) {
            addInPin(new NoFloatingInPin("CI", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    ciState = !newState;
                }
            });
            carryHi = false;
            carryLo = true;
            udPin = addInPin(new InPin("UD", this, true) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    boolean newOutState = (!newState || outBus.state != maxCount) && (newState || outBus.state != 0);
                    if (cOutPin.state != newOutState) {
                        cOutPin.state = newOutState;
                        cOutPin.setState(newOutState);
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    resetInactive = !newState;
                    if (!resetInactive && outBus.state != 0) {
                        outBus.state = 0;
                        outBus.setState(0);
                        if (!cOutPin.state) {
                            cOutPin.state = true;
                            cOutPin.setState(true);
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("CI", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    ciState = newState;
                }
            });
            carryHi = true;
            carryLo = false;
            udPin = addInPin(new InPin("UD", this, true) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    boolean newOutState = newState && outBus.state == maxCount || !newState && outBus.state == 0;
                    if (cOutPin.state != newOutState) {
                        cOutPin.state = newOutState;
                        cOutPin.setState(newOutState);
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    resetInactive = !newState;
                    if (!resetInactive && outBus.state != 0) {
                        outBus.state = 0;
                        outBus.setState(0);
                        if (cOutPin.state) {
                            cOutPin.state = false;
                            cOutPin.setState(false);
                        }
                    }
                }
            });
        }
        jBus = addInBus(new NoFloatingInBus("J", this, 4) {
            @Override
            public void setState(long newState) {
                state = newState;
                if (!presetDisabled && resetInactive && outBus.state != newState) {
                    outBus.state = newState;
                    outBus.setState(newState);
                }
            }
        });
        addInPin(new NoFloatingInPin("PE", this) {
            @Override
            public void setState(boolean newState) {
                state = newState;
                presetDisabled = !newState;
                if (!presetDisabled && resetInactive && outBus.state != jBus.state) {
                    outBus.state = jBus.state;
                    outBus.setState(outBus.state);
                }
            }
        });
        if (params.containsKey("bdReverse")) {
            addInPin(new InPin("BD", this) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    maxCount = !newState ? 15 : 9;
                }
            });
            maxCount = 9;
        } else {
            addInPin(new InPin("BD", this) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    maxCount = newState ? 15 : 9;
                }
            });
            maxCount = 15;
        }
        if (reverse) {
            addInPin(new NoFloatingInPin("C", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        cState = false;
                    } else {
                        cState = true;
                        if (eState) {
                            process();
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("C", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        cState = true;
                        if (eState) {
                            process();
                        }
                    } else {
                        cState = false;
                    }
                }
            });
        }
        if (eReverse) {
            addInPin(new NoFloatingInPin("E", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        eState = false;
                    } else {
                        eState = true;
                        if (cState) {
                            process();
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("E", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        eState = true;
                        if (cState) {
                            process();
                        }
                    } else {
                        eState = false;
                    }
                }
            });
        }
        eState = !eReverse;
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        outBus.useBitPresentation = true;
        outBus.state = 0;
        outBus.hiImpedance = false;
        cOutPin = getOutPin("CO");
        cOutPin.state = carryLo;
        cOutPin.hiImpedance = false;
    }

    @Override
    public void reset() {
        outBus.state = 0;
        outBus.hiImpedance = false;
        outBus.setState(0);
        cOutPin.state = carryLo;
        cOutPin.hiImpedance = false;
        cOutPin.setState(carryLo);
    }

    private void process() {
        if (ciState && presetDisabled && resetInactive) {
            if (udPin.state) {
                outBus.state++;
                if (outBus.state == maxCount) {
                    cOutPin.setState(carryHi);
                } else {
                    cOutPin.setState(carryLo);
                    if (outBus.state > maxCount) {
                        outBus.state = 0;
                    }
                }
            } else {
                outBus.state--;
                if (outBus.state == 0) {
                    cOutPin.setState(carryHi);
                } else {
                    cOutPin.setState(carryLo);
                    if (outBus.state < 0) {
                        outBus.state = maxCount;
                    }
                }
            }
            outBus.setState(outBus.state);
        }
    }
}
