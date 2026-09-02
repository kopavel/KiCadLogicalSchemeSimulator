/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class ByteBufferSlice implements IRingBufferSlice {
    private final byte[] slice;
    private int pos = -1;

    public ByteBufferSlice(byte[] slice) {
        this.slice = slice;
    }

    @Override
    public int size() {
        return slice.length;
    }

    @Override
    public int next() {
        return slice[++pos];
    }

    @Override
    public void skip() {
        pos++;
    }

    @Override
    public int peek() {
        return slice[pos];
    }
}
