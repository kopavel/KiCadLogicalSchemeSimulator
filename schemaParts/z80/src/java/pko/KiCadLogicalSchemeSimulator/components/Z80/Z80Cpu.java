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
package pko.KiCadLogicalSchemeSimulator.components.Z80;
import com.codingrodent.microprocessor.Z80.CPUConstants;
import com.codingrodent.microprocessor.Z80.Z80Core;
import com.codingrodent.microprocessor.io.device.DeviceRequest;
import com.codingrodent.microprocessor.io.memory.MemoryRequest;
import com.codingrodent.microprocessor.io.queue.AsyncIoQueue;
import com.codingrodent.microprocessor.io.queue.ReadRequest;
import com.codingrodent.microprocessor.io.queue.Request;
import com.codingrodent.microprocessor.io.queue.WriteRequest;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Z80Cpu extends SchemaPart {
    private final Z80Core cpu;
    private final AsyncIoQueue ioQueue = new AsyncIoQueue();
    protected InBus dIn;
    protected Bus dOut;
    protected Bus aOut;
    protected InPin waitPin;
    protected Pin rdPin;
    protected Pin wrPin;
    protected Pin mReqPin;
    protected Pin ioReqPin;
    protected Pin m1Pin;
    protected Pin refreshPin;
    protected Pin haltPin;
    protected int T;
    protected int M;
    private boolean inWait;
    private boolean T1;
    private boolean T2;
    private boolean T3;
    private boolean T4;
    private boolean M1;
    private boolean nmiTriggered;
    private boolean extraWait;
    private boolean needDataPinReset;
    private boolean needRefreshPinReset;

    public Z80Cpu(String id) {
        super(id, null);
        cpu = new Z80Core(ioQueue);
        addInPin(new InPin("CLK", this) {
            @Override
            public void setState(boolean newState) {
                state = newState;
                if (state) {
                    if (M > 0) {
                        if (T4 || (!M1 && T3)) {
                            T = 1;
                            if (needRefreshPinReset) {
                                refreshPin.setState(true);
                                needRefreshPinReset = false;
                            }
                            if (needDataPinReset) {
                                if (!dOut.hiImpedance) {
                                    dOut.setHiImpedance();
                                }
                                needDataPinReset = false;
                            }
                            ioQueue.next();
                            if (ioQueue.request == null) {
                                if (nmiTriggered) {
                                    nmiTriggered = false;
                                    cpu.processNMI();
                                    M++;
                                } else {
                                    M = 1;
                                }
                            } else {
                                M++;
                            }
                        } else {
                            if (T2 && (inWait || extraWait)) {
                                extraWait = false;
                            } else {
                                T++;
                            }
                        }
                        T1 = T == 1;
                        T2 = T == 2;
                        T3 = T == 3;
                        T4 = T == 4;
                        M1 = M == 1;
                        Request ioRequest = ioQueue.request;
                        if (T1) {
                            if (M1) {
                                m1Pin.setState(false);
                                cpu.executeOneInstruction();
                                ioRequest = ioQueue.request;
                            }
                            aOut.setState(ioRequest.address);
                            extraWait = ioRequest instanceof DeviceRequest;
                        } else if (T2) {
                            if (!inWait && ioRequest instanceof DeviceRequest) {
                                ioReqPin.setState(false);
                                if (ioRequest instanceof WriteRequest) {
                                    wrPin.setState(false);
                                } else {
                                    rdPin.setState(false);
                                }
                            }
                        } else if (T3) {
                            if (M1) {
                                //noinspection DataFlowIssue
                                ((ReadRequest) ioRequest).callback.accept((int) dIn.getState());
                                rdPin.setState(true);
                                mReqPin.setState(true);
                                m1Pin.setState(true);
                                refreshPin.setState(false);
                                needRefreshPinReset = true;
                            }
                        }
                    }
                } else {
                    if (M > 0) {
                        Request ioRequest = ioQueue.request;
                        if (T1) {
                            if (ioRequest instanceof MemoryRequest) {
                                mReqPin.setState(false);
                                if (ioRequest instanceof ReadRequest) {
                                    rdPin.setState(false);
                                }
                            }
                            if (ioRequest instanceof WriteRequest writeRequest) {
                                dOut.setState(writeRequest.payload);
                                needDataPinReset = true;
                            }
                        } else if (T2) {
                            if (!inWait && ioRequest instanceof MemoryRequest && ioRequest instanceof WriteRequest) {
                                wrPin.setState(false);
                            }
                            inWait = !waitPin.state;
                        } else if (T3) {
                            if (M1) {
                                mReqPin.setState(false);
                            } else if (ioRequest instanceof MemoryRequest) {
                                if (ioRequest instanceof ReadRequest readRequest) {
                                    readRequest.callback.accept((int) dIn.getState());
                                    rdPin.setState(true);
                                } else {
                                    wrPin.setState(true);
                                }
                                mReqPin.setState(true);
                            } else {
                                if (ioRequest instanceof ReadRequest readRequest) {
                                    readRequest.callback.accept((int) dIn.getState());
                                    rdPin.setState(true);
                                } else {
                                    wrPin.setState(true);
                                }
                                ioReqPin.setState(true);
                            }
                        } else {
                            mReqPin.setState(true);
                        }
                    }
                }
            }
        });
        addInPin(new InPin("~{RESET}", this) {
            @Override
            public void setState(boolean newState) {
                state = newState;
                if (state) {
                    reset();
                }
            }
        });
        addInPin(new InPin("~{NMI}", this) {
            @Override
            public void setState(boolean newState) {
                state = newState;
                if (!newState) {
                    nmiTriggered = true;
                }
            }
        });
        addInPin("~{INT}");
        waitPin = addInPin("~{WAIT}");
        addInPin("~{BUSRQ}");
        addTriStateOutPin("~{BUSACK}", true);
        addTriStateOutPin("~{RD}", true);
        addTriStateOutPin("~{WR}", true);
        addTriStateOutPin("~{MREQ}", true);
        addTriStateOutPin("~{IORQ}", true);
        addTriStateOutPin("~{M1}", true);
        addTriStateOutPin("~{RFSH}", true);
        addTriStateOutPin("~{HALT}", true);
        addTriStateOutBus("D", 8);
        addTriStateOutBus("A", 16);
        dIn = addInBus("D", 8);
    }

    @Override
    public void initOuts() {
        rdPin = getOutPin("~{RD}");
        wrPin = getOutPin("~{WR}");
        mReqPin = getOutPin("~{MREQ}");
        ioReqPin = getOutPin("~{IORQ}");
        m1Pin = getOutPin("~{M1}");
        refreshPin = getOutPin("~{RFSH}");
        haltPin = getOutPin("~{HALT}");
        aOut = getOutBus("A");
        dOut = getOutBus("D");
    }

    @Override
    public String extraState() {
        return "Adr :" + String.format("%04x", aOut.state) +//
                "\nData:" + String.format("%02x", dIn.getState()) +//
                "\nPC  :" + String.format("%02x", cpu.getProgramCounter()) + //
                "\nA   :" + String.format("%02x", cpu.getRegisterValue(CPUConstants.RegisterNames.A)) +//
                "\nF   :" + String.format("%06d", Integer.parseInt(Integer.toBinaryString(cpu.getRegisterValue(CPUConstants.RegisterNames.F)))) + //
                "\nBC  :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.BC)) +//
                "\nDE  :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.DE)) +//
                "\nHL  :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.HL)) +//
                "\nIX  :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.IX)) +//
                "\nIY  :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.IY)) +//
                "\nSP  :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.SP)) +//
                "\naA  :" + String.format("%02x", cpu.getRegisterValue(CPUConstants.RegisterNames.A_ALT)) +//
                "\naF  :" + String.format("%06d", Integer.parseInt(Integer.toBinaryString(cpu.getRegisterValue(CPUConstants.RegisterNames.F_ALT)))) + //
                "\naBC :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.BC_ALT)) +//
                "\naDE :" + String.format("%04x", cpu.getRegisterValue(CPUConstants.RegisterNames.DE_ALT)) +//
                "\nI   :" + String.format("%02x", cpu.getRegisterValue(CPUConstants.RegisterNames.I)) +//
                "\nNMI triggered:" + nmiTriggered +//
                "\nM   :" + M +//
                "\nT   :" + T;
    }

    public void reset() {
        T = 0;
        cpu.reset();
        if (mReqPin.hiImpedance || !mReqPin.state) {
            mReqPin.setState(true);
        }
        if (rdPin.hiImpedance || !rdPin.state) {
            rdPin.setState(true);
        }
        if (wrPin.hiImpedance || !wrPin.state) {
            wrPin.setState(true);
        }
        if (!aOut.hiImpedance) {
            aOut.setHiImpedance();
        }
        if (!dOut.hiImpedance) {
            dOut.setHiImpedance();
        }
        if (ioReqPin.hiImpedance || !ioReqPin.state) {
            ioReqPin.setState(true);
        }
        if (m1Pin.hiImpedance || !m1Pin.state) {
            m1Pin.setState(true);
        }
        if (refreshPin.hiImpedance || !refreshPin.state) {
            refreshPin.setState(true);
        }
        M = 1;
        T1 = false;
        T2 = false;
        T3 = false;
        T4 = false;
        M1 = true;
        needDataPinReset = false;
        needRefreshPinReset = false;
        nmiTriggered = false;
    }
}
