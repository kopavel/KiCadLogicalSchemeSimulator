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
import pko.KiCadLogicalSchemeSimulator.api.pins.in.FallingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.RisingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.stream.IntStream;

public class Shifter extends SchemaPart {
    private final InPin dPins;
    private final InPin dsPins;
    private final long hiDsMask;
    private final long outMask;
    private long latch = 0;
    private OutPin out;
    private boolean plInactive;

    protected Shifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        int qSize = dSize;
        if (params.containsKey("qSize")) {
            qSize = Integer.parseInt(params.get("qSize"));
        }
        dPins = addInPin(new InPin("D", this, dSize) {
            @Override
            public void onChange(long newState, boolean hiImpedance, boolean strong) {
                if (!plInactive) {
                    latch = dPins.getState();
                }
            }
        });
        boolean plReverse = params.containsKey("plReverse");
        outMask = Utils.getMaskForSize(dSize);
        hiDsMask = 1L << (dSize - 1);
        dsPins = addInPin("DS", 1);
        if (plReverse) {
            addInPin(new InPin("PL", this) {
                @Override
                public void onChange(long newState, boolean hiImpedance, boolean strong) {
                    if (hiImpedance) {
                        throw new FloatingPinException(this);
                    }
                    plInactive = newState > 0;
                    if (!plInactive) {
                        latch = dPins.getState();
                        out.setState(latch & out.mask);
                    }
                }
            });
        } else {
            addInPin(new InPin("PL", this) {
                @Override
                public void onChange(long newState, boolean hiImpedance, boolean strong) {
                    if (hiImpedance) {
                        throw new FloatingPinException(this);
                    }
                    plInactive = newState == 0;
                    if (!plInactive) {
                        latch = dPins.getState();
                        out.setState(latch & out.mask);
                    }
                }
            });
        }
        if (reverse) {
            addInPin(new FallingEdgeInPin("CP", this) {
                @Override
                public void onFallingEdge() {
                    if (plInactive && latch > 0) {
                        latch = (latch << 1) & outMask;
                        if (dsPins.state > 0) {
                            latch = latch | 1;
                        }
                        out.setState(latch & out.mask);
                    }
                }
            });
            addInPin(new FallingEdgeInPin("CN", this) {
                @Override
                public void onFallingEdge() {
                    if (plInactive && latch > 0) {
                        latch = latch >> 1;
                        if (dsPins.state > 0) {
                            latch = latch | hiDsMask;
                        }
                        out.setState(latch & out.mask);
                    }
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("CP", this) {
                @Override
                public void onRisingEdge() {
                    if (plInactive && latch > 0) {
                        latch = (latch << 1) & outMask;
                        if (dsPins.state > 0) {
                            latch = latch | 1;
                        }
                        out.setState(latch & out.mask);
                    }
                }
            });
            addInPin(new RisingEdgeInPin("CN", this) {
                @Override
                public void onRisingEdge() {
                    if (plInactive && latch > 0) {
                        latch = latch >> 1;
                        if (dsPins.state > 0) {
                            latch = latch | hiDsMask;
                        }
                        out.setState(latch & out.mask);
                    }
                }
            });
        }
        addOutPin("Q",
                qSize,
                IntStream.range(0, qSize).mapToObj(String::valueOf)
                        .map(pos -> "Q" + pos).toArray(String[]::new));
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
