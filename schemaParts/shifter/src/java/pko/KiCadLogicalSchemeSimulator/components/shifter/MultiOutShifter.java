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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiOutShifter extends SchemaPart {
    private final InBus dBus;
    private final InPin dsPins;
    private final long hiDsMask;
    private final long outMask;
    private long latch = 0;
    private Bus out;
    private boolean plInactive;

    protected MultiOutShifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        int qSize = dSize;
        if (params.containsKey("qSize")) {
            qSize = Integer.parseInt(params.get("qSize"));
        }
        dBus = addInBus("D", dSize);
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
                        latch = dBus.getState();
                        if (out.state != latch) {
                            out.state = latch;
                            out.setState(latch);
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("PL", this) {
                @Override
                public void setState(boolean newState) {
                    plInactive = !newState;
                    if (newState) {
                        latch = dBus.getState();
                        if (out.state != latch) {
                            out.state = latch;
                            out.setState(latch);
                        }
                    }
                }
            });
        }
        if (reverse) {
            addInPin(new NoFloatingInPin("CP", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (!state && plInactive && latch != 0) {
                        latch = (latch << 1) & outMask;
                        if (dsPins.state) {
                            latch |= 1;
                        }
                        if (out.state != latch) {
                            out.state = latch;
                            out.setState(latch);
                        }
                    }
                }
            });
            addInPin(new NoFloatingInPin("CN", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (!state && plInactive && latch != 0) {
                        latch = latch >> 1;
                        if (dsPins.state) {
                            latch |= hiDsMask;
                        }
                        if (out.state != latch) {
                            out.state = latch;
                            out.setState(latch);
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("CP", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state && plInactive && latch != 0) {
                        latch = (latch << 1) & outMask;
                        if (dsPins.state) {
                            latch |= 1;
                        }
                        if (out.state != latch) {
                            out.state = latch;
                            out.setState(latch);
                        }
                    }
                }
            });
            addInPin(new NoFloatingInPin("CN", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state && plInactive && latch != 0) {
                        latch = latch >> 1;
                        if (dsPins.state) {
                            latch |= hiDsMask;
                        }
                        if (out.state != latch) {
                            out.state = latch;
                            out.setState(latch);
                        }
                    }
                }
            });
        }
        addOutBus("Q", qSize);
    }

    @Override
    public void initOuts() {
        out = getOutBus("Q");
    }

    @Override
    public String extraState() {
        return "latch" + "\n" + Long.toBinaryString(latch);
    }

    @Override
    public void reset() {
        latch = 0;
        out.state = 0;
        out.hiImpedance = false;
        out.setState(0);
    }
}
