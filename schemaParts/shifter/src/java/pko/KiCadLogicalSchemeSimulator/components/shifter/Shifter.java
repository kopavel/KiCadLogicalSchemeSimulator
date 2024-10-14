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
package pko.KiCadLogicalSchemeSimulator.components.shifter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class Shifter extends SchemaPart {
    private final InBus dBus;
    private final InPin dsPins;
    private final long hiDsMask;
    private final long outMask;
    private final InPin rPin;
    private final InPin ciPin;
    private final boolean clearReverse;
    private final boolean inhibitReverse;
    private long latch = 0;
    private Bus out;
    private boolean parallelLoad;
    private boolean clockEnabled = true;

    protected Shifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        int qSize = dSize;
        if (params.containsKey("qSize")) {
            qSize = Integer.parseInt(params.get("qSize"));
        }
        dBus = addInBus("D", dSize);
        outMask = Utils.getMaskForSize(dSize);
        hiDsMask = 1L << (dSize - 1);
        dsPins = addInPin("DS");
        if (params.containsKey("plReverse")) {
            addInPin(new InPin("PL", this) {
                @Override
                public void setHi() {
                    state = true;
                    parallelLoad = false;
                }

                @Override
                public void setLo() {
                    state = false;
                    parallelLoad = true;
                }
            });
        } else {
            addInPin(new InPin("PL", this) {
                @Override
                public void setHi() {
                    state = true;
                    parallelLoad = true;
                }

                @Override
                public void setLo() {
                    state = true;
                    parallelLoad = true;
                }
            });
        }
        clearReverse = params.containsKey("clearReverse");
        if (clearReverse) {
            clockEnabled = false;
            rPin = addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    clockEnabled = ciPin.state ^ inhibitReverse;
                }

                @Override
                public void setLo() {
                    state = false;
                    clockEnabled = false;
                    latch = 0;
                }
            });
        } else {
            rPin = addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    clockEnabled = ciPin.state ^ inhibitReverse;
                }

                @Override
                public void setLo() {
                    state = false;
                    clockEnabled = false;
                    latch = 0;
                }
            });
        }
        inhibitReverse = params.containsKey("inhibitReverse");
        if (inhibitReverse) {
            clockEnabled = false;
            ciPin = addInPin(new InPin("CI", this) {
                @Override
                public void setHi() {
                    state = true;
                    clockEnabled = false;
                }

                @Override
                public void setLo() {
                    state = false;
                    clockEnabled = rPin.state ^ clearReverse;
                }
            });
        } else {
            ciPin = addInPin(new InPin("CI", this) {
                @Override
                public void setHi() {
                    state = true;
                    clockEnabled = rPin.state ^ clearReverse;
                }

                @Override
                public void setLo() {
                    state = false;
                    clockEnabled = false;
                }
            });
        }
        if (reverse) {
            addInPin(new InPin("CP", this) {
                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    if (clockEnabled) {
                        if (parallelLoad) {
                            latch = dBus.state;
                        } else {
                            if (latch != 0) {
                                latch = (latch << 1) & outMask;
                            }
                            if (dsPins.state) {
                                latch |= 1;
                            }
                        }
                        if (out.state != latch) {
                            out.setState(latch);
                        }
                    }
                }
            });
            addInPin(new InPin("CN", this) {
                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    if (clockEnabled) {
                        if (parallelLoad) {
                            latch = dBus.state;
                        } else {
                            if (latch != 0) {
                                latch = latch >> 1;
                            }
                            if (dsPins.state) {
                                latch |= hiDsMask;
                            }
                        }
                        if (out.state != latch) {
                            out.setState(latch);
                        }
                    }
                }
            });
        } else {
            addInPin(new InPin("CP", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (clockEnabled) {
                        if (parallelLoad) {
                            latch = dBus.state;
                        } else {
                            if (latch != 0) {
                                latch = (latch << 1) & outMask;
                            }
                            if (dsPins.state) {
                                latch |= 1;
                            }
                        }
                        if (out.state != latch) {
                            out.setState(latch);
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
            addInPin(new InPin("CN", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (clockEnabled) {
                        if (parallelLoad) {
                            latch = dBus.state;
                        } else {
                            if (latch != 0) {
                                latch = latch >> 1;
                            }
                            if (dsPins.state) {
                                latch |= hiDsMask;
                            }
                        }
                        if (out.state != latch) {
                            out.setState(latch);
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
        }
        addOutBus("Q", qSize);
    }

    @Override
    public void initOuts() {
        out = getOutBus("Q");
    }

    @Override
    public String extraState() {
        return "latch" + "\n" + Long.toBinaryString(latch);
    }

    @Override
    public void reset() {
        latch = 0;
        out.setState(0);
    }
}
