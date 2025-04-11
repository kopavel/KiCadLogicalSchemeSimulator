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
package pko.KiCadLogicalSchemeSimulator.components.mos6502;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.FallingEdgePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Cpu;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.IoQueue;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.Request;

public class Mos6502 extends SchemaPart {
    private final Cpu core;
    private final IoQueue queue;
    public InPin f0Pin;
    public FallingEdgePin nRdyPin;
    public InPin nNmiPin;
    public InPin nIrqPin;
    public InPin nSoPin;
    public InBus dIn;
    public Pin f1Pin;
    public Pin f2Pin;
    public Pin syncPin;
    public Pin rwPin;
    public Bus dOut;
    public Bus aOut;
    public boolean isReady;
    private Request curentRequest;

    protected Mos6502(String id, String sParam) {
        super(id, sParam);
        core = new Cpu();
        queue = new IoQueue();
        core.setIoQueue(queue);
        addInPin(new InPin("~{RESET}", this) {
            @Override
            public void setHi() {
                state = true;
                reset();
            }

            @Override
            public void setLo() {
                state = false;
            }
        });
        addInPin(new InPin("~{NMI}", this) {
            @Override
            public void setHi() {
                state = true;
                core.state.nmiAsserted = false;
            }

            @Override
            public void setLo() {
                state = false;
                core.state.nmiAsserted = true;
            }
        });
        addInPin(new InPin("~{IRQ}", this) {
            @Override
            public void setHi() {
                state = true;
                core.state.irqAsserted = false;
            }

            @Override
            public void setLo() {
                state = false;
                core.state.irqAsserted = true;
            }
        });
        addInPin(new InPin("~{S.O.}", this) {
            @Override
            public void setHi() {
                state = true;
            }

            @Override
            public void setLo() {
                state = false;
                core.state.overflowFlag = true;
            }
        });
        InPin rdyPin = addInPin("~{RDY}");
        addInPin(new InPin("F0", this) {
            @Override
            public void setHi() {
                if (isReady) {
                    if (curentRequest != null && curentRequest.read) {
                        curentRequest.callback.accept((int) dIn.state);
                    }
                    curentRequest = queue.next();
                    f2Pin.setLo();
                    if (curentRequest == null) {
                        core.step();
                        curentRequest = queue.request;
                    }
                    f1Pin.setHi();
                    aOut.setState(curentRequest.address);
                    if (curentRequest.read) {
                        if (!rwPin.state) {
                            rwPin.setHi();
                        }
                        if (!dOut.hiImpedance) {
                            dOut.setHiImpedance();
                        }
                    } else {
                        if (rwPin.state) {
                            rwPin.setLo();
                        }
                    }
                    f1Pin.setLo();
                }
                isReady = rdyPin.state;
            }

            @Override
            public void setLo() {
                if (isReady) {
                    f2Pin.setHi();
                    if (!curentRequest.read) {
                        dOut.setState(curentRequest.payload);
                    }
                }
            }
        });
        dIn = addInBus("D", 8);
        addTriStateOutBus("D", 8);
        addOutBus("A", 16);
        addOutPin("F1");
        addOutPin("F2");
        addOutPin("SYNC");
        addOutPin("R/~{W}");
    }

    @Override
    public void reset() {
        isReady = false;
        if (!dOut.hiImpedance) {
            dOut.setHiImpedance();
        }
        queue.clear();
        core.reset();
    }

    @Override
    public void initOuts() {
        f1Pin = getOutPin("F1");
        f2Pin = getOutPin("F2");
        syncPin = getOutPin("SYNC");
        rwPin = getOutPin("R/~{W}");
        dOut = getOutBus("D");
        aOut = getOutBus("A");
    }
}
