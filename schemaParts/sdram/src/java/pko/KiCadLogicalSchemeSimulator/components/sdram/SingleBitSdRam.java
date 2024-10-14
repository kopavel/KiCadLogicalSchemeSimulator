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
    private final int module;
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
        module = (int) Math.pow(2, size);
        int ramSize = (int) Math.pow(2, size * 2);
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
                    hiPart = (int) (addrPin.state * module);
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
                    addr = (int) (hiPart + addrPin.state);
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
                    hiPart = (int) (addrPin.state * module);
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
                    addr = (int) (hiPart + addrPin.state);
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
        return "A:" + String.format("%" + (int) Math.ceil(size / 4d) + "X", addr) + "\nD:" + dIn.state;
    }

    @Override
    public void initOuts() {
        dOut = getOutPin("Dout");
    }
}
