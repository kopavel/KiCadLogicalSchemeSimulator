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
