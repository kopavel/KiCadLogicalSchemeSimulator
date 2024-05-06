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
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FallingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class DCounter extends SchemaPart {
    private final InPin jPin;
    private OutPin outPin;
    private OutPin cOutPin;
    private boolean countUp = true;
    private boolean clockEnabled = true;
    private boolean presetDisabled = false;
    private long count = 0;
    private long maxCount = 15;

    protected DCounter(String id, String sParam) {
        super(id, sParam);
        addOutPin("Q", 4);
        addInPin(new InPin("CI", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                clockEnabled = newState > 0;
                if ((countUp && (count == maxCount)) || (!countUp && (count == 0))) {
                    cOutPin.setState(clockEnabled ? 0 : 1);
                }
            }
        });
        jPin = addInPin(new InPin("J", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (!presetDisabled) {
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
                if (!presetDisabled) {
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
                maxCount = rawState > 0 ? 15 : 9;
            }
        });
        if (reverse) {
            addInPin(new FallingEdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    process();
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("C", this) {
                @Override
                public void onRisingEdge() {
                    process();
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
        if (clockEnabled && presetDisabled) {
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
