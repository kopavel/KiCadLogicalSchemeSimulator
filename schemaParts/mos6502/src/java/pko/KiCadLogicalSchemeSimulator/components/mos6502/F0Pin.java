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
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Cpu;
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

    public F0Pin(String id, Mos6502 parent) {
        super(id, parent);
        rdyPin = parent.rdyPin;
        dIn = parent.dIn;
        Cpu core = parent.core;
        queue = new IoQueue(core, this);
        core.setIoQueue(queue);
    }

    @Override
    public void setHi() {
        state = true;
        Request request = curentRequest;
        if (isReady) {
            if (request.read) {
                request.address = -1;
                request.callback.accept(dIn.state);
            }
            if (opCode) {
                syncPin.setLo();
                opCode = false;
            }
            request = (curentRequest = queue.pop());
        }
        f2Pin.setLo();
        if (!dOut.hiImpedance) {
            dOut.setHiImpedance();
        }
        f1Pin.setHi();
        if (isReady) {
            aOut.setState(request.address);
            if (request.read) {
                if (!rwPin.state) {
                    rwPin.setHi();
                }
            } else if (rwPin.state) {
                rwPin.setLo();
            }
            if (opCode) {
                syncPin.setHi();
            }
        }
        isReady = rdyPin.state || !request.read;
        f1Pin.setLo();
    }

    @Override
    public void setLo() {
        state = false;
        Request request;
        f2Pin.setHi();
        if (!curentRequest.read && (request = curentRequest).address >= 0) {
            dOut.setState(request.payload);
            request.address = -1;
        }
    }
}
