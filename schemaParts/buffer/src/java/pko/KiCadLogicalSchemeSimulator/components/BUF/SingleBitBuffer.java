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
package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class SingleBitBuffer extends SchemaPart {
    private final InPin dPin;
    private final InPin oePin;
    private Pin qPin;
    private boolean latch;

    public SingleBitBuffer(String id, String sParam) {
        super(id, sParam);
        addTriStateOutPin("Q");
        if (params.containsKey("latch")) {
            if (reverse) {
                oePin = addInPin(new InPin("OE", this) {
                    @Override
                    public void setHi() {
                        state = true;
                        if (!qPin.hiImpedance) {
                            qPin.setHiImpedance();
                        }
                    }

                    @Override
                    public void setLo() {
                        state = false;
                        if (qPin.state != latch || qPin.hiImpedance) {
                            if (latch) {
                                qPin.setHi();
                            } else {
                                qPin.setLo();
                            }
                        }
                    }
                });
                addInPin(new InPin("WR", this) {
                    @Override
                    public void setHi() {
                        state = true;
                    }

                    @Override
                    public void setLo() {
                        state = false;
                        latch = dPin.state;
                        if (!oePin.state && (qPin.state != latch || qPin.hiImpedance)) {
                            if (latch) {
                                qPin.setHi();
                            } else {
                                qPin.setLo();
                            }
                        }
                    }
                });
            } else {
                oePin = addInPin(new InPin("OE", this) {
                    @Override
                    public void setHi() {
                        state = true;
                        if (qPin.state != latch || qPin.hiImpedance) {
                            if (latch) {
                                qPin.setHi();
                            } else {
                                qPin.setLo();
                            }
                        }
                    }

                    @Override
                    public void setLo() {
                        state = false;
                        if (!qPin.hiImpedance) {
                            qPin.setHiImpedance();
                        }
                    }
                });
                addInPin(new InPin("WR", this) {
                    @Override
                    public void setHi() {
                        state = true;
                        latch = dPin.state;
                        if (oePin.state && (qPin.state != latch || qPin.hiImpedance)) {
                            if (latch) {
                                qPin.setHi();
                            } else {
                                qPin.setLo();
                            }
                        }
                    }

                    @Override
                    public void setLo() {
                        state = false;
                    }
                });
            }
            dPin = addInPin("D");
        } else if (reverse) {
            oePin = addInPin(new InPin("CS", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (!qPin.hiImpedance) {
                        qPin.setHiImpedance();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (qPin.state != dPin.state || qPin.hiImpedance) {
                        if (dPin.state) {
                            qPin.setHi();
                        } else {
                            qPin.setLo();
                        }
                    }
                }
            });
            dPin = addInPin(new InPin("D", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (!oePin.state && (!qPin.state || qPin.hiImpedance)) {
                        qPin.setHi();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (!oePin.state && (qPin.state || qPin.hiImpedance)) {
                        qPin.setLo();
                    }
                }
            });
        } else {
            oePin = addInPin(new InPin("CS", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (qPin.state != dPin.state || qPin.hiImpedance) {
                        if (dPin.state) {
                            qPin.setHi();
                        } else {
                            qPin.setLo();
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (!qPin.hiImpedance) {
                        qPin.setHiImpedance();
                    }
                }
            });
            dPin = addInPin(new InPin("D", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (oePin.state && (!qPin.state || qPin.hiImpedance)) {
                        qPin.setHi();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (oePin.state && (qPin.state || qPin.hiImpedance)) {
                        qPin.setLo();
                    }
                }
            });
        }
    }

    @Override
    public String extraState() {
        return params.containsKey("latch") ? "latch" : "";
    }

    @Override
    public void initOuts() {
        qPin = getOutPin("Q");
    }

    @Override
    public void reset() {
        latch = false;
    }
}
