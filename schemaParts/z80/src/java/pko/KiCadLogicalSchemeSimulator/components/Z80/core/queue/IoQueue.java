/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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