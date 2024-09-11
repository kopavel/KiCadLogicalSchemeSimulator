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
package pko.KiCadLogicalSchemeSimulator.components.multiplexer;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;

import java.util.ArrayList;
import java.util.List;

public class MaskedMultiplexer extends SchemaPart {
    private final InBus[] inBuses;
    private long outMask = -1;
    private int nState;
    private Bus outBus;

    protected MaskedMultiplexer(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("nSize")) {
            throw new RuntimeException("Component " + id + " has no parameter \"nSize\"");
        }
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int partsAmount = Integer.parseInt(params.get("size"));
        int nSize = Integer.parseInt(params.get("nSize"));
        if (nSize > 30) {
            throw new RuntimeException("Component " + id + " max nSize is 30");
        }
        int partSize = (int) Math.pow(2, nSize);
        inBuses = new InBus[partSize];
        if (reverse) {
            outMask = -1;
            addInPin(new NoFloatingInPin("OE", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    if (newState) {
                        outMask = 0;
                    } else {
                        outMask = -1;
                    }
                    InBus inBus = inBuses[nState];
                    if (!inBus.hiImpedance) {
                        if (newState && outBus.state != inBus.state) {
                            outBus.state = inBus.state;
                            outBus.setState(inBus.state);
                        } else if (!newState && outBus.state != 0) {
                            outBus.state = 0;
                            outBus.setState(0);
                        }
                    }
                }
            });
            for (int i = 0; i < partSize; i++) {
                long mask = 1L << i;
                long nMask = ~mask;
                addInPin(new NoFloatingInPin("OE" + (char) ('a' + i), this) {
                    @Override
                    public void setState(boolean newState) {
                        hiImpedance = false;
                        state = newState;
                        if (newState) {
                            outMask &= nMask;
                        } else {
                            outMask |= mask;
                        }
                        InBus inBus = inBuses[nState];
                        if (!inBus.hiImpedance && outBus.state != (inBus.state & outMask)) {
                            outBus.state = (inBus.state & outMask);
                            outBus.setState(outBus.state);
                        }
                    }
                });
            }
        } else {
            addInPin(new NoFloatingInPin("OE", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    if (newState) {
                        outMask = -1;
                    } else {
                        outMask = 0;
                    }
                    InBus inBus = inBuses[nState];
                    if (!inBus.hiImpedance) {
                        if (!newState && outBus.state != inBus.state) {
                            outBus.state = inBus.state;
                            outBus.setState(inBus.state);
                        } else if (newState && outBus.state != 0) {
                            outBus.state = 0;
                            outBus.setState(0);
                        }
                    }
                }
            });
            for (int i = 0; i < partSize; i++) {
                long mask = 1L << i;
                long nMask = ~mask;
                addInPin(new NoFloatingInPin("OE" + (char) ('a' + i), this) {
                    @Override
                    public void setState(boolean newState) {
                        hiImpedance = false;
                        state = newState;
                        if (newState) {
                            outMask |= mask;
                        } else {
                            outMask &= nMask;
                        }
                        InBus inBus = inBuses[nState];
                        if (!inBus.hiImpedance && outBus.state != (inBus.state & outMask)) {
                            outBus.state = (inBus.state & outMask);
                            outBus.setState(outBus.state);
                        }
                    }
                });
            }
        }
        for (int inNo = 0; inNo < partSize; inNo++) {
            List<String> aliases = new ArrayList<>();
            for (int part = 0; part < partsAmount; part++) {
                aliases.add((char) ('A' + part) + "" + inNo);
            }
            int finalInNo = inNo;
            inBuses[inNo] = addInBus(new CorrectedInBus(String.valueOf(finalInNo), this, partsAmount, aliases.toArray(new String[0])) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(long newState) {
                    state = newState;
                    hiImpedance = false;
                    if (finalInNo == nState /*&& outBus.state != (newState & outMask)*/) {
                        outBus.state = newState & outMask;
                        outBus.setState(outBus.state);
                    }
                }
            });
        }
        for (int i = 0; i < nSize; i++) {
            int mask = 1 << i;
            int nMask = ~mask;
            addInPin(new NoFloatingInPin("N" + i, this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    if (newState) {
                        nState |= mask;
                    } else {
                        nState &= nMask;
                    }
                    if (!inBuses[nState].hiImpedance && outBus.state != (inBuses[nState].state & outMask)) {
                        outBus.state = inBuses[nState].state & outMask;
                        outBus.setState(outBus.state);
                    }
                }
            });
        }
        String[] aliases = new String[partsAmount];
        for (byte i = 0; i < partsAmount; i++) {
            aliases[i] = "Q" + (char) ('A' + i);
        }
        addOutBus("Q", partsAmount, aliases);
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
    }

    @Override
    public void reset() {
        outBus.state = 0;
        outBus.hiImpedance = false;
        outBus.setState(0);
    }
}
