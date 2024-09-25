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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.concurrent.ThreadLocalRandom;

public class SdRam extends SchemaPart {
    private final long[] bytes;
    private final InBus addrPin;
    private final InBus dIn;
    private final InPin we;
    private final int module;
    private final int size;
    private final int aSize;
    private Bus dOut;
    private int hiPart;
    private int addr;

    protected SdRam(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("size")) {
            throw new RuntimeException("SdRam component " + id + " need \"size\" parameter");
        }
        size = Integer.parseInt(params.get("size"));
        if (size < 2) {
            throw new RuntimeException("SdRam component " + id + " size must be larger, than 1");
        }
        if (size > 64) {
            throw new RuntimeException("SdRam component " + id + " max size is 64");
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("SdRam component " + id + " need \"aSize\" parameter");
        }
        aSize = Integer.parseInt(params.get("aSize"));
        if (aSize > 15) {
            throw new RuntimeException("SdRam component " + id + " max aSize is 15");
        }
        module = (int) Math.pow(2, aSize);
        int ramSize = (int) Math.pow(2, aSize * 2);
        bytes = new long[ramSize];
        long maskForSize = Utils.getMaskForSize(size);
        for (int i = 0; i < ramSize; i++) {
            bytes[i] = ThreadLocalRandom.current().nextLong() & maskForSize;
        }
        addrPin = addInBus("A", aSize);
        addTriStateOutBus("D", size);
        dIn = addInBus(params.containsKey("separateOut") ? "Din" : "D", size);
        if (reverse) {
            addInPin(new InPin("~{RAS}", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (!state) {
                        hiPart = (int) (addrPin.state * module);
                    }
                }
            });
            addInPin(new InPin("~{CAS}", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        if (!dOut.hiImpedance) {
                            dOut.setHiImpedance();
                        }
                    } else {
                        addr = (int) (hiPart + addrPin.state);
                        if (we.state) {
                            if (dOut.state != bytes[addr] || dOut.hiImpedance) {
                                dOut.setState(bytes[addr]);
                            }
                        } else {
                            bytes[addr] = dIn.state;
                        }
                    }
                }
            });
        } else {
            addInPin(new InPin("RAS", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        hiPart = (int) (addrPin.state * module);
                    }
                }
            });
            addInPin(new InPin("CAS", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (state) {
                        addr = (int) (hiPart + addrPin.state);
                        if (we.state) {
                            bytes[addr] = dIn.state;
                        } else {
                            if (dOut.state != bytes[addr] || dOut.hiImpedance) {
                                dOut.setState(bytes[addr]);
                            }
                        }
                    } else {
                        if (!dOut.hiImpedance) {
                            dOut.setHiImpedance();
                        }
                    }
                }
            });
        }
        we = addInPin(reverse ? "~{WE}" : "WE");
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%" + (int) Math.ceil(aSize / 4d) + "X", addr) + "\nD:" + String.format("%" + (int) Math.ceil(size / 4d) + "X", dIn.state);
    }

    @Override
    public void initOuts() {
        dOut = getOutBus("D");
    }
}
