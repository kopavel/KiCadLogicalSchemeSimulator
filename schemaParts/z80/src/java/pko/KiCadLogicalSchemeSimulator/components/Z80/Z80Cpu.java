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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.components.Z80.core.CPUConstants;
import pko.KiCadLogicalSchemeSimulator.components.Z80.core.Z80Core;
import pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue.IoQueue;

public class Z80Cpu extends SchemaPart {
    protected final InPin waitPin;
    protected final InBus dIn;
    final Z80Core cpu;
    final IoQueue ioQueue = new IoQueue();
    protected Bus dOut;
    protected Bus aOut;
    protected Pin rdPin;
    protected Pin wrPin;
    protected Pin mReqPin;
    protected Pin ioReqPin;
    protected Pin m1Pin;
    protected Pin refreshPin;
    protected Pin haltPin;
    Z80CPin cPin;

    public Z80Cpu(String id) {
        super(id, null);
        cpu = new Z80Core(ioQueue);
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
            }

            @Override
            public void setLo() {
                state = false;
                cPin.nmiTriggered = true;
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
        cPin = addInPin(new Z80CPin("CLK", this));
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
        cPin.rdPin = getOutPin("~{RD}");
        cPin.wrPin = getOutPin("~{WR}");
        cPin.mReqPin = getOutPin("~{MREQ}");
        cPin.ioReqPin = getOutPin("~{IORQ}");
        cPin.m1Pin = getOutPin("~{M1}");
        cPin.refreshPin = getOutPin("~{RFSH}");
        cPin.aOut = getOutBus("A");
        cPin.dOut = getOutBus("D");
    }

    @Override
    public String extraState() {
        return "Adr :" + String.format("%04x", aOut.state) +//
                "\nData:" + String.format("%02x", dIn.state) +//
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
                "\nNMI triggered:" + cPin.nmiTriggered +//
                "\nM   :" + cPin.M +//
                "\nT   :" + cPin.T;
    }

    public void reset() {
        cPin.T = 0;
        cpu.reset();
        if (mReqPin.hiImpedance || !mReqPin.state) {
            mReqPin.setHi();
        }
        if (rdPin.hiImpedance || !rdPin.state) {
            rdPin.setHi();
        }
        if (wrPin.hiImpedance || !wrPin.state) {
            wrPin.setHi();
        }
        if (!aOut.hiImpedance) {
            aOut.setHiImpedance();
        }
        if (!dOut.hiImpedance) {
            dOut.setHiImpedance();
        }
        if (ioReqPin.hiImpedance || !ioReqPin.state) {
            ioReqPin.setHi();
        }
        if (m1Pin.hiImpedance || !m1Pin.state) {
            m1Pin.setHi();
        }
        if (refreshPin.hiImpedance || !refreshPin.state) {
            refreshPin.setHi();
        }
        cPin.M = 1;
        cPin.nmiTriggered = false;
    }
}
