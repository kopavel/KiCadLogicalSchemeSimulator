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
package lv.pko.KiCadLogicalSchemeSimulator.components.dCounter;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.EdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class DCounter extends SchemaPart {
    private final InPin jPin;
    private final boolean carryReverse;
    private final boolean bdReverse;
    private OutPin outPin;
    private OutPin cOutPin;
    private boolean countUp = true;
    private boolean ciState = true;
    private boolean cState;
    private boolean eState;
    private boolean presetDisabled = false;
    private long count = 0;
    private long maxCount = 15;
    private boolean resetInactive;

    protected DCounter(String id, String sParam) {
        super(id, sParam);
        carryReverse = params.containsKey("carryReverse");
        bdReverse = params.containsKey("bdReverse");
        boolean eReverse = params.containsKey("eReverse");
        addOutPin("Q", 4);
        addInPin(new InPin("CI", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                ciState = (newState > 0) ^ carryReverse;
                if ((countUp && (count == maxCount)) || (!countUp && (count == 0))) {
                    cOutPin.setState(ciState ? 0 : 1);
                }
            }
        });
        jPin = addInPin(new InPin("J", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (!presetDisabled && resetInactive) {
                    count = getState();
                    outPin.setState(count);
                }
            }
        });
        addInPin(new InPin("PE", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                presetDisabled = newState == 0;
                if (!presetDisabled && resetInactive) {
                    count = jPin.getState();
                    outPin.setState(count);
                }
            }
        });
        addInPin(new InPin("UD", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                countUp = rawState > 0;
            }
        });
        addInPin(new InPin("BD", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                maxCount = ((rawState > 0) ^ bdReverse) ? 15 : 9;
            }
        });
        addInPin(new InPin("R", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                resetInactive = newState == 0;
                if (!resetInactive) {
                    count = 0;
                    outPin.setState(0);
                }
            }
        });
        if (reverse) {
            addInPin(new EdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    cState = true;
                    if (eState) {
                        process();
                    }
                }

                @Override
                public void onRisingEdge() {
                    cState = false;
                }
            });
        } else {
            addInPin(new EdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    cState = false;
                }

                @Override
                public void onRisingEdge() {
                    cState = true;
                    if (eState) {
                        process();
                    }
                }
            });
        }
        if (eReverse) {
            addInPin(new EdgeInPin("E", this) {
                @Override
                public void onFallingEdge() {
                    eState = false;
                }

                @Override
                public void onRisingEdge() {
                    eState = true;
                    if (cState) {
                        process();
                    }
                }
            });
        } else {
            addInPin(new EdgeInPin("E", this) {
                @Override
                public void onFallingEdge() {
                    eState = true;
                    if (cState) {
                        process();
                    }
                }

                @Override
                public void onRisingEdge() {
                    eState = false;
                }
            });
        }
    }

    @Override
    public void initOuts() {
        outPin = getOutPin("Q");
        outPin.useBitPresentation = true;
        cOutPin = getOutPin("CO");
    }

    private void process() {
        if (ciState && presetDisabled && resetInactive) {
            if (countUp) {
                count++;
                if (count == maxCount) {
                    cOutPin.setState(0);
                } else {
                    cOutPin.setState(1);
                    if (count > maxCount) {
                        count = 0;
                    }
                }
            } else {
                count--;
                if (count == 0) {
                    cOutPin.setState(0);
                } else {
                    cOutPin.setState(1);
                    if (count < 0) {
                        count = maxCount;
                    }
                }
            }
        }
    }
}
