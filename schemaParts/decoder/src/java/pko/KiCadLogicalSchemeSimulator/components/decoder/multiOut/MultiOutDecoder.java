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
package pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiOutDecoder extends SchemaPart {
    private final long[] csStates;
    int partAmount;
    int outSize;
    private Pin[][] outs;

    protected MultiOutDecoder(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize = Integer.parseInt(params.get("size"));
        if (!params.containsKey("cs")) {
            throw new RuntimeException("Component " + id + " has no parameter \"cs\"");
        }
        String[] partCSs = params.get("cs").split(",");
        partAmount = partCSs.length;
        boolean[][] CSs = new boolean[partAmount][0];
        csStates = new long[partAmount];
        outSize = (int) Math.pow(2, inSize);
        for (int i = 0; i < partCSs.length; i++) {
            String[] csItems = partCSs[i].split(":");
            CSs[i] = new boolean[csItems.length];
            for (int j = 0; j < csItems.length; j++) {
                CSs[i][j] = csItems[j].equals("R");
            }
        }
        Bus aBus;
        if (reverse) {
            aBus = addInBus(new InBus("A", this, inSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    for (int i = 0; i < outs.length; i++) {
                        Pin out;
                        if (csStates[i] == 0 && (out = outs[i][(int) newState]).hiImpedance) {
                            out.setLo();
                        }
                    }
                }
            });
        } else {
            aBus = addInBus(new InBus("A", this, inSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    for (int i = 0; i < outs.length; i++) {
                        Pin[] out = outs[i];
                        if (csStates[i] == 0) {
                            for (int j = 0; j < outSize; j++) {
                                if (j != newState && out[j].hiImpedance) {
                                    out[j].setLo();
                                }
                            }
                        }
                    }
                }
            });
        }
        for (int i = 0; i < CSs.length; i++) {
            boolean[] partCS = CSs[i];
            int finalI = i;
            int fullMask = (int) Utils.getMaskForSize(partCS.length);
            for (int j = 0; j < partCS.length; j++) {
                boolean csReverse = partCS[j];
                int mask = 1 << j;
                int nMask = ~mask & fullMask;
                if (csReverse) {
                    //FixMe where is nonReverse mode
                    addInPin(new InPin("CS" + ((char) ('a' + finalI)) + j, this) {
                        @Override
                        public void setHi() {
                            state = true;
                            csStates[finalI] |= mask;
                            Pin[] out = outs[finalI];
                            for (Pin pin : out) {
                                if (!pin.hiImpedance) {
                                    pin.setHiImpedance();
                                }
                            }
                        }

                        @Override
                        public void setLo() {
                            state = false;
                            if (csStates[finalI] == mask) {
                                outs[finalI][(int) aBus.state].setLo();
                                csStates[finalI] = 0;
                            } else {
                                csStates[finalI] &= nMask;
                            }
                        }
                    });
                } else {
                    csStates[finalI] |= mask;
                    addInPin(new InPin("CS" + ((char) ('a' + finalI)) + j, this) {
                        @Override
                        public void setHi() {
                            state = true;
                            if (csStates[finalI] == mask) {
                                outs[finalI][(int) aBus.state].setLo();
                                csStates[finalI] = 0;
                            } else {
                                csStates[finalI] &= nMask;
                            }
                        }

                        @Override
                        public void setLo() {
                            state = false;
                            csStates[finalI] |= mask;
                            Pin[] out = outs[finalI];
                            for (Pin pin : out) {
                                if (!pin.hiImpedance) {
                                    pin.setHiImpedance();
                                }
                            }
                        }
                    });
                }
            }
        }
        for (int i = 0; i < partAmount; i++) {
            for (int j = 0; j < outSize; j++) {
                addTriStateOutPin("Q" + ((char) ('a' + i)) + j);
            }
        }
    }

    @Override
    public void initOuts() {
        outs = new Pin[partAmount][outSize];
        for (int i = 0; i < partAmount; i++) {
            for (int j = 0; j < outSize; j++) {
                outs[i][j] = getOutPin("Q" + ((char) ('a' + i)) + j);
                if (csStates[i] > 0) {
                    outs[i][j].hiImpedance = true;
                }
            }
        }
    }
}
