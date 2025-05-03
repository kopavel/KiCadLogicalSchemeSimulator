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

package pko.KiCadLogicalSchemeSimulator.components.mos6502.queue;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.F0Pin;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Cpu;

public class IoQueue {
    private final Cpu core;
    private final F0Pin f0Pin;
    public Request head;
    public Request tail;
    int lowByte;
    private final Callback loReadWordCallback = lowByte -> this.lowByte = lowByte;
    int[] resultArray;
    int arrayPos;
    int arrayLength;
    Callback finalCallback;
    private final Callback hiWordReadCallback = hiByte -> finalCallback.accept((hiByte << 8) | lowByte);
    ArrayCallback arrayCallback;
    private final Callback arrayReadCallback = read -> {
        resultArray[arrayPos++] = read;
        if (arrayPos == arrayLength) {
            arrayCallback.accept();
        }
    };

    public IoQueue(Cpu core, F0Pin f0Pin) {
        this.core = core;
        this.f0Pin = f0Pin;
        tail = new Request();
        head = tail;
        tail.next = tail;
    }

    public void write(int address, int value) {
        shiftWrite();
        Request request;
        (request = tail).address = address;
        request.read = false;
        request.payload = value;
    }

    public void read(int address, Callback callback) {
        shiftWrite();
        Request request;
        (request = tail).address = address;
        request.callback = callback;
        request.read = true;
    }

    public void readArray(int[] addresses, int[] destination, int length, ArrayCallback callback) {
        resultArray = destination;
        arrayPos = 0;
        arrayLength = length;
        arrayCallback = callback;
        for (int i = 0; i < length; i++) {
            int address = addresses[i];
            shiftWrite();
            Request request;
            (request = tail).address = address;
            request.callback = arrayReadCallback;
            request.read = true;
        }
    }

    public void writeWord(int lo, int hi, int value) {
        shiftWrite();
        Request request;
        (request = tail).address = lo;
        request.read = false;
        request.payload = value & 0xff;
        shiftWrite();
        (request = tail).address = hi;
        request.read = false;
        request.payload = value >> 8;
    }

    public void readWord(int lo, int hi, Callback callback) {
        shiftWrite();
        finalCallback = callback;
        Request request;
        (request = tail).address = lo;
        request.callback = loReadWordCallback;
        request.read = true;
        shiftWrite();
        (request = tail).address = hi;
        request.callback = hiWordReadCallback;
        request.read = true;
    }

    public void clear() {
        head = tail;
        tail.address = -1;
        tail.next = tail;
    }

    public Request pop() {
        Request currentRequest;
        if ((currentRequest = head).address < 0) {
            if (currentRequest == tail) {
                core.step();
                f0Pin.opCode = true;
            } else {
                currentRequest = (head = currentRequest.next);
                if (currentRequest.address < 0) {
                    core.step();
                    f0Pin.opCode = true;
                }
            }
        }
        return currentRequest;
    }

    public String toString() {
        Request request = head.next;
        StringBuilder sb = new StringBuilder();
        while (request.address >= 0) {
            sb.append(request).append(";");
            if (request == tail) {
                break;
            } else {
                request = request.next;
            }
        }
        return sb.toString();
    }

    private void shiftWrite() {
        if (tail.address >= 0) {
            Request next = tail.next;
            if (next.address != -1) {
                Request newRequest = new Request();
                newRequest.next = next;
                tail = (tail.next = newRequest);
            } else {
                tail = next;
            }
        }
    }
}
