/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer;
public abstract class AsyncLongConsumer extends AsyncIntConsumers {
    protected AsyncLongConsumer(int size) {
        super(size, 1);
    }

    public void accept(int payload) {
        do {
            Slot slot = queue.writeSlot;
            if (slot.payload == -1L) {
                slot.payload = payload;
                queue.writeSlot = slot.nextSlot;
                return;
            }
        } while (run);
    }
}
