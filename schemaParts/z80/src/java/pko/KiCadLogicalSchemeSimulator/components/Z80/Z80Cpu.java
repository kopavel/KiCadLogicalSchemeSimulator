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
import com.codingrodent.microprocessor.Io.AsyncIoQueue;
import com.codingrodent.microprocessor.Io.IoRequest;
import com.codingrodent.microprocessor.Z80.CPUConstants;
import com.codingrodent.microprocessor.Z80.Z80Core;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.EdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.RisingEdgeInPin;

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
    private boolean extraWait;
    private boolean needDataPinReset;
    private boolean needRefreshPinReset;

    public Z80Cpu(String id) {
        super(id, null);
        cpu = new Z80Core(ioQueue);
        addInPin(new EdgeInPin("CLK", this) {
            @Override
            public void onFallingEdge() {
                if (M > 0) {
                    IoRequest ioRequest = ioQueue.requests.peek();
                    assert ioRequest != null;
                    if (T1) {
                        if (ioRequest.isMemory) {
                            mReqPin.state = false;
                            mReqPin.setState(false, true);
                            if (!ioRequest.isWrite) {
                                rdPin.state = false;
                                rdPin.setState(false, true);
                            }
                        }
                        if (ioRequest.isWrite) {
                            dOut.state = ioRequest.payload;
                            dOut.setState(ioRequest.payload);
                            needDataPinReset = true;
                        }
                    } else if (T2) {
                        if (!inWait && ioRequest.isMemory && ioRequest.isWrite) {
                            wrPin.state = false;
                            wrPin.setState(false, true);
                        }
                        inWait = !waitPin.state;
                    } else if (T3) {
                        if (M1) {
                            mReqPin.state = false;
                            mReqPin.setState(false, true);
                        } else if (ioRequest.isMemory) {
                            if (!ioRequest.isWrite) {
                                ioRequest.callback.accept((int) dIn.getState());
                                rdPin.state = true;
                                rdPin.setState(true, true);
                            } else {
                                wrPin.state = true;
                                wrPin.setState(true, true);
                            }
                            mReqPin.state = true;
                            mReqPin.setState(true, true);
                        } else {
                            if (!ioRequest.isWrite) {
                                ioRequest.callback.accept((int) dIn.getState());
                                rdPin.state = true;
                                rdPin.setState(true, true);
                            } else {
                                wrPin.state = true;
                                wrPin.setState(true, true);
                            }
                            ioReqPin.state = true;
                            ioReqPin.setState(true, true);
                        }
                    } else {
                        mReqPin.state = true;
                        mReqPin.setState(true, true);
                    }
                }
            }

            @Override
            public void onRisingEdge() {
                if (M > 0) {
                    if (T4 || (!M1 && T3)) {
                        T = 1;
                        ioQueue.requests.poll();
//                Log.trace(Z80Cpu.class, "cpuDone is {}", cpuDone);
                        if (ioQueue.requests.isEmpty()) {
                            M = 1;
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
//        Log.trace(Z80Cpu.class, "Set pins at {},{}", M, T);
                    IoRequest ioRequest = ioQueue.requests.peek();
                    assert ioRequest != null;
                    if (T1) {
                        if (needRefreshPinReset) {
                            refreshPin.state = true;
                            refreshPin.setState(true, true);
                            needRefreshPinReset = false;
                        }
                        if (needDataPinReset) {
                            dOut.setHiImpedance();
                            dOut.hiImpedance = true;
                            needDataPinReset = false;
                        }
                        if (M1) {
                            cpu.executeOneInstruction();
                            ioRequest = ioQueue.requests.peek();
                            assert ioRequest != null;
                            m1Pin.state = false;
                            m1Pin.setState(false, true);
                        }
                        aOut.state = ioRequest.address;
                        aOut.setState(ioRequest.address);
                        extraWait = !ioRequest.isMemory;
                    } else if (T2) {
                        if (!inWait && !ioRequest.isMemory) {
                            ioReqPin.state = false;
                            ioReqPin.setState(false, true);
                            if (ioRequest.isWrite) {
                                wrPin.state = false;
                                wrPin.setState(false, true);
                            } else {
                                rdPin.state = false;
                                rdPin.setState(false, true);
                            }
                        }
                    } else if (T3) {
                        if (M1) {
                            ioRequest.callback.accept((int) dIn.getState());
                            rdPin.state = true;
                            rdPin.setState(true, true);
                            mReqPin.state = true;
                            mReqPin.setState(true, true);
                            m1Pin.state = true;
                            m1Pin.setState(true, true);
                            refreshPin.state = false;
                            refreshPin.setState(false, true);
                            needRefreshPinReset = true;
                        }
                    }
                }
            }
        });
        addInPin(new RisingEdgeInPin("~{RESET}", this) {
            @Override
            public void onRisingEdge() {
                reset();
            }
        });
        addInPin("~{NMI}");
        addInPin("~{INT}");
        waitPin = addInPin("~{WAIT}");
        addInPin("~{BUSRQ}");
        addOutPin("~{BUSACK}", true, true);
        addOutPin("~{RD}", true, true);
        addOutPin("~{WR}", true, true);
        addOutPin("~{MREQ}", true, true);
        addOutPin("~{IORQ}", true, true);
        addOutPin("~{M1}", true, true);
        addOutPin("~{RFSH}", true, true);
        addOutPin("~{HALT}", true, true);
        addOutBus("D", 8);
        addOutBus("A", 16);
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
                "\nI   :" + String.format("%02x", cpu.getRegisterValue(CPUConstants.RegisterNames.I));
    }

    public void reset() {
//            Log.trace(Z80Cpu.class, "reset");
        T = 0;
        M = 0;
        cpu.reset();
        mReqPin.state = true;
        mReqPin.setState(true, true);
        rdPin.state = true;
        rdPin.setState(true, true);
        wrPin.state = true;
        wrPin.setState(true, true);
        aOut.state = 0;
        aOut.setState(0);
        dOut.setHiImpedance();
        dOut.hiImpedance = true;
        rdPin.state = true;
        rdPin.setState(true, true);
        wrPin.state = true;
        wrPin.setState(true, true);
        mReqPin.state = true;
        mReqPin.setState(true, true);
        ioReqPin.state = true;
        ioReqPin.setState(true, true);
        m1Pin.state = true;
        m1Pin.setState(true, true);
        refreshPin.state = true;
        refreshPin.setState(true, true);
        M = 1;
    }
}
