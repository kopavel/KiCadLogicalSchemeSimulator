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
package pko.KiCadLogicalSchemeSimulator.components.shifter;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.FallingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.RisingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class Shifter extends SchemaPart {
    private final InBus dBus;
    private final InPin dsPins;
    private final long hiDsMask;
    private final long outMask;
    private long latch = 0;
    private Pin out;
    private boolean plInactive;

    protected Shifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        dBus = addInBus(new CorrectedInBus("D", this, dSize) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setState(long newState) {
                state = newState;
                if (!plInactive) {
                    latch = dBus.state;
                }
                hiImpedance = false;
            }
        });
        boolean plReverse = params.containsKey("plReverse");
        outMask = Utils.getMaskForSize(dSize);
        hiDsMask = 1L << (dSize - 1);
        dsPins = addInPin("DS");
        if (plReverse) {
            addInPin(new NoFloatingInPin("PL", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    plInactive = newState;
                    if (!plInactive) {
                        latch = dBus.state;
                        if (out.state == ((latch & 1) == 0)) {
                            out.state = ((latch & 1) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("PL", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    plInactive = !newState;
                    if (!plInactive) {
                        latch = dBus.state;
                        if (out.state == ((latch & 1) == 0)) {
                            out.state = ((latch & 1) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
        }
        if (reverse) {
            addInPin(new FallingEdgeInPin("CP", this) {
                @Override
                public void onFallingEdge() {
                    if (plInactive && latch != 0) {
                        latch = (latch << 1) & outMask;
                        if (dsPins.state) {
                            latch = latch | 1;
                        }
                        if (out.state == ((latch & 1) == 0)) {
                            out.state = ((latch & 1) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
            addInPin(new FallingEdgeInPin("CN", this) {
                @Override
                public void onFallingEdge() {
                    if (plInactive && latch != 0) {
                        latch = latch >> 1;
                        if (dsPins.state) {
                            latch = latch | hiDsMask;
                        }
                        if (out.state == ((latch & 1) == 0)) {
                            out.state = ((latch & 1) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("CP", this) {
                @Override
                public void onRisingEdge() {
                    if (plInactive && latch != 0) {
                        latch = (latch << 1) & outMask;
                        if (dsPins.state) {
                            latch = latch | 1;
                        }
                        if (out.state == ((latch & 1) == 0)) {
                            out.state = ((latch & 1) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
            addInPin(new RisingEdgeInPin("CN", this) {
                @Override
                public void onRisingEdge() {
                    if (plInactive && latch != 0) {
                        latch = latch >> 1;
                        if (dsPins.state) {
                            latch = latch | hiDsMask;
                        }
                        if (out.state == ((latch & 1) == 0)) {
                            out.state = ((latch & 1) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
        }
        addOutPin("Q", false);
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
        out.state = false;
        out.hiImpedance = false;
        out.setState(false);
    }
}
