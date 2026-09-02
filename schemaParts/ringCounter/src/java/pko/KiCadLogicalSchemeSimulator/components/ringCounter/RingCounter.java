/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.ringCounter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class RingCounter extends SchemaPart {
    public final long coMax;
    public final long countMax;
    private Bus outBus;
    private Pin carryOutPin;
    private boolean clockEnabled = true;

    protected RingCounter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int pinAmount;
        try {
            pinAmount = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must positive >=2 number");
        }
        if (pinAmount < 2) {
            throw new RuntimeException("Component " + id + " size must positive >=2 number");
        }
        countMax = (long) Math.pow(2, pinAmount);
        coMax = (long) Math.pow(2, ((double) pinAmount / 2));
        addOutBus("Q", pinAmount);
        addOutPin("CO");
        addInPin(new InPin("CI", this) {
            @Override
            public void setHi() {
                state = true;
                clockEnabled = false;
            }

            @Override
            public void setLo() {
                state = false;
                clockEnabled = true;
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
                        if (outBus.state >= countMax) {
                            outBus.state = 1;
                            if (carryOutPin.state) {
                                carryOutPin.setLo();
                            }
                        } else {
                            outBus.state = outBus.state << 1;
                            if (carryOutPin.state != (outBus.state < coMax)) {
                                if (outBus.state < coMax) {
                                    carryOutPin.setHi();
                                } else {
                                    carryOutPin.setLo();
                                }
                            }
                        }
                        outBus.setState(outBus.state);
                    }
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    clockEnabled = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    clockEnabled = false;
                    if (outBus.state != 1) {
                        outBus.setState(1);
                    }
                }
            });
        } else {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (clockEnabled) {
                        if (outBus.state >= countMax) {
                            outBus.state = 1;
                            if (carryOutPin.state) {
                                carryOutPin.setLo();
                            }
                        } else {
                            outBus.state = outBus.state << 1;
                            if (carryOutPin.state != outBus.state < coMax) {
                                if (outBus.state < coMax) {
                                    carryOutPin.setHi();
                                } else {
                                    carryOutPin.setLo();
                                }
                            }
                        }
                        outBus.setState(outBus.state);
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
            addInPin(new InPin("R", this) {
                @Override
                public void setHi() {
                    state = true;
                    clockEnabled = false;
                    if (outBus.state != 1) {
                        outBus.setState(1);
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    clockEnabled = true;
                }
            });
        }
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        outBus.useBitPresentation = true;
        carryOutPin = getOutPin("CO");
    }

    @Override
    public void reset() {
        outBus.setState(1);
        carryOutPin.setHi();
    }
}
