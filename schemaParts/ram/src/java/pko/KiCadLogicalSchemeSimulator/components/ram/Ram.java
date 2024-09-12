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
package pko.KiCadLogicalSchemeSimulator.components.ram;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.NoFloatingCorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;

public class Ram extends SchemaPart {
    private final long[] words;
    private final InBus dIn;
    private final int size;
    private final int aSize;
    private final InPin csPin;
    private final InPin oePin;
    private final Bus aBus;
    private Bus dOut;

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
        addOutBus("D", size);
        dIn = addInBus("D", size);
        if (reverse) {
            aBus = addInBus(new NoFloatingCorrectedInBus("A", this, aSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    if (!csPin.state) {
                        rOut();
                    }
                    hiImpedance = false;
                }
            });
            csPin = addInPin(new NoFloatingInPin("~{CS}", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    rOut();
                }
            });
            oePin = addInPin(new NoFloatingInPin("~{OE}", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    rOut();
                }
            });
            addInPin(new NoFloatingInPin("~{WE}", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    if (!state && !csPin.state) {
                        words[(int) aBus.state] = dIn.state;
                    }
                }
            });
        } else {
            aBus = addInBus(new NoFloatingCorrectedInBus("A", this, aSize) {
                @Override
                public void setState(long newState) {
                    state = newState;
                    if (!csPin.state) {
                        out();
                    }
                    hiImpedance = false;
                }
            });
            csPin = addInPin(new NoFloatingInPin("CS", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    out();
                }
            });
            oePin = addInPin(new NoFloatingInPin("OE", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    out();
                }
            });
            addInPin(new NoFloatingInPin("WE", this) {
                @Override
                public void setState(boolean newState) {
                    hiImpedance = false;
                    state = newState;
                    if (state && csPin.state) {
                        words[(int) aBus.state] = dIn.state;
                    }
                }
            });
        }
    }

    @Override
    public void initOuts() {
        dOut = getOutBus("D");
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(size / 4d) + "X", (int) aBus.state) + "\nD:" +
                String.format("%0" + (int) Math.ceil(size / 4d) + "X", words[(int) aBus.state]);
    }

    private void out() {
        if (oePin.state && csPin.state) {
            if (dOut.state != words[(int) aBus.state] || dOut.hiImpedance) {
                dOut.state = words[(int) aBus.state];
                dOut.hiImpedance = false;
                dOut.setState(dOut.state);
            }
        } else {
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
                dOut.hiImpedance = true;
            }
        }
    }

    private void rOut() {
        if (oePin.state | csPin.state) {
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
                dOut.hiImpedance = true;
            }
        } else {
            if (dOut.state != words[(int) aBus.state] || dOut.hiImpedance) {
                dOut.state = words[(int) aBus.state];
                dOut.hiImpedance = false;
                dOut.setState(dOut.state);
            }
        }
    }
}
