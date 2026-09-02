/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package pko.KiCadLogicalSchemeSimulator.components.mos6502.queue;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Utils;

public class Request {
    public int address = -1;
    public boolean read;
    public int payload;
    public Callback callback;
    public Request next;

    public String toString() {
        return read ? "R:" + Utils.wordToHex(address) : "W:" + Utils.byteToHex(payload) + "->" + Utils.wordToHex(address);
    }
}