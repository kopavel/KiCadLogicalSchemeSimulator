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
import pko.KiCadLogicalSchemeSimulator.api.FloatingInException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;
import pko.KiCadLogicalSchemeSimulator.model.Model;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class Decoder extends SchemaPart {
    private Bus outBus;
    private boolean csState;
    private long outState;

    protected Decoder(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize = Integer.parseInt(params.get("size"));
        CorrectedInBus aBus;
        if (params.containsKey("outReverse")) {
            aBus = addInBus(new CorrectedInBus("A", this, inSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    outState = ~(1L << newState);
                    if (csState && (outBus.state != outState || outBus.hiImpedance)) {
                        outBus.state = outState;
                        outBus.setState(outState);
                        outBus.hiImpedance = false;
                    }
                    hiImpedance = false;
                }

                @Override
                public void setHiImpedance() {
                    assert !hiImpedance : "Already in hiImpedance:" + this;
                    hiImpedance = true;
                    if (csState) {
                        if (Model.stabilizing) {
                            Model.forResend.add(this);
                            Log.warn(this.getClass(), "Floating pin {}, try resend later", this);
                        } else {
                            throw new FloatingInException(this);
                        }
                    }
                }
            });
        } else {
            aBus = addInBus(new CorrectedInBus("A", this, inSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    outState = 1L << state;
                    if (csState) {
                        if (outBus.state != outState || outBus.hiImpedance) {
                            outBus.state = outState;
                            outBus.setState(outState);
                            outBus.hiImpedance = false;
                        }
                    }
                    hiImpedance = false;
                }

                @Override
                public void setHiImpedance() {
                    assert !hiImpedance : "Already in hiImpedance:" + this;
                    hiImpedance = true;
                    if (csState) {
                        if (Model.stabilizing) {
                            Model.forResend.add(this);
                            Log.warn(this.getClass(), "Floating pin {}, try resend later", this);
                        } else {
                            throw new FloatingInException(this);
                        }
                    }
                }
            });
        }
        if (reverse) {
            addInPin(new NoFloatingInPin("CS", this) {
                @Override
                public void setState(boolean newState, boolean strong) {
                    state = newState;
                    csState = !newState;
                    if (csState) {
                        if (aBus.hiImpedance) {
                            if (Model.stabilizing) {
                                Model.forResend.add(this);
                                Log.warn(this.getClass(), "Floating pin {}, try resend later", this);
                            } else {
                                throw new FloatingInException(aBus);
                            }
                        } else if (outBus.state != outState || outBus.hiImpedance) {
                            outBus.state = outState;
                            outBus.setState(outState);
                            outBus.hiImpedance = false;
                        }
                    } else if (!outBus.hiImpedance) {
                        outBus.setHiImpedance();
                        outBus.hiImpedance = true;
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("CS", this) {
                @Override
                public void setState(boolean newState, boolean strong) {
                    state = newState;
                    csState = newState;
                    if (csState) {
                        if (aBus.hiImpedance) {
                            if (Model.stabilizing) {
                                Model.forResend.add(this);
                                Log.warn(this.getClass(), "Floating pin {}, try resend later", this);
                            } else {
                                throw new FloatingInException(aBus);
                            }
                        } else if (outBus.state != outState || outBus.hiImpedance) {
                            outBus.state = outState;
                            outBus.setState(outState);
                            outBus.hiImpedance = false;
                        }
                    } else if (!outBus.hiImpedance) {
                        outBus.setHiImpedance();
                        outBus.hiImpedance = true;
                    }
                }
            });
        }
        int outSize = (int) Math.pow(2, inSize);
        addOutBus("Q", outSize);
        csState = reverse;
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        outBus.useBitPresentation = true;
    }
}
