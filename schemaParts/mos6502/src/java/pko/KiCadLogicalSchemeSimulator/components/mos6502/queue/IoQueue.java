/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.mos6502.queue;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.F0Pin;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Cpu;

@SuppressWarnings("FieldMayBeFinal")
public class IoQueue {
    private final Cpu core;
    private final F0Pin f0Pin;
    public Request head;
    public Request tail;
    private ArrayCallback arrayCallback;
    private int lowByte;
    private int arrayPos;
    private int arrayLength;
    private Callback finalCallback;
    private int[] resultArray;
    private Callback loReadWordCallback = lowByte -> this.lowByte = lowByte;
    private Callback hiWordReadCallback = hiByte -> finalCallback.accept((hiByte << 8) | lowByte);
    private Callback arrayReadCallback = read -> {
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
        Request request = shiftWrite();
        request.read = false;
        request.payload = value;
        request.address = address;
    }

    public void read(int address, Callback callback) {
        Request request = shiftWrite();
        request.callback = callback;
        request.read = true;
        request.address = address;
    }

    public void readArray(int[] addresses, int[] destination, int length, ArrayCallback callback) {
        resultArray = destination;
        arrayPos = 0;
        arrayLength = length;
        arrayCallback = callback;
        if (length == 0) {
            callback.accept();
            return;
        }
        Request r = acquire();
        int i = 0;
        while (true) {
            r.callback = arrayReadCallback;
            r.read = true;
            r.address = addresses[i];
            if (++i == length) {
                return;
            }
            r = advance(r);
        }
    }

    public void writeWord(int lo, int hi, int value) {
        Request r = acquire();
        r.read = false;
        r.payload = value & 0xff;
        r.address = lo;
        r = advance(r);
        r.read = false;
        r.payload = value >> 8;
        r.address = hi;
    }

    public void readWord(int lo, int hi, Callback callback) {
        finalCallback = callback;
        Request request = acquire();
        request.callback = loReadWordCallback;
        request.read = true;
        request.address = lo;
        request = advance(request);
        request.callback = hiWordReadCallback;
        request.read = true;
        request.address = hi;
    }

    public void clear() {
        Request start = tail;
        Request r = start;
        do {
            r.address = -1;
            r = r.next;
        } while (r != start);
        head = tail;
    }

    public Request pop() {
        Request r = head;
        if (r.address >= 0) {
            return r;
        }
        if (r != tail) {
            r = r.next;
            head = r;
            if (r.address >= 0) {
                return r;
            }
        }
        core.step();
        f0Pin.opCode = true;
        return r;
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

    private Request shiftWrite() {
        Request lTail;
        if ((lTail = tail).address < 0) {
            return lTail;
        }
        Request next;
        if ((next = lTail.next).address < 0) {
            return (tail = next);
        }
        Request newRequest = (tail = lTail.next = new Request());
        newRequest.next = next;
        return newRequest;
    }

    private Request acquire() {
        Request t = tail;
        if (t.address < 0) {
            return t;
        }
        return advance(t);
    }

    private Request advance(Request current) {
        Request next = current.next;
        if (next.address < 0) {
            tail = next;
            return next;
        }
        Request created = new Request();
        created.next = next;
        current.next = created;
        tail = created;
        return created;
    }
}
