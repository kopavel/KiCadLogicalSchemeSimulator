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
public class IoQueue {
    public Request request;
    public Request write;
    int lowByte;
    private final Callback loReadWordCallback = lowByte -> {
        this.lowByte = lowByte;
    };
    Callback wordCallback;
    private final Callback hiWordReadCallback = (hiByte) -> wordCallback.accept((hiByte << 8) | lowByte);

    public IoQueue() {
        write = new Request();
        request = write;
        write.next = write;
    }

    public void writeWord(int address, int value) {
        shiftWrite();
        write.address = address;
        write.read = false;
        write.memory = true;
        write.payload = value & 0xff;
        shiftWrite();
        write.address = address + 1;
        write.read = false;
        write.memory = true;
        write.payload = value >> 8;
    }

    public void writeByte(int address, int value) {
        shiftWrite();
        write.address = address;
        write.read = false;
        write.memory = true;
        write.payload = value;
    }

    public void readByte(int address, Callback callback) {
        shiftWrite();
        write.address = address;
        write.read = true;
        write.memory = true;
        write.callback = callback;
    }

    public void readWord(int address, Callback callback) {
        shiftWrite();
        wordCallback = callback;
        write.address = address;
        write.read = true;
        write.memory = true;
        write.callback = loReadWordCallback;
        shiftWrite();
        write.address = address + 1;
        write.read = true;
        write.memory = true;
        write.callback = hiWordReadCallback;
    }

    public void ioRead(int address, Callback callback) {
        shiftWrite();
        write.address = address;
        write.read = true;
        write.memory = false;
        write.callback = callback;
    }

    public void ioWrite(int address, int value) {
        shiftWrite();
        write.address = address;
        write.read = false;
        write.memory = false;
        write.payload = value;
    }

    public void clear() {
        request = write;
        write.address = -1;
        write.next = write;
    }

    public void next() {
        request.address = -1;
        request = request.next;
    }

    private void shiftWrite() {
        if (write.next.address != -1) {
            Request next = write.next;
            Request newRequest = new Request();
            newRequest.next = next;
            write.next = newRequest;
            write = newRequest;
        } else {
            write = write.next;
        }
    }
}
