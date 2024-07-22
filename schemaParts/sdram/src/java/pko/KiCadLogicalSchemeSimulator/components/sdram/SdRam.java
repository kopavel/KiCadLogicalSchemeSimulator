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
import pko.KiCadLogicalSchemeSimulator.api.pins.in.EdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.FallingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.RisingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class SdRam extends SchemaPart {
    private final long[] bytes;
    private final InPin addrPin;
    private final InPin dIn;
    private final InPin we;
    private final int module;
    private final int size;
    private final int aSize;
    private TriStateOutPin dOut;
    private int hiPart;

    protected SdRam(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("size")) {
            throw new RuntimeException("Ram component need \"size\" parameter");
        }
        size = Integer.parseInt(params.get("size"));
        if (size < 1) {
            throw new RuntimeException("Component " + id + " must be positive number");
        }
        if (size > 64) {
            throw new RuntimeException("Component " + id + " max size is 64");
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("Ram component need \"size\" parameter");
        }
        aSize = Integer.parseInt(params.get("aSize"));
        if (aSize > 15) {
            throw new RuntimeException("Component " + id + " max size is 15");
        }
        module = (int) Math.pow(2, aSize);
        int ramSize = (int) Math.pow(2, aSize * 2);
        bytes = new long[ramSize];
        addrPin = addInPin("A", aSize);
        addTriStateOutPin("D", size);
        dIn = addInPin("D", size);
        if (reverse) {
            addInPin(new FallingEdgeInPin("~{RAS}", this) {
                @Override
                public void onFallingEdge() {
                    hiPart = (int) (addrPin.getState() * module);
                }
            });
            addInPin(new EdgeInPin("~{CAS}", this) {
                @Override
                public void onFallingEdge() {
                    int addr;
                    addr = (int) (hiPart + addrPin.getState());
                    if (we.state == 0) {
                        bytes[addr] = dIn.getState();
                    } else {
                        dOut.setState(bytes[addr]);
                    }
                }

                @Override
                public void onRisingEdge() {
                    dOut.setHiImpedance();
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("RAS", this) {
                @Override
                public void onRisingEdge() {
                    hiPart = (int) (addrPin.getState() * module);
                }
            });
            addInPin(new EdgeInPin("CAS", this) {
                @Override
                public void onFallingEdge() {
                    dOut.setHiImpedance();
                }

                @Override
                public void onRisingEdge() {
                    int addr;
                    addr = (int) (hiPart + addrPin.getState());
                    if (we.state > 0) {
                        bytes[addr] = dIn.getState();
                    } else {
                        dOut.setState(bytes[addr]);
                    }
                }
            });
        }
        we = addInPin(reverse ? "~{WE}" : "WE", 1);
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%" + (int) Math.ceil(aSize / 4d) + "X", hiPart + addrPin.getState()) + "\nD:" +
                String.format("%" + (int) Math.ceil(size / 4d) + "X", dIn.getState());
    }

    @Override
    public void initOuts() {
        dOut = (TriStateOutPin) getOutPin("D");
    }
}
