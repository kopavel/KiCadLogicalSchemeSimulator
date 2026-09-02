/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.ram;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.util.concurrent.ThreadLocalRandom;

//FixMe make unittest
public class SingleBitRam extends SchemaPart {
    private final boolean[] data;
    private final int size;
    private final InPin csPin;
    private final InPin oePin;
    private final Bus aBus;
    private final InPin dIn;
    private Pin dOut;

    protected SingleBitRam(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("size")) {
            throw new RuntimeException("Ram component need \"size\" parameter");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException ignore) {
            throw new RuntimeException("Ram component " + id + " size must be positive number");
        }
        if (size < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (size > 31) {
            throw new RuntimeException("Component " + id + " max size is 31");
        }
        int ramSize = (int) Math.pow(2, size);
        data = new boolean[ramSize];
        for (int i = 0; i < ramSize; i++) {
            data[i] = ThreadLocalRandom.current().nextBoolean();
        }
        addTriStateOutPin("Dout");
        dIn = addInPin("Din");
        if (reverse) {
            aBus = addInBus(new InBus("A", this, size) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    if (!csPin.state) {
                        rOut();
                    }
                }
            });
            csPin = addInPin(new InPin("~{CS}", this) {
                @Override
                public void setHi() {
                    state = true;
                    rOut();
                }

                @Override
                public void setLo() {
                    state = false;
                    rOut();
                }
            });
            oePin = addInPin(new InPin("~{OE}", this) {
                @Override
                public void setHi() {
                    state = true;
                    rOut();
                }

                @Override
                public void setLo() {
                    state = false;
                    rOut();
                }
            });
            addInPin(new InPin("~{WE}", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (!csPin.state) {
                        data[aBus.state] = dIn.state;
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
        } else {
            aBus = addInBus(new InBus("A", this, size) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    if (!csPin.state) {
                        out();
                    }
                }
            });
            csPin = addInPin(new InPin("CS", this) {
                @Override
                public void setHi() {
                    state = true;
                    out();
                }

                @Override
                public void setLo() {
                    state = false;
                    out();
                }
            });
            oePin = addInPin(new InPin("OE", this) {
                @Override
                public void setHi() {
                    state = true;
                    out();
                }

                @Override
                public void setLo() {
                    state = false;
                    out();
                }
            });
            addInPin(new InPin("WE", this) {
                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    if (csPin.state) {
                        data[aBus.state] = dIn.state;
                    }
                }
            });
        }
    }

    @Override
    public void initOuts() {
        dOut = getOutPin("Dout");
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(size / 4.0d) + "X", aBus.state) + "\nD:" + data[aBus.state];
    }

    private void out() {
        if (oePin.state && csPin.state) {
            if (dOut.state != data[aBus.state] || dOut.hiImpedance) {
                if (data[aBus.state]) {
                    dOut.setHi();
                } else {
                    dOut.setLo();
                }
            }
        } else {
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
            }
        }
    }

    private void rOut() {
        if (oePin.state || csPin.state) {
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
            }
        } else {
            if (dOut.state != data[aBus.state] || dOut.hiImpedance) {
                if (data[aBus.state]) {
                    dOut.setHi();
                } else {
                    dOut.setLo();
                }
            }
        }
    }
}
