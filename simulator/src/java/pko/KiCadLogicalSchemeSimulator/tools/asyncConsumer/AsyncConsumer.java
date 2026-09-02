/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AsyncConsumer<T> extends AsyncConsumers<T> {
    protected AsyncConsumer(int size) {
        super(size, 1);
    }

    @Override
    public void accept(T payload) {
        do {
            Slot<T> slot = queue.writeSlot;
            AtomicReference<T> currentPayload = slot.payload;
            if (currentPayload.getOpaque() == null) {
                currentPayload.setOpaque(payload);
                queue.writeSlot = slot.nextSlot;
                return;
            }
        } while (run);
    }
}
