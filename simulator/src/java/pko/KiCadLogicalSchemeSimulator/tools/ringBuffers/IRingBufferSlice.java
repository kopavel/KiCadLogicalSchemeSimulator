/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public interface IRingBufferSlice {
    int size();
    int next();
    void skip();
    int peek();
}
