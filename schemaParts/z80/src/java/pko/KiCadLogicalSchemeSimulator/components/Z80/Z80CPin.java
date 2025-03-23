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
import com.codingrodent.microprocessor.Z80.Z80Core;
import com.codingrodent.microprocessor.io.queue.AsyncIoQueue;
import com.codingrodent.microprocessor.io.queue.Request;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Z80CPin extends InPin {
    final public InBus dIn;
    final public AsyncIoQueue ioQueue;
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
        this.waitPin = parent.waitPin;
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
        AsyncIoQueue queue = ioQueue;
        state = true;
        int lM = M;
        int lT;
        if (((lT = T) == 3 && lM != 1) || lT == 4) {
            lT = (T = 1);
            Pin pin;
            Bus bus;
            if (!(pin = refreshPin).state) {
                pin.setHi();
            }
            if (!(bus = dOut).hiImpedance) {
                bus.setHiImpedance();
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
                    Request ioRequest = queue.request;
                    ioRequest.callback.accept((int) dIn.state);
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
                        ioRequest.callback.accept((int) dIn.state);
                        mReqPin.setHi();
                        rdPin.setHi();
                    } else {
                        mReqPin.setHi();
                        wrPin.setHi();
                    }
                } else {
                    if (ioRequest.read) {
                        ioRequest.callback.accept((int) dIn.state);
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
