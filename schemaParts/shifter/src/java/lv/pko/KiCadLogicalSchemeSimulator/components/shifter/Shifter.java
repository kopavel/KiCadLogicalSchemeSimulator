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
package lv.pko.KiCadLogicalSchemeSimulator.components.shifter;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FallingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class Shifter extends SchemaPart {
    private final InPin dPins;
    private final InPin dsPins;
    private final long hiDsMask;
    private final boolean plReverse;
    private long latch = 0;
    private OutPin out;
    private long outMask;
    private boolean plInactive;

    protected Shifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        dPins = addInPin(new InPin("D", this, dSize) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (!plInactive) {
                    latch = dPins.getState();
                }
            }
        });
        plReverse = params.containsKey("plReverse");
        for (int i = 0; i < dSize; i++) {
            outMask = outMask << 1 | 1;
        }
        hiDsMask = 1L << (dSize - 1);
        dsPins = addInPin("DS", 1);
        addInPin(new InPin("PL", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                plInactive = (newState == 0) ^ plReverse;
                if (!plInactive) {
                    latch = dPins.getState();
                    out.setState(latch);
                }
            }
        });
        if (reverse) {
            addInPin(new FallingEdgeInPin("CP", this) {
                @Override
                public void onFallingEdge() {
                    if (latch > 0) {
                        if (plInactive) {
                            latch = (latch << 1) & outMask;
                            if (dsPins.rawState > 0) {
                                latch = latch | 1;
                            }
                            out.setState(latch);
                        }
                    }
                }
            });
            addInPin(new FallingEdgeInPin("CN", this) {
                @Override
                public void onFallingEdge() {
                    if (latch > 0) {
                        if (plInactive) {
                            latch = (latch >> 1) & outMask;
                            if (dsPins.rawState > 0) {
                                latch = latch | hiDsMask;
                            }
                            out.setState(latch);
                        }
                    }
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("CP", this) {
                @Override
                public void onRisingEdge() {
                    if (latch > 0) {
                        if (plInactive) {
                            latch = (latch << 1) & outMask;
                            if (dsPins.rawState > 0) {
                                latch = latch | 1;
                            }
                            out.setState(latch);
                        }
                    }
                }
            });
            addInPin(new RisingEdgeInPin("CN", this) {
                @Override
                public void onRisingEdge() {
                    if (latch > 0) {
                        if (plInactive) {
                            latch = (latch >> 1) & outMask;
                            if (dsPins.rawState > 0) {
                                latch = latch | hiDsMask;
                            }
                            out.setState(latch);
                        }
                    }
                }
            });
        }
        addOutPin("Q", dSize);
    }

    @Override
    public void initOuts() {
        out = getOutPin("Q");
    }

    @Override
    public String extraState() {
        return "latch" + "\n" + Long.toBinaryString(latch);
    }

    @Override
    public void reset() {
        latch = 0;
        out.setState(0);
    }
}
