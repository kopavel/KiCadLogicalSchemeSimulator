/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer;
import java.util.concurrent.atomic.AtomicReference;

public abstract class WeakAsyncConsumers<T> extends AsyncConsumers<T> {
    protected WeakAsyncConsumers(int size, int threads) {
        super(size, threads);
    }

    @Override
    public void accept(T payload) {
        Queue<T> currentQueue = queue;
        while (currentQueue != null) {
            Slot<T> slot = currentQueue.writeSlot;
            AtomicReference<T> currentPayload = slot.payload;
            if (currentPayload.getOpaque() == null) {
                currentPayload.setOpaque(payload);
                currentQueue.writeSlot = slot.nextSlot;
                return;
            }
            currentQueue = currentQueue.next;
        }
        consume(payload);
    }
}
