/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.mos6502;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Cpu;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.Callback;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.IoQueue;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.queue.Request;

public class F0Pin extends InPin {
    public final IoQueue queue;
    private final InPin rdyPin;
    private final InBus dIn;
    public boolean isReady;
    public Request curentRequest = new Request();
    public Pin syncPin;
    public Pin f2Pin;
    public Pin f1Pin;
    public Pin rwPin;
    public Bus dOut;
    public Bus aOut;
    public boolean opCode;
    public int resetCounter = 6;

    public F0Pin(String id, Mos6502 parent) {
        super(id, parent);
        rdyPin = parent.rdyPin;
        dIn = parent.dIn;
        Cpu core = parent.core;
        queue = new IoQueue(core, this);
        core.setIoQueue(queue);
    }

    @Override
    public void setLo() {
        state = false;
        if (resetCounter != 0) {
            return;
        }
        boolean ready = isReady;
        Request request = curentRequest;
        if (ready) {
            if (request.read) {
                Callback callback = request.callback;
                int data = dIn.state;
                request.address = -1;
                callback.accept(data);
            }
            if (opCode) {
                syncPin.setLo();
                opCode = false;
            }
            curentRequest = request = queue.pop();
        }
        f2Pin.setLo();
        Bus dout = dOut;
        if (!dout.hiImpedance) {
            dout.setHiImpedance();
        }
        f1Pin.setHi();
        boolean read = request.read;
        if (ready) {
            aOut.setState(request.address);
            Pin rw = rwPin;
            if (rw.state != read) {
                if (read) {
                    rw.setHi();
                } else {
                    rw.setLo();
                }
            }
            if (opCode) {
                syncPin.setHi();
            }
        }
        isReady = !read || rdyPin.state;
    }

    @Override
    public void setHi() {
        state = true;
        if (resetCounter == 0) {
            f1Pin.setLo();
            Request request;
            f2Pin.setHi();
            if (!(request = curentRequest).read && request.address >= 0) {
                dOut.setState(request.payload);
                request.address = -1;
            }
        } else {
            if (resetCounter == 6) {
                reset();
            }
            resetCounter--;
        }
    }

    public void reset() {
        queue.clear();
        isReady = rdyPin.state;
        opCode = false;
        resetCounter = 6;
        curentRequest = new Request();
        ((Mos6502) parent).core.reset();
    }
}
