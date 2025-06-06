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
import pko.KiCadLogicalSchemeSimulator.tools.MemoryDumpPanel;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import javax.swing.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class SdRam extends SchemaPart {
    private final int[] bytes;
    private final InBus addrPin;
    private final InBus dIn;
    private final InPin we;
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
        if (size > 32) {
            throw new RuntimeException("SdRam component " + id + " max size is 32");
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("SdRam component " + id + " need \"aSize\" parameter");
        }
        aSize = Integer.parseInt(params.get("aSize"));
        if (aSize > 15) {
            throw new RuntimeException("SdRam component " + id + " max aSize is 15");
        }
        int ramSize = (int) Math.pow(2, aSize << 1);
        bytes = new int[ramSize];
        int maskForSize = Utils.getMaskForSize(size);
        for (int i = 0; i < ramSize; i++) {
            bytes[i] = ThreadLocalRandom.current().nextInt() & maskForSize;
        }
        addrPin = addInBus("A", aSize);
        addTriStateOutBus("D", size);
        we = addInPin(reverse ? "~{WE}" : "WE");
        dIn = addInBus(params.containsKey("separateOut") ? "Din" : "D", size);
        if (reverse) {
            addInPin(new InPin("~{RAS}", this) {
                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    hiPart = addrPin.state << aSize;
                }
            });
            addInPin(new InPin("~{CAS}", this) {
                final InBus in = dIn;
                final InBus aIn = addrPin;
                final int[] bts = bytes;
                final InPin w = we;

                @Override
                public void setHi() {
                    state = true;
                    Bus out;
                    if (!(out = dOut).hiImpedance) {
                        out.setHiImpedance();
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    int a = (addr = hiPart + aIn.state);
                    if (w.state) {
                        Bus out;
                        int i;
                        if ((out = dOut).state != (i = bts[a]) || out.hiImpedance) {
                            out.setState(i);
                        }
                    } else {
                        bts[a] = in.state;
                    }
                }
            });
        } else {
            addInPin(new InPin("RAS", this) {
                @Override
                public void setHi() {
                    state = true;
                    hiPart = addrPin.state << aSize;
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
                        bytes[addr] = dIn.state;
                    } else {
                        Bus out;
                        int i;
                        if ((out = dOut).state != (i = bytes[addr]) || out.hiImpedance) {
                            out.setState(i);
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    Bus out;
                    if (!(out = dOut).hiImpedance) {
                        out.setHiImpedance();
                    }
                }
            });
        }
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%" + (int) Math.ceil(aSize / 4.0d) + "X", addr) + "\nD:" + String.format("%" + (int) Math.ceil(size / 4.0d) + "X", dIn.state);
    }

    @Override
    public Supplier<JPanel> extraPanel() {
        return () -> new MemoryDumpPanel(bytes);
    }

    @Override
    public void initOuts() {
        dOut = getOutBus("D");
    }
}
