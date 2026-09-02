/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.Z80;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.components.Z80.core.Z80Core;
import pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue.IoQueue;
import pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue.Request;

public class Z80CPin extends InPin {
    final public InBus dIn;
    final public IoQueue ioQueue;
    final public Z80Core cpu;
    private final InPin waitPin;
    public Pin refreshPin;
    public Bus dOut;
    public Bus aOut;
    public Pin rdPin;
    public Pin wrPin;
    public Pin mReqPin;
    public Pin m1Pin;
    public Pin ioReqPin;
    int T;
    int M;
    boolean notInWait;
    boolean nmiTriggered;
    boolean extraWait;

    public Z80CPin(String id, Z80Cpu parent) {
        super(id, parent);
        waitPin = parent.waitPin;
        refreshPin = parent.refreshPin;
        aOut = parent.aOut;
        dOut = parent.dOut;
        rdPin = parent.rdPin;
        wrPin = parent.wrPin;
        mReqPin = parent.mReqPin;
        m1Pin = parent.m1Pin;
        ioReqPin = parent.ioReqPin;
        dIn = parent.dIn;
        ioQueue = parent.ioQueue;
        cpu = parent.cpu;
    }

    @Override
    public void setHi() {
        IoQueue queue = ioQueue;
        state = true;
        int lM;
        int lT;
        if ((lT = T) == 4 || ((lM = M) != 1 && lT == 3)) {
            lT = (T = 1);
            if (!refreshPin.state) {
                refreshPin.setHi();
            }
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
            }
            queue.next();
            if (queue.request.address == -1) {
                if (nmiTriggered) {
                    nmiTriggered = false;
                    cpu.processNMI();
                    lM = ++M;
                } else {
                    lM = (M = 1);
                }
            } else {
                lM = ++M;
            }
        } else if (lT == 2 && (extraWait || !notInWait)) {
            extraWait = false;
        } else {
            lT = ++T;
        }
        switch (lT) {
            case 1 -> {
                if (lM == 1) {
                    m1Pin.setLo();
                    cpu.executeOneInstruction();
                }
                Request ioRequest = queue.request;
                aOut.setState(ioRequest.address);
                extraWait = !ioRequest.memory;
            }
            case 2 -> {
                Request ioRequest = queue.request;
                if (!ioRequest.memory && notInWait) {
                    ioReqPin.setLo();
                    if (ioRequest.read) {
                        rdPin.setLo();
                    } else {
                        wrPin.setLo();
                    }
                }
            }
            case 3 -> {
                if (lM == 1) {
                    queue.request.callback.accept(dIn.state);
                    mReqPin.setHi();
                    rdPin.setHi();
                    m1Pin.setHi();
                    //FixMe create refresh address counter and set address from it.
                    aOut.setState(0);
                    refreshPin.setLo();
                }
            }
        }
    }

    @Override
    public void setLo() {
        state = false;
        Request ioRequest = ioQueue.request;
        switch (T) {
            case 1 -> {
                if (ioRequest.memory) {
                    if (ioRequest.read) {
                        rdPin.setLo();
                    }
                    mReqPin.setLo();
                }
                if (!ioRequest.read) {
                    dOut.setState(ioRequest.payload);
                }
            }
            case 2 -> {
                if (!ioRequest.read && ioRequest.memory && notInWait) {
                    wrPin.setLo();
                }
                notInWait = waitPin.state;
            }
            case 3 -> {
                if (M == 1) {
                    mReqPin.setLo();
                } else if (ioRequest.memory) {
                    if (ioRequest.read) {
                        ioRequest.callback.accept(dIn.state);
                        mReqPin.setHi();
                        rdPin.setHi();
                    } else {
                        mReqPin.setHi();
                        wrPin.setHi();
                    }
                } else {
                    if (ioRequest.read) {
                        ioRequest.callback.accept(dIn.state);
                        rdPin.setHi();
                    } else {
                        wrPin.setHi();
                    }
                    ioReqPin.setHi();
                }
            }
            default -> mReqPin.setHi();
        }
    }
}
