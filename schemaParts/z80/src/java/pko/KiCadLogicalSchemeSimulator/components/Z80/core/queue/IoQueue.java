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
package pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue;
@SuppressWarnings("FieldMayBeFinal")
public class IoQueue {
    public Request request;
    public Request write;
    Callback wordCallback;
    int lowByte;
    private Callback loReadWordCallback = lowByte -> this.lowByte = lowByte;
    private Callback hiWordReadCallback = hiByte -> wordCallback.accept((hiByte << 8) | lowByte);

    public IoQueue() {
        Request first = new Request();
        request = first;
        write = first;
        first.next = first;
    }

    public void writeWord(int address, int value) {
        Request r = shiftWrite(write);
        r.read = false;
        r.memory = true;
        r.payload = value & 0xff;
        r.address = address;
        r = shiftWrite(r);
        r.read = false;
        r.memory = true;
        r.payload = value >> 8;
        r.address = address + 1;
    }

    public void writeByte(int address, int value) {
        Request r = shiftWrite(write);
        r.read = false;
        r.memory = true;
        r.payload = value;
        r.address = address;
    }

    public void readByte(int address, Callback callback) {
        Request r = shiftWrite(write);
        r.callback = callback;
        r.read = true;
        r.memory = true;
        r.address = address;
    }

    public void readWord(int address, Callback callback) {
        wordCallback = callback;
        Request r = shiftWrite(write);
        r.callback = loReadWordCallback;
        r.read = true;
        r.memory = true;
        r.address = address;
        r = shiftWrite(r);
        r.callback = hiWordReadCallback;
        r.read = true;
        r.memory = true;
        r.address = address + 1;
    }

    public void ioRead(int address, Callback callback) {
        Request r = shiftWrite(write);
        r.callback = callback;
        r.read = true;
        r.memory = false;
        r.address = address;
    }

    public void ioWrite(int address, int value) {
        Request r = shiftWrite(write);
        r.read = false;
        r.memory = false;
        r.payload = value;
        r.address = address;
    }

    public void clear() {
        Request r = request;
        Request last = write;
        while (true) {
            r.address = -1;
            if (r == last) {
                break;
            }
            r = r.next;
        }
        request = last.next;
    }

    public void next() {
        Request current = request;
        current.address = -1;
        request = current.next;
    }

    private Request shiftWrite(Request current) {
        Request next = current.next;
        if (next.address >= 0) {
            Request created = new Request();
            created.next = next;
            current.next = created;
            next = created;
        }
        write = next;
        return next;
    }
}