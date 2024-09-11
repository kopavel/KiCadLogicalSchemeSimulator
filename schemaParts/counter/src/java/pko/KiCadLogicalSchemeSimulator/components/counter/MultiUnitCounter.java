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
package pko.KiCadLogicalSchemeSimulator.components.counter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiUnitCounter extends SchemaPart {
    private final long[] countMask;
    private final Bus[] outBuses;
    private final Pin[] outPins;
    private final int[] sizes;

    protected MultiUnitCounter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("sizes")) {
            throw new RuntimeException("Component " + id + " has no parameter \"sizes\"");
        }
        String[] sSizes = params.get("sizes").split(",");
        countMask = new long[sSizes.length];
        outBuses = new Bus[sSizes.length];
        outPins = new Pin[sSizes.length];
        sizes = new int[sSizes.length];
        for (int i = 0; i < sSizes.length; i++) {
            try {
                sizes[i] = Integer.parseInt(sSizes[i]);
            } catch (NumberFormatException r) {
                throw new RuntimeException("Component " + id + " sizes part No " + i + " must be positive number");
            }
            if (sizes[i] < 1) {
                throw new RuntimeException("Component " + id + " sizes part No " + i + " must be positive number");
            }
            int finalI = i;
            if (sizes[i] == 1) {
                addOutPin("Q" + (char) ('a' + i));
                if (reverse) {
                    addInPin(new NoFloatingInPin("C" + (char) ('a' + i), this) {
                        @Override
                        public void setState(boolean newState) {
                            hiImpedance = false;
                            state = newState;
                            if (!state) {
                                outPins[finalI].state = !outPins[finalI].state;
                                outPins[finalI].setState(outPins[finalI].state);
                            }
                        }
                    });
                } else {
                    addInPin(new NoFloatingInPin("C" + (char) ('a' + i), this) {
                        @Override
                        public void setState(boolean newState) {
                            state = newState;
                            hiImpedance = false;
                            if (state) {
                                outPins[finalI].state = !outPins[finalI].state;
                                outPins[finalI].setState(outPins[finalI].state);
                            }
                        }
                    });
                }
                addInPin(new NoFloatingInPin("R" + (char) ('a' + i), this) {
                    @Override
                    public void setState(boolean newState) {
                        state = newState;
                        hiImpedance = false;
                        if (state) {
                            outPins[finalI].state = false;
                            outPins[finalI].setState(false);
                        }
                    }
                });
            } else {
                countMask[i] = Utils.getMaskForSize(sizes[i]);
                addOutBus("Q" + (char) ('a' + i), sizes[i]);
                if (reverse) {
                    addInPin(new NoFloatingInPin("C" + (char) ('a' + finalI), this) {
                        @Override
                        public void setState(boolean newState) {
                            state = newState;
                            hiImpedance = false;
                            if (!state) {
                                outBuses[finalI].state = (outBuses[finalI].state + 1) & countMask[finalI];
                                outBuses[finalI].setState(outBuses[finalI].state);
                            }
                        }
                    });
                } else {
                    addInPin(new NoFloatingInPin("C" + (char) ('a' + i), this) {
                        @Override
                        public void setState(boolean newState) {
                            state = newState;
                            hiImpedance = false;
                            if (state) {
                                outBuses[finalI].state = (outBuses[finalI].state + 1) & countMask[finalI];
                                outBuses[finalI].setState(outBuses[finalI].state);
                            }
                        }
                    });
                }
                addInPin(new NoFloatingInPin("R" + (char) ('a' + i), this) {
                    @Override
                    public void setState(boolean newState) {
                        state = newState;
                        hiImpedance = false;
                        if (state) {
                            outBuses[finalI].state = 0;
                            outBuses[finalI].setState(0);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] == 1) {
                outPins[i] = getOutPin("Q" + (char) +('a' + i));
            } else {
                outBuses[i] = getOutBus("Q" + (char) +('a' + i));
                outBuses[i].useBitPresentation = true;
            }
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] == 1) {
                outPins[i].hiImpedance = false;
                outPins[i].state = false;
                outPins[i].setState(false);
            } else {
                outBuses[i].hiImpedance = false;
                outBuses[i].state = 0;
                outBuses[i].setState(0);
            }
        }
    }
}
