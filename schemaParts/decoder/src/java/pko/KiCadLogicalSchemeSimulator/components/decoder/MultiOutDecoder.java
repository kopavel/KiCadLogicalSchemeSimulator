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
package pko.KiCadLogicalSchemeSimulator.components.decoder;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiOutDecoder extends SchemaPart {
    private final long[] csStates;
    int partAmount;
    private Bus[] outBuses;
    private long outState;

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
        int outSize = (int) Math.pow(2, inSize);
        for (int i = 0; i < partCSs.length; i++) {
            String[] csItems = partCSs[i].split(":");
            CSs[i] = new boolean[csItems.length];
            for (int j = 0; j < csItems.length; j++) {
                CSs[i][j] = csItems[j].equals("R");
            }
        }
        long outMask = Utils.getMaskForSize(outSize);
        if (reverse) {
            addInBus(new InBus("A", this, inSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    outState = (1L << newState) ^ outMask;
                    for (int i = 0; i < outBuses.length; i++) {
                        if (csStates[i] == 0 && (outBuses[i].state != outState || outBuses[i].hiImpedance)) {
                            outBuses[i].setState(outState);
                        }
                    }
                }
            });
        } else {
            addInBus(new InBus("A", this, inSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    outState = 1L << state;
                    for (int i = 0; i < outBuses.length; i++) {
                        if (csStates[i] == 0 && (outBuses[i].state != outState || outBuses[i].hiImpedance)) {
                            outBuses[i].setState(outState);
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
                    addInPin(new InPin("CS" + (char) ('a' + finalI) + j, this) {
                        @Override
                        public void setHi() {
                            state = true;
                            if (!outBuses[finalI].hiImpedance) {
                                outBuses[finalI].setHiImpedance();
                                csStates[finalI] = mask;
                            } else {
                                csStates[finalI] |= mask;
                            }
                        }

                        @Override
                        public void setLo() {
                            state = false;
                            if (csStates[finalI] == nMask) {
                                outBuses[finalI].setState(outState);
                                csStates[finalI] = 0;
                            } else {
                                csStates[finalI] &= nMask;
                            }
                        }
                    });
                } else {
                    csStates[finalI] |= mask;
                    addInPin(new InPin("CS" + (char) ('a' + finalI) + j, this) {
                        @Override
                        public void setHi() {
                            state = true;
                            if (csStates[finalI] == nMask) {
                                outBuses[finalI].setState(outState);
                                csStates[finalI] = 0;
                            } else {
                                csStates[finalI] &= nMask;
                            }
                        }

                        @Override
                        public void setLo() {
                            state = false;
                            if (!outBuses[finalI].hiImpedance) {
                                outBuses[finalI].setHiImpedance();
                                csStates[finalI] = mask;
                            } else {
                                csStates[finalI] |= mask;
                            }
                        }
                    });
                }
            }
        }
        for (int i = 0; i < partAmount; i++) {
            addTriStateOutBus("Q" + (char) ('a' + i), outSize);
        }
    }

    @Override
    public void initOuts() {
        outBuses = new Bus[partAmount];
        for (int i = 0; i < partAmount; i++) {
            outBuses[i] = getOutBus("Q" + (char) ('a' + i));
            outBuses[i].useBitPresentation = true;
        }
    }
}
