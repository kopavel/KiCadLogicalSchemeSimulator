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
package lv.pko.DigitalNetSimulator.components.Z80;
import com.codingrodent.microprocessor.IBaseDevice;
import com.codingrodent.microprocessor.IMemory;
import com.codingrodent.microprocessor.Z80.CPUConstants;
import com.codingrodent.microprocessor.Z80.Z80Core;
import lv.pko.DigitalNetSimulator.api.pins.in.EdgeInPin;
import lv.pko.DigitalNetSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.DigitalNetSimulator.tools.Log;

public class Z80Cpu extends Z80CpuSyncLayer {
    private final Z80Core cpu;
    private boolean inWait;
    private boolean T1;
    private boolean T2;
    private boolean T3;
    private boolean T4;
    private boolean M1;
    private boolean readRequestAtT1;
    private boolean ioRequestAtT1;
    private boolean extraWaitAtT1;
    private boolean needDataPinReset;
    private boolean needRefreshPinReset;

    public Z80Cpu(String id) {
        super(id, null);
        IMemory memoryHandler = getMemoryHandler();
        IBaseDevice portHandler = getPortHandler();
        cpu = new Z80Core(memoryHandler, portHandler);
        addInPin(new EdgeInPin("CLK", this) {
            @Override
            public void onFallingEdge() {
                clockFall();
            }

            @Override
            public void onRisingEdge() {
                clockRaise();
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
        waitPin = addInPin("~{WAIT}", 1);
        addInPin("~{BUSRQ}");
        addOutPin("~{BUSACK}", 1, 1);
        addOutPin("~{RD}", 1, 1);
        addOutPin("~{WR}", 1, 1);
        addOutPin("~{MREQ}", 1, 1);
        addOutPin("~{IORQ}", 1, 1);
        addOutPin("~{M1}", 1, 1);
        addOutPin("~{RFSH}", 1, 1);
        addOutPin("~{HALT}", 1, 1);
        addTriStateOutPin("D", 8);
        addTriStateOutPin("A", 16);
        dIn = addInPin("D", 8);
        Thread.ofPlatform().name("CpuCalcTread").start(() -> {
            while (true) {
                try {
//                    Log.trace(Z80Cpu.class, "ack calcMutex,  amount {}", calcSemaphore.getAvailableAmount());
                    calcSemaphore.acquire();
//                    Log.trace(Z80Cpu.class, "got calcMutex,  amount {}", calcSemaphore.getAvailableAmount());
                    coreBusy();
                    cpu.executeOneInstruction();
//                    Log.trace(Z80Cpu.class, "set  cpuDone=t");
                    haltPin.setState(cpu.getHalt() ? 0 : 1);
                    cpuDone = true;
                    coreFree();
                } catch (Exception e) {
                    Log.error(Z80Cpu.class, "CPU core error at M" + M + " T" + T, e);
                }
            }
        });
//        reset(true);
    }

    public void clockRaise() {
        if (M > 0) {
            if (T4 || (!M1 && T3)) {
                T = 1;
                coreBusy();
//                Log.trace(Z80Cpu.class, "cpuDone is {}", cpuDone);
                if (cpuDone) {
                    doCalc();
                    M = 1;
                } else {
                    M++;
                }
                coreFree();
            } else {
                if (T2 && (inWait || extraWaitAtT1)) {
                    extraWaitAtT1 = false;
                } else {
                    T++;
                }
            }
            T1 = T == 1;
            T2 = T == 2;
            T3 = T == 3;
            T4 = T == 4;
            M1 = M == 1;
            setPinsOnRaise();
        }
    }

    public void clockFall() {
        if (M > 0) {
            setPinsOnFall();
        }
    }

    @Override
    public String extraState() {
        return "Adr :" + String.format("%04x", addOut.state) +//
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

    private void reset() {
//            Log.trace(Z80Cpu.class, "reset");
        T = 0;
        M = 0;
        ioSemaphore.reset(false);
        addressSemaphore.reset(false);
        while (calcSemaphore.availableAmount > 0) {
        }
        cpu.reset();
        mReqPin.setState(1);
        rdPin.setState(1);
        wrPin.setState(1);
        addOut.setState(0);
        dOut.setHiImpedance();
        rdPin.setState(1);
        wrPin.setState(1);
        mReqPin.setState(1);
        ioReqPin.setState(1);
        m1Pin.setState(1);
        refreshPin.setState(1);
        ioSemaphore.reset();
        ioDoneSemaphore.reset();
        addressSemaphore.reset();
        addressDoneSemaphore.reset();
        coreBusyMutex.reset();
        M = 1;
        cpuDone = true;
//            Log.trace(Z80Cpu.class, "reset done. {},{}", addressSemaphore.getAvailableAmount(), addressDoneSemaphore.getAvailableAmount());
    }

    private void setPinsOnRaise() {
//        Log.trace(Z80Cpu.class, "Set pins at {},{}", M, T);
        if (T1) {
            if (needRefreshPinReset) {
                refreshPin.setState(1);
                needRefreshPinReset = false;
            }
            if (needDataPinReset) {
                dOut.setHiImpedance();
                needDataPinReset = false;
            }
            if (M1) {
                if (cpuDone) {
                    doCalc();
                }
                m1Pin.setState(0);
            }
            permitAddress();
            ioRequestAtT1 = ioRequest;
            extraWaitAtT1 = ioRequestAtT1;
            readRequestAtT1 = readRequest;
        } else if (T2) {
            if (!inWait && ioRequestAtT1) {
                ioReqPin.setState(0);
                if (readRequestAtT1) {
                    rdPin.setState(0);
                } else {
                    wrPin.setState(0);
                }
            }
        } else if (T3) {
            if (M1) {
                permitIO();
                rdPin.setState(1);
                mReqPin.setState(1);
                m1Pin.setState(1);
                refreshPin.setState(0);
                needRefreshPinReset = true;
            }
        }
    }

    private void setPinsOnFall() {
//        Log.trace(Z80Cpu.class, "Set pins at {},{}", M, T);
        if (T1) {
            if (!ioRequestAtT1) {
                mReqPin.setState(0);
                if (readRequestAtT1) {
                    rdPin.setState(0);
                }
            }
            if (!readRequestAtT1) {
                permitIO();
                needDataPinReset = true;
            }
        } else if (T2) {
            if (!inWait && !ioRequestAtT1 && !readRequestAtT1) {
                wrPin.setState(0);
            }
            inWait = waitPin.rawState == 0;
        } else if (T3) {
            if (M1) {
                mReqPin.setState(0);
            } else if (!ioRequestAtT1) {
                if (readRequestAtT1) {
                    permitIO();
                    rdPin.setState(1);
                } else {
                    wrPin.setState(1);
                }
                mReqPin.setState(1);
            } else {
                if (readRequestAtT1) {
                    permitIO();
                    rdPin.setState(1);
                } else {
                    wrPin.setState(1);
                }
                ioReqPin.setState(1);
            }
        } else {
            mReqPin.setState(1);
        }
    }
}
