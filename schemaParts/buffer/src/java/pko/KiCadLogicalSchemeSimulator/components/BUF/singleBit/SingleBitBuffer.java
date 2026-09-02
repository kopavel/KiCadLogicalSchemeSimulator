/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.BUF.singleBit;
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
        return (params.containsKey("latch") ? "latch" : "") + (params.containsKey("reverse") ? (params.containsKey("latch") ? ";" : "") + "reverse" : "");
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
