/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.dCounter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class DCounter extends SchemaPart {
    private final InBus jBus;
    private final boolean carryHi;
    private final boolean carryLo;
    private final InPin udPin;
    public int maxCount;
    private Bus outBus;
    private Pin cOutPin;
    private boolean ciState;
    private boolean cState = true;
    private boolean eState;
    private boolean presetDisabled = true;
    private boolean resetInactive = true;

    protected DCounter(String id, String sParam) {
        super(id, sParam);
        boolean eReverse = params.containsKey("eReverse");
        addOutBus("Q", 4);
        addOutPin("CO");
        if (params.containsKey("carryReverse")) {
            addInPin(new InPin("CI", this) {
                @Override
                public void setHi() {
                    state = true;
                    ciState = false;
                    setCOut(udPin.state);
                }

                @Override
                public void setLo() {
                    state = false;
                    ciState = true;
                    setCOut(udPin.state);
                }
            });
            ciState = true;
            carryHi = false;
            carryLo = true;
            udPin = addInPin(new InPin("UD", this, true) {
                @Override
                public void setHi() {
                    state = true;
                    if (outBus.state == maxCount) {
                        if (cOutPin.state) {
                            cOutPin.setLo();
                        }
                    } else if (!cOutPin.state) {
                        cOutPin.setHi();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (outBus.state == 0) {
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    } else if (cOutPin.state) {
                        cOutPin.setLo();
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    resetInactive = false;
                    if (outBus.state != 0) {
                        outBus.setState(0);
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    resetInactive = true;
                }
            });
        } else {
            addInPin(new InPin("CI", this) {
                @Override
                public void setHi() {
                    state = true;
                    ciState = true;
                    setCOut(udPin.state);
                }

                @Override
                public void setLo() {
                    state = false;
                    ciState = false;
                    setCOut(udPin.state);
                }
            });
            carryHi = true;
            carryLo = false;
            udPin = addInPin(new InPin("UD", this, true) {
                @Override
                public void setHi() {
                    state = true;
                    if (outBus.state == maxCount) {
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    } else if (cOutPin.state) {
                        cOutPin.setLo();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (outBus.state == 0) {
                        if (!cOutPin.state) {
                            cOutPin.setHi();
                        }
                    } else if (cOutPin.state) {
                        cOutPin.setLo();
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    resetInactive = false;
                    if (outBus.state != 0) {
                        outBus.setState(0);
                        if (cOutPin.state) {
                            cOutPin.setLo();
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    resetInactive = true;
                }
            });
        }
        jBus = addInBus(new InBus("J", this, 4) {
            @Override
            public void setState(int newState) {
                state = newState;
                if (!presetDisabled && resetInactive && outBus.state != newState) {
                    outBus.setState(newState);
                }
            }
        });
        addInPin(new InPin("PE", this) {
            @Override
            public void setHi() {
                state = true;
                presetDisabled = false;
                if (resetInactive && outBus.state != jBus.state) {
                    outBus.setState(jBus.state);
                }
            }

            @Override
            public void setLo() {
                state = false;
                presetDisabled = true;
            }
        });
        if (params.containsKey("bdReverse")) {
            addInPin(new InPin("BD", this) {
                @Override
                public void setHi() {
                    state = true;
                    maxCount = 9;
                }

                @Override
                public void setLo() {
                    state = false;
                    maxCount = 15;
                }
            });
            maxCount = 15;
        } else {
            addInPin(new InPin("BD", this) {
                @Override
                public void setHi() {
                    state = true;
                    maxCount = 15;
                }

                @Override
                public void setLo() {
                    state = false;
                    maxCount = 9;
                }
            });
            maxCount = 9;
        }
        if (reverse) {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                    cState = false;
                }

                @Override
                public void setLo() {
                    state = false;
                    cState = true;
                    if (eState) {
                        process();
                    }
                }
            });
        } else {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                    cState = true;
                    if (eState) {
                        process();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    cState = false;
                }
            });
        }
        if (eReverse) {
            addInPin(new InPin("E", this) {
                @Override
                public void setHi() {
                    state = true;
                    eState = false;
                }

                @Override
                public void setLo() {
                    state = false;
                    eState = true;
                    if (cState) {
                        process();
                    }
                }
            });
        } else {
            addInPin(new InPin("E", this) {
                @Override
                public void setHi() {
                    state = true;
                    eState = true;
                    if (cState) {
                        process();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    eState = false;
                }
            });
        }
        eState = eReverse;
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        outBus.useBitPresentation = true;
        cOutPin = getOutPin("CO");
    }

    @Override
    public void reset() {
        outBus.setState(0);
        if (carryLo) {
            cOutPin.setHi();
        } else {
            cOutPin.setLo();
        }
    }

    private void process() {
        if (ciState && presetDisabled && resetInactive) {
            if (udPin.state) {
                if (outBus.state == maxCount) {
                    outBus.setState(0);
                } else {
                    outBus.setState(outBus.state + 1);
                }
                setCOut(true);
            } else {
                if (outBus.state == 0) {
                    outBus.setState(maxCount);
                } else {
                    outBus.setState(outBus.state - 1);
                }
                setCOut(false);
            }
        }
    }

    private void setCOut(boolean udState) {
        if (ciState && outBus.state == (udState ? maxCount : 0)) {
            if (carryHi) {
                cOutPin.setHi();
            } else {
                cOutPin.setLo();
            }
        } else {
            if (carryLo) {
                cOutPin.setHi();
            } else {
                cOutPin.setLo();
            }
        }
    }
}
