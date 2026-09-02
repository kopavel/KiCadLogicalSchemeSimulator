/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.jkTrigger;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

//FixME in case of DcTrigger chain on single clock - need C priority in reverse chain order.
public class JkTrigger extends SchemaPart {
    private final InPin jPin;
    private final InPin kPin;
    private final InPin rPin;
    private final InPin sPin;
    private Pin qOut;
    private Pin iqOut;
    private boolean clockEnabled = true;

    public JkTrigger(String id, String sParam) {
        super(id, sParam);
        jPin = addInPin("J");
        kPin = addInPin("K");
        rPin = addInPin(new InPin("R", this) {
            @Override
            public void setHi() {
                state = true;
                clockEnabled = false;
                if (!iqOut.state) {
                    iqOut.setHi();
                }
                if (qOut.state != sPin.state) {
                    if (sPin.state) {
                        qOut.setHi();
                    } else {
                        qOut.setLo();
                    }
                }
            }

            @Override
            public void setLo() {
                state = false;
                clockEnabled = !sPin.state;
                if (sPin.state) {
                    if (!qOut.state) {
                        qOut.setHi();
                    }
                    if (iqOut.state) {
                        iqOut.setLo();
                    }
                }
            }
        });
        sPin = addInPin(new InPin("S", this) {
            @Override
            public void setHi() {
                state = true;
                clockEnabled = false;
                if (!qOut.state) {
                    qOut.setHi();
                }
                if (iqOut.state != rPin.state) {
                    if (rPin.state) {
                        iqOut.setHi();
                    } else {
                        iqOut.setLo();
                    }
                }
            }

            @Override
            public void setLo() {
                state = false;
                clockEnabled = !rPin.state;
                if (rPin.state) {
                    if (qOut.state) {
                        qOut.setLo();
                    }
                    if (!iqOut.state) {
                        iqOut.setHi();
                    }
                }
            }
        });
        if (reverse) {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    if (clockEnabled) {
                        if (jPin.state && kPin.state) {
                            if (qOut.state) {
                                qOut.setLo();
                                iqOut.setHi();
                            } else {
                                qOut.setHi();
                                iqOut.setLo();
                            }
                        } else if (jPin.state) {
                            if (!qOut.state) {
                                qOut.setHi();
                            }
                            if (iqOut.state) {
                                iqOut.setLo();
                            }
                        } else if (kPin.state) {
                            if (qOut.state) {
                                qOut.setLo();
                            }
                            if (!iqOut.state) {
                                iqOut.setHi();
                            }
                        }
                    }
                }
            });
        } else {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (clockEnabled) {
                        if (jPin.state && kPin.state) {
                            if (qOut.state) {
                                qOut.setLo();
                                iqOut.setHi();
                            } else {
                                qOut.setHi();
                                iqOut.setLo();
                            }
                        } else if (jPin.state) {
                            if (!qOut.state) {
                                qOut.setHi();
                            }
                            if (iqOut.state) {
                                iqOut.setLo();
                            }
                        } else if (kPin.state) {
                            if (qOut.state) {
                                qOut.setLo();
                            }
                            if (!iqOut.state) {
                                iqOut.setHi();
                            }
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
        }
        addOutPin("Q");
        addOutPin("~{Q}");
    }

    @Override
    public void initOuts() {
        qOut = getOutPin("Q");
        iqOut = getOutPin("~{Q}");
        qOut.setLo();
        iqOut.setHi();
    }

    @Override
    public void reset() {
        qOut.setLo();
        iqOut.setHi();
    }
}
