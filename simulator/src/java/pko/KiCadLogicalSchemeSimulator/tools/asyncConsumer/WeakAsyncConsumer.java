/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer;
import java.util.concurrent.atomic.AtomicReference;

public abstract class WeakAsyncConsumer<T> extends AsyncConsumers<T> {
    protected WeakAsyncConsumer(int size) {
        super(size, 1);
    }

    @Override
    public void accept(T payload) {
        Slot<T> slot = queue.writeSlot;
        AtomicReference<T> currentPayload = slot.payload;
        if (currentPayload.getOpaque() == null) {
            currentPayload.setOpaque(payload);
            queue.writeSlot = slot.nextSlot;
            return;
        }
        consume(payload);
    }
}
