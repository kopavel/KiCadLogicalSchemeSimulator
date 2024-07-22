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
package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api_v2.FloatingInException;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.FallingEdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.NoFloatingInPin;

public class Buffer extends SchemaPart {
    private final int busSize;
    private final InBus dBus;
    private final InPin oePin;
    private Bus qBus;
    private long latch;

    public Buffer(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        try {
            busSize = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (busSize < 1) {
            throw new RuntimeException("Component " + id + " size  must be positive number");
        }
        if (busSize > 64) {
            throw new RuntimeException("Component " + id + " size  must be less then 64");
        }
        addOutBus("Q", busSize);
        if (params.containsKey("latch")) {
            oePin = addInPin(new NoFloatingInPin("~{OE}", this) {
                @Override
                public void setState(boolean newState, boolean newStrong) {
                    state = newState;
                    if (!state) {
                        if (qBus.state != latch || qBus.hiImpedance) {
                            qBus.state = latch;
                            qBus.hiImpedance = false;
                            qBus.setState(qBus.state);
                        }
                    } else if (!qBus.hiImpedance) {
                        qBus.setHiImpedance();
                        qBus.hiImpedance = true;
                    }
                }
            });
            addInPin(new FallingEdgeInPin("~{WR}", this) {
                @Override
                public void onFallingEdge() {
                    latch = dBus.state;
                    if (!oePin.state && (qBus.state != latch || qBus.hiImpedance)) {
                        qBus.state = latch;
                        qBus.hiImpedance = false;
                        qBus.setState(latch);
                    }
                }
            });
            dBus = addInBus("D", busSize);
        } else {
            oePin = addInPin(new NoFloatingInPin("~{CS}", this) {
                @Override
                public void setState(boolean newState, boolean newStrong) {
                    state = newState;
                    if (!state) {
                        if (qBus.state != dBus.state || qBus.hiImpedance) {
                            qBus.state = dBus.state;
                            qBus.hiImpedance = false;
                            qBus.setState(qBus.state);
                        }
                    } else if (!qBus.hiImpedance) {
                        qBus.setHiImpedance();
                        qBus.hiImpedance = true;
                    }
                }
            });
            dBus = addInBus(new InBus("D", this, busSize) {
                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                    if (!oePin.state) {
                        throw new FloatingInException(this);
                    }
                }

                @Override
                public void setState(long newState) {
                    state = newState;
                    hiImpedance = false;
                    if (!oePin.state && (qBus.state != latch || qBus.hiImpedance)) {
                        qBus.state = newState;
                        qBus.hiImpedance = false;
                        qBus.setState(newState);
                    }
                }
            });
        }
    }

    @Override
    public String extraState() {
        return "D:" + String.format("%" + (int) Math.ceil(busSize / 4d) + "X", dBus.state) + "\nQ:" +
                String.format("%" + (int) Math.ceil(busSize / 4d) + "X", qBus.state);
    }

    @Override
    public void initOuts() {
        qBus = getOutBus("Q");
    }
}
