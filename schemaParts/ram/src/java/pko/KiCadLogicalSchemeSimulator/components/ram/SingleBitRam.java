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
                public void setState(long newState) {
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
                }

                @Override
                public void setLo() {
                    state = false;
                    if (!csPin.state) {
                        data[(int) aBus.state] = dIn.state;
                    }
                }
            });
        } else {
            aBus = addInBus(new InBus("A", this, size) {
                @Override
                public void setState(long newState) {
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
                    if (csPin.state) {
                        data[(int) aBus.state] = dIn.state;
                    }
                }

                @Override
                public void setLo() {
                    state = false;
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
        return "A:" + String.format("%0" + (int) Math.ceil(size / 4d) + "X", (int) aBus.state) + "\nD:" + data[(int) aBus.state];
    }

    private void out() {
        if (oePin.state && csPin.state) {
            if (dOut.state != data[(int) aBus.state] || dOut.hiImpedance) {
                if (data[(int) aBus.state]) {
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
        if (oePin.state | csPin.state) {
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
            }
        } else {
            if (dOut.state != data[(int) aBus.state] || dOut.hiImpedance) {
                if (data[(int) aBus.state]) {
                    dOut.setHi();
                } else {
                    dOut.setLo();
                }
            }
        }
    }
}
