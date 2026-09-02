/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.sdram;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.util.concurrent.ThreadLocalRandom;

public class SingleBitSdRam extends SchemaPart {
    private final boolean[] mem;
    private final InBus addrPin;
    private final InPin dIn;
    private final InPin we;
    private final int size;
    private Pin dOut;
    private int hiPart;
    private int addr;

    protected SingleBitSdRam(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("size")) {
            throw new RuntimeException("Ram component need \"size\" parameter");
        }
        size = Integer.parseInt(params.get("size"));
        if (size < 1) {
            throw new RuntimeException("Component " + id + " must be positive number");
        }
        if (size > 15) {
            throw new RuntimeException("Component " + id + " max size is 15");
        }
        int ramSize = (int) Math.pow(2, size << 1);
        mem = new boolean[ramSize];
        for (int i = 0; i < ramSize; i++) {
            mem[i] = ThreadLocalRandom.current().nextBoolean();
        }
        addrPin = addInBus("A", size);
        addTriStateOutPin("Dout");
        dIn = addInPin("Din");
        if (reverse) {
            addInPin(new InPin("~{RAS}", this) {
                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    hiPart = addrPin.state << size;
                }
            });
            addInPin(new InPin("~{CAS}", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (!dOut.hiImpedance) {
                        dOut.setHiImpedance();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    addr = hiPart + addrPin.state;
                    if (we.state) {
                        if (dOut.state != mem[addr] || dOut.hiImpedance) {
                            if (mem[addr]) {
                                dOut.setHi();
                            } else {
                                dOut.setLo();
                            }
                        }
                    } else {
                        mem[addr] = dIn.state;
                    }
                }
            });
        } else {
            addInPin(new InPin("RAS", this) {
                @Override
                public void setHi() {
                    state = true;
                    hiPart = addrPin.state << size;
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
            addInPin(new InPin("CAS", this) {
                @Override
                public void setHi() {
                    state = true;
                    addr = hiPart + addrPin.state;
                    if (we.state) {
                        mem[addr] = dIn.state;
                    } else {
                        if (dOut.state != mem[addr] || dOut.hiImpedance) {
                            if (mem[addr]) {
                                dOut.setHi();
                            } else {
                                dOut.setLo();
                            }
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (!dOut.hiImpedance) {
                        dOut.setHiImpedance();
                    }
                }
            });
        }
        we = addInPin(reverse ? "~{WE}" : "WE");
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(size / 4.0d) + "X", addr) + "\nD:" + dIn.state;
    }

    @Override
    public void initOuts() {
        dOut = getOutPin("Dout");
    }
}
