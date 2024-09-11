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
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;

//FixMe make unittest
public class MultiUnitDcTrigger extends SchemaPart {
    private final InPin[] dPin;
    private final InPin rPin;
    private final InPin sPin;
    private final Pin[] qOut;
    private final Pin[] iqOut;
    private final int size;
    private boolean clockEnabled = true;

    protected MultiUnitDcTrigger(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " has no parameter \"size\"");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " size must be >1");
        }
        if (size < 2) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " size must be >1");
        }
        dPin = new InPin[size];
        qOut = new Pin[size];
        iqOut = new Pin[size];
        if (params.containsKey("setReverse")) {
            rPin = addInPin(new NoFloatingInPin("R", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    clockEnabled = newState && sPin.state;
                    if (newState) {
                        if (!sPin.state) {
                            for (int i = 0; i < size; i++) {
                                if (iqOut[i].state) {
                                    iqOut[i].state = false;
                                    iqOut[i].setState(false);
                                }
                                if (!qOut[i].state) {
                                    qOut[i].state = true;
                                    qOut[i].setState(true);
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < size; i++) {
                            if (!iqOut[i].state) {
                                iqOut[i].state = true;
                                iqOut[i].setState(true);
                            }
                            if (qOut[i].state == sPin.state) {
                                qOut[i].state = !sPin.state;
                                qOut[i].setState(!sPin.state);
                            }
                        }
                    }
                }
            });
            sPin = addInPin(new NoFloatingInPin("S", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    clockEnabled = newState && rPin.state;
                    if (newState) {
                        if (!rPin.state) {
                            for (int i = 0; i < size; i++) {
                                if (qOut[i].state) {
                                    qOut[i].state = false;
                                    qOut[i].setState(false);
                                }
                                if (!iqOut[i].state) {
                                    iqOut[i].state = true;
                                    iqOut[i].setState(true);
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < size; i++) {
                            if (!qOut[i].state) {
                                qOut[i].state = true;
                                qOut[i].setState(true);
                            }
                            if (iqOut[i].state == rPin.state) {
                                iqOut[i].state = !rPin.state;
                                iqOut[i].setState(!rPin.state);
                            }
                        }
                    }
                }
            });
        } else {
            rPin = addInPin(new NoFloatingInPin("R", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    clockEnabled = !newState && !sPin.state;
                    if (newState) {
                        for (int i = 0; i < size; i++) {
                            if (!iqOut[i].state) {
                                iqOut[i].state = true;
                                iqOut[i].setState(true);
                            }
                            if (qOut[i].state != sPin.state) {
                                qOut[i].state = sPin.state;
                                qOut[i].setState(sPin.state);
                            }
                        }
                    } else if (sPin.state) {
                        for (int i = 0; i < size; i++) {
                            if (iqOut[i].state) {
                                iqOut[i].state = false;
                                iqOut[i].setState(false);
                            }
                            if (!qOut[i].state) {
                                qOut[i].state = true;
                                qOut[i].setState(true);
                            }
                        }
                    }
                }
            });
            sPin = addInPin(new NoFloatingInPin("S", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    clockEnabled = !newState && !rPin.state;
                    if (newState) {
                        for (int i = 0; i < size; i++) {
                            if (!qOut[i].state) {
                                qOut[i].state = true;
                                qOut[i].setState(true);
                            }
                            if (iqOut[i].state != rPin.state) {
                                iqOut[i].state = rPin.state;
                                iqOut[i].setState(rPin.state);
                            }
                        }
                    } else if (rPin.state) {
                        for (int i = 0; i < size; i++) {
                            if (qOut[i].state) {
                                qOut[i].state = false;
                                qOut[i].setState(false);
                            }
                            if (!iqOut[i].state) {
                                iqOut[i].state = true;
                                iqOut[i].setState(true);
                            }
                        }
                    }
                }
            });
        }
        if (reverse) {
            addInPin(new NoFloatingInPin("C", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    if (!newState && clockEnabled) {
                        for (int i = 0; i < size; i++) {
                            if (dPin[i].state) {
                                if (!qOut[i].state) {
                                    qOut[i].state = true;
                                    qOut[i].setState(true);
                                }
                                if (iqOut[i].state) {
                                    iqOut[i].state = false;
                                    iqOut[i].setState(false);
                                }
                            } else {
                                if (qOut[i].state) {
                                    qOut[i].state = false;
                                    qOut[i].setState(false);
                                }
                                if (!iqOut[i].state) {
                                    iqOut[i].state = true;
                                    iqOut[i].setState(true);
                                }
                            }
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("C", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    if (newState && clockEnabled) {
                        for (int i = 0; i < size; i++) {
                            if (dPin[i].state) {
                                if (!qOut[i].state) {
                                    qOut[i].state = true;
                                    qOut[i].setState(true);
                                }
                                if (iqOut[i].state) {
                                    iqOut[i].state = false;
                                    iqOut[i].setState(false);
                                }
                            } else {
                                if (qOut[i].state) {
                                    qOut[i].state = false;
                                    qOut[i].setState(false);
                                }
                                if (!iqOut[i].state) {
                                    iqOut[i].state = true;
                                    iqOut[i].setState(true);
                                }
                            }
                        }
                    }
                }
            });
        }
        for (int i = 0; i < size; i++) {
            dPin[i] = addInPin("D" + (char) ('a' + i));
            addOutPin("Q" + (char) ('a' + i), true);
            addOutPin("~{Q" + (char) ('a' + i) + "}", false);
        }
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < size; i++) {
            qOut[i] = getOutPin("Q" + (char) ('a' + i));
            iqOut[i] = getOutPin("~{Q" + (char) ('a' + i) + "}");
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < size; i++) {
            qOut[i].hiImpedance = false;
            qOut[i].state = false;
            qOut[i].setState(false);
            iqOut[i].state = true;
            iqOut[i].hiImpedance = false;
            iqOut[i].setState(true);
        }
    }
}