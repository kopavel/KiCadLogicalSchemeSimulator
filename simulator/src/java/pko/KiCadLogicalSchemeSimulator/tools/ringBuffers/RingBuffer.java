/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public abstract class RingBuffer {
    public static final int DEFAULT_CAPACITY = 10000000;
    protected final int capacity;
    protected boolean isFull;
    protected int writePos;

    protected RingBuffer(int capacity) {
        this.capacity = capacity;
        writePos = -1;
    }

    public int available() {
        return isFull ? capacity : writePos + 1;
    }

    public void put(int element) {
        if (++writePos == capacity) {
            writePos = 0;
            isFull = true;
        }
    }

    public abstract IRingBufferSlice take(int offset, int amount);
}
