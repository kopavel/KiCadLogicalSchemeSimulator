/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.mos6502;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Cpu;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.Request;

public class Mos6502 extends SchemaPart {
    public final InPin rdyPin;
    public final Cpu core;
    public final F0Pin f0Pin;
    public final InBus dIn;
    public Pin f1Pin;
    public Pin f2Pin;
    public Pin syncPin;
    public Pin rwPin;
    public Bus dOut;
    public Bus aOut;
    public boolean opCode;

    protected Mos6502(String id, String sParam) {
        super(id, sParam);
        core = new Cpu();
        addInPin(new InPin("~{RES}", this) {
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
                core.state.nmiAsserted = true;
            }
        });
        addInPin(new InPin("~{IRQ}", this) {
            @Override
            public void setHi() {
                state = true;
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
        rdyPin = addInPin("~{RDY}");
        dIn = addInBus("D", 8);
        f0Pin = addInPin(new F0Pin("F0", this));
        addTriStateOutBus("D", 8);
        addOutBus("A", 16);
        addOutPin("F1");
        addOutPin("F2");
        addOutPin("SYNC");
        addOutPin("R/~{W}");
    }

    @Override
    public void reset() {
        f0Pin.resetCounter = 6;
    }

    @Override
    public void initOuts() {
        f1Pin = getOutPin("F1");
        f0Pin.f1Pin = f1Pin;
        f2Pin = getOutPin("F2");
        f0Pin.f2Pin = f2Pin;
        syncPin = getOutPin("SYNC");
        f0Pin.syncPin = syncPin;
        rwPin = getOutPin("R/~{W}");
        f0Pin.rwPin = rwPin;
        dOut = getOutBus("D");
        f0Pin.dOut = dOut;
        aOut = getOutBus("A");
        f0Pin.aOut = aOut;
    }

    @Override
    public String extraState() {
        Request request = f0Pin.curentRequest;
        return "Queue:" + (request.address < 0 ? "" : (request + ";")) + f0Pin.queue.toString() + "\n" + core.state.toTraceEvent();
    }
}
