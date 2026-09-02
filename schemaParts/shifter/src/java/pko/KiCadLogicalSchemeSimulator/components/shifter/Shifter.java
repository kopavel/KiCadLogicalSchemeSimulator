/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
    private final int hiDsMask;
    private final int outMask;
    private final InPin rPin;
    private final InPin ciPin;
    private final boolean clearReverse;
    private final boolean inhibitReverse;
    private int latch;
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
        hiDsMask = 1 << (dSize - 1);
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
                    parallelLoad = false;
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
        return "latch" + "\n" + Integer.toBinaryString(latch);
    }

    @Override
    public void reset() {
        latch = 0;
        out.setState(0);
    }
}
