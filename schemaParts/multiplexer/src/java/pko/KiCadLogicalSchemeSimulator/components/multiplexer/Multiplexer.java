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
import pko.KiCadLogicalSchemeSimulator.api.FloatingInException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.util.ArrayList;
import java.util.List;

public class Multiplexer extends SchemaPart {
    private final InBus[] inBuses;
    private int nState;
    private Bus outBus;

    protected Multiplexer(String id, String sParam) {
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
        for (int inNo = 0; inNo < partSize; inNo++) {
            List<String> aliases = new ArrayList<>();
            for (int part = 0; part < partsAmount; part++) {
                aliases.add(String.valueOf((char) (((byte) 'A') + part)) + inNo);
            }
            int finalInNo = inNo;
            inBuses[inNo] = addInBus(new CorrectedInBus(String.valueOf(finalInNo), this, partsAmount, aliases.toArray(new String[0])) {
                private final int number = finalInNo;

                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                }

                @Override
                public void setState(long newState) {
                    state = newState;
                    hiImpedance = false;
                    if (number == nState && outBus.state != newState) {
                        outBus.state = newState;
                        outBus.setState(newState);
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
                    state = newState;
                    if (newState) {
                        nState |= mask;
                    } else {
                        nState &= nMask;
                    }
                    InBus inBus = inBuses[nState];
                    if (inBus.hiImpedance) {
                        if (Net.stabilizing) {
                            Net.forResend.add(this);
                            assert Log.debug(this.getClass(), "Floating pin {}, try resend later", this);
                        } else {
                            throw new FloatingInException(inBus);
                        }
                    } else if (outBus.state != inBus.state) {
                        outBus.state = inBus.state;
                        outBus.setState(inBus.state);
                    }
                }
            });
        }
        String[] aliases = new String[partsAmount];
        for (byte i = 0; i < partsAmount; i++) {
            aliases[i] = 'Q' + String.valueOf((char) (((byte) 'A') + i));
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
