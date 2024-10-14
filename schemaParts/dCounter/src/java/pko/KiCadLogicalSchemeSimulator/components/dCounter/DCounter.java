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
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

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
            addInPin(new InPin("CI", this) {
                @Override
                public void setHi() {
                    state = true;
                    ciState = false;
                }

                @Override
                public void setLo() {
                    state = false;
                    ciState = true;
                }
            });
            carryHi = false;
            carryLo = true;
            udPin = addInPin(new InPin("UD", this, true) {
                @Override
                public void setHi() {
                    state = true;
                    if (outBus.state == maxCount) {
                        if (cOutPin.state) {
                            cOutPin.setLo();
                        }
                    } else if (!cOutPin.state) {
                        cOutPin.setHi();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (outBus.state == 0) {
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    } else if (cOutPin.state) {
                        cOutPin.setLo();
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    resetInactive = false;
                    if (outBus.state != 0) {
                        outBus.setState(0);
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    resetInactive = true;
                }
            });
        } else {
            addInPin(new InPin("CI", this) {
                @Override
                public void setHi() {
                    state = true;
                    ciState = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    ciState = false;
                }
            });
            carryHi = true;
            carryLo = false;
            udPin = addInPin(new InPin("UD", this, true) {
                @Override
                public void setHi() {
                    state = true;
                    if (outBus.state == maxCount) {
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    } else if (cOutPin.state) {
                        cOutPin.setLo();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (outBus.state == 0) {
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    } else if (cOutPin.state) {
                        cOutPin.setLo();
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    resetInactive = false;
                    if (outBus.state != 0) {
                        outBus.setState(0);
                        if (cOutPin.state) {
                            cOutPin.setLo();
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    resetInactive = true;
                }
            });
        }
        jBus = addInBus(new InBus("J", this, 4) {
            @Override
            public void setState(long newState) {
                state = newState;
                if (!presetDisabled && resetInactive && outBus.state != newState) {
                    outBus.setState(newState);
                }
            }
        });
        addInPin(new InPin("PE", this) {
            @Override
            public void setHi() {
                state = true;
                presetDisabled = false;
                if (resetInactive && outBus.state != jBus.state) {
                    outBus.setState(jBus.state);
                }
            }

            @Override
            public void setLo() {
                state = false;
                presetDisabled = true;
            }
        });
        if (params.containsKey("bdReverse")) {
            addInPin(new InPin("BD", this) {
                @Override
                public void setHi() {
                    state = true;
                    maxCount = 9;
                }

                @Override
                public void setLo() {
                    state = false;
                    maxCount = 15;
                }
            });
            maxCount = 15;
        } else {
            addInPin(new InPin("BD", this) {
                @Override
                public void setHi() {
                    state = true;
                    maxCount = 15;
                }

                @Override
                public void setLo() {
                    state = false;
                    maxCount = 9;
                }
            });
            maxCount = 9;
        }
        if (reverse) {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                    cState = false;
                }

                @Override
                public void setLo() {
                    state = false;
                    cState = true;
                    if (eState) {
                        process();
                    }
                }
            });
        } else {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                    cState = true;
                    if (eState) {
                        process();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    cState = false;
                }
            });
        }
        if (eReverse) {
            addInPin(new InPin("E", this) {
                @Override
                public void setHi() {
                    state = true;
                    eState = false;
                }

                @Override
                public void setLo() {
                    state = false;
                    eState = true;
                    if (cState) {
                        process();
                    }
                }
            });
        } else {
            addInPin(new InPin("E", this) {
                @Override
                public void setHi() {
                    state = true;
                    eState = true;
                    if (cState) {
                        process();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    eState = false;
                }
            });
        }
        eState = eReverse;
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        outBus.useBitPresentation = true;
        cOutPin = getOutPin("CO");
        outBus.setState(0);
        if (carryLo) {
            cOutPin.setHi();
        } else {
            cOutPin.setLo();
        }
    }

    @Override
    public void reset() {
        outBus.setState(0);
        if (carryLo) {
            cOutPin.setHi();
        } else {
            cOutPin.setLo();
        }
    }

    private void process() {
        if (ciState && presetDisabled && resetInactive) {
            if (udPin.state) {
                outBus.state++;
                if (outBus.state == maxCount) {
                    if (carryHi) {
                        cOutPin.setHi();
                    } else {
                        cOutPin.setLo();
                    }
                } else {
                    if (carryLo) {
                        cOutPin.setHi();
                    } else {
                        cOutPin.setLo();
                    }
                    if (outBus.state > maxCount) {
                        outBus.setState(0);
                    }
                }
            } else {
                outBus.state--;
                if (outBus.state == 0) {
                    if (carryHi) {
                        cOutPin.setHi();
                    } else {
                        cOutPin.setLo();
                    }
                } else {
                    if (carryLo) {
                        cOutPin.setHi();
                    } else {
                        cOutPin.setLo();
                    }
                    if (outBus.state < 0) {
                        outBus.setState(maxCount);
                    }
                }
            }
        }
    }
}
