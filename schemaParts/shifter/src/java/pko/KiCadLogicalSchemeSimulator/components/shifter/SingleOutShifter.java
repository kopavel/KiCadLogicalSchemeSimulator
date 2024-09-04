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
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class SingleOutShifter extends SchemaPart {
    private final InBus dBus;
    private final InPin dsPins;
    private final long hiDsMask;
    private final long latchMask;
    private final long outMask;
    private final InPin rPin;
    private final InPin ciPin;
    private final boolean clearReverse;
    private final boolean inhibitReverse;
    private long latch = 0;
    private Pin out;
    private boolean parallelLoad;
    private boolean clockEnabled = true;

    protected SingleOutShifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        if (!params.containsKey("outPin")) {
            throw new RuntimeException("Component " + id + " has no parameter \"outPin\"");
        }
        outMask = 1L << Long.parseLong(params.get("outPin"));
        dBus = addInBus("D", dSize);
        boolean plReverse = params.containsKey("plReverse");
        latchMask = Utils.getMaskForSize(dSize);
        hiDsMask = 1L << (dSize - 1);
        dsPins = addInPin("DS");
        if (plReverse) {
            addInPin(new NoFloatingInPin("PL", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    parallelLoad = !newState;
                }
            });
        } else {
            addInPin(new NoFloatingInPin("PL", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    parallelLoad = newState;
                }
            });
        }
        clearReverse = params.containsKey("clearReverse");
        if (clearReverse) {
            clockEnabled = false;
            rPin = addInPin(new NoFloatingInPin("R", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (newState) {
                        clockEnabled = ciPin.state ^ inhibitReverse;
                    } else {
                        clockEnabled = false;
                        latch = 0;
                    }
                }
            });
        } else {
            rPin = addInPin(new NoFloatingInPin("R", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (newState) {
                        clockEnabled = false;
                        latch = 0;
                    } else {
                        clockEnabled = ciPin.state ^ inhibitReverse;
                    }
                }
            });
        }
        inhibitReverse = params.containsKey("inhibitReverse");
        if (inhibitReverse) {
            clockEnabled = false;
            ciPin = addInPin(new NoFloatingInPin("CI", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (newState) {
                        clockEnabled = false;
                    } else {
                        clockEnabled = rPin.state ^ clearReverse;
                    }
                }
            });
        } else {
            ciPin = addInPin(new NoFloatingInPin("CI", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (newState) {
                        clockEnabled = rPin.state ^ clearReverse;
                    } else {
                        clockEnabled = false;
                    }
                }
            });
        }
        if (reverse) {
            addInPin(new NoFloatingInPin("CP", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (clockEnabled && !newState) {
                        if (parallelLoad) {
                            latch = dBus.state;
                        } else {
                            if (latch != 0) {
                                latch = (latch << 1) & latchMask;
                            }
                            if (dsPins.state) {
                                latch |= 1;
                            }
                        }
                        if (out.state == ((latch & outMask) == 0)) {
                            out.state = ((latch & outMask) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
            addInPin(new NoFloatingInPin("CN", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (clockEnabled && !newState) {
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
                        if (out.state == ((latch & outMask) == 0)) {
                            out.state = ((latch & outMask) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("CP", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (clockEnabled && newState) {
                        if (parallelLoad) {
                            latch = dBus.state;
                        } else {
                            if (latch != 0) {
                                latch = (latch << 1) & latchMask;
                            }
                            if (dsPins.state) {
                                latch |= 1;
                            }
                        }
                        if (out.state == ((latch & outMask) == 0)) {
                            out.state = ((latch & outMask) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
            addInPin(new NoFloatingInPin("CN", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (clockEnabled && newState) {
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
                        if (out.state == ((latch & outMask) == 0)) {
                            out.state = ((latch & outMask) != 0);
                            out.setState(out.state);
                        }
                    }
                }
            });
        }
        addOutPin("Q", false);
    }

    @Override
    public void initOuts() {
        out = getOutPin("Q");
    }

    @Override
    public String extraState() {
        return "latch" + "\n" + Long.toBinaryString(latch);
    }

    @Override
    public void reset() {
        latch = 0;
        out.state = false;
        out.hiImpedance = false;
        out.setState(false);
    }
}
