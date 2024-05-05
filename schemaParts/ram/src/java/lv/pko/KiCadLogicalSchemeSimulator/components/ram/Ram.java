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
package lv.pko.KiCadLogicalSchemeSimulator.components.ram;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FallingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class Ram extends SchemaPart {
    private final long[] words;
    private final InPin dIn;
    private final int size;
    private final int aSize;
    private TriStateOutPin dOut;
    private int addr;
    private boolean csActive;
    private boolean oeActive;

    protected Ram(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("size")) {
            throw new RuntimeException("Ram component need \"size\" parameter");
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("Ram component need \"dSize\" parameter");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException ignore) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        try {
            aSize = Integer.parseInt(params.get("aSize"));
        } catch (NumberFormatException ignore) {
            throw new RuntimeException("Component " + id + " aSize must be positive number");
        }
        if (size < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (size > 64) {
            throw new RuntimeException("Component " + id + " max size is 64");
        }
        if (aSize < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (aSize > 31) {
            throw new RuntimeException("Component " + id + " max size is 31");
        }
        int ramSize = (int) Math.pow(2, aSize);
        words = new long[ramSize];
        addInPin(new InPin("A", this, aSize) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                addr = (int) newState;
                if (csActive) {
                    if (hiImpedance) {
                        throw new FloatingPinException(this);
                    }
                    out();
                }
            }
        });
        addTriStateOutPin("D", size);
        dIn = addInPin("D", size);
        addInPin(new InPin(reverse ? "~{CS}" : "CS", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                csActive = (rawState > 0) ^ reverse;
                out();
            }
        });
        addInPin(new InPin(reverse ? "~{OE}" : "OE", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                oeActive = (newState > 0) ^ reverse;
                out();
            }
        });
        if (reverse) {
            addInPin(new FallingEdgeInPin("~{WE}", this) {
                @Override
                public void onFallingEdge() {
                    if (csActive) {
                        words[addr] = (byte) dIn.getState();
                    }
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("WE", this) {
                @Override
                public void onRisingEdge() {
                    if (csActive) {
                        words[addr] = (byte) dIn.getState();
                    }
                }
            });
        }
    }

    @Override
    public void initOuts() {
        dOut = (TriStateOutPin) getOutPin("D");
        dOut.hiImpedance = true;
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(size / 4d) + "X", addr) + "\nD:" + String.format("%0" + (int) Math.ceil(size / 4d) + "X", words[addr]);
    }

    private void out() {
        if (oeActive && csActive) {
            dOut.setState(words[addr]);
        } else {
            dOut.setHiImpedance();
        }
    }
}
