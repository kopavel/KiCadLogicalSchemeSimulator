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
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;

public class SingleBitMultiplexer extends SchemaPart {
    private final InPin[] inPins;
    private int nState;
    private Pin outPin;

    protected SingleBitMultiplexer(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int size = Integer.parseInt(params.get("size"));
        if (size > 30) {
            throw new RuntimeException("Component " + id + " max nSize is 30");
        }
        int partSize = (int) Math.pow(2, size);
        inPins = new InPin[partSize];
        for (int inNo = 0; inNo < partSize; inNo++) {
            int finalInNo = inNo;
            inPins[inNo] = addInPin(new NoFloatingInPin(String.valueOf(finalInNo), this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    hiImpedance = false;
                    if (finalInNo == nState /*&& outBus.state != newState*/) {
                        outPin.hiImpedance = false;
                        outPin.state = newState;
                        outPin.setState(newState);
                    }
                }
            });
        }
        for (int i = 0; i < size; i++) {
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
                    if (!inPins[nState].hiImpedance && outPin.state != inPins[nState].state) {
                        outPin.state = inPins[nState].state;
                        outPin.hiImpedance = false;
                        outPin.setState(outPin.state);
                    }
                }
            });
        }
        addOutPin("Q");
    }

    @Override
    public void initOuts() {
        outPin = getOutPin("Q");
    }
}
