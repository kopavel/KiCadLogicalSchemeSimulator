/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package pko.KiCadLogicalSchemeSimulator.tools.ringBuffers.blocking;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AsyncConsumer<T> implements Consumer<T>, Closeable {
    private final AtomicBoolean run = new AtomicBoolean();
    private Slot<T> currentSlot;

    public AsyncConsumer(int size) {
        currentSlot = new Slot<>();
        Slot<T> head = currentSlot;
        for (int i = 1; i < size; i++) {
            Slot<T> newSlot = new Slot<>();
            currentSlot.next = newSlot;
            currentSlot = newSlot;
        }
        currentSlot.next = head;
        Thread.ofPlatform().start(() -> {
            Slot<T> currentSlot = AsyncConsumer.this.currentSlot;
            run.setOpaque(true);
            while (run.getOpaque()) {
                AtomicReference<T> currentPayload = currentSlot.payload;
                T payload;
                while ((payload = currentPayload.getOpaque()) == null && run.getOpaque()) {
                    Thread.onSpinWait();
                }
                currentPayload.setRelease(null);
                currentSlot = currentSlot.next;
                if (payload != null) {
                    consume(payload);
                }
            }
        });
    }

    @Override
    public void accept(T payload) {
        AtomicReference<T> currentPayload = currentSlot.payload;
        while (currentPayload.getOpaque() != null) {
            Thread.onSpinWait();
        }
        currentPayload.setRelease(payload);
        currentSlot = currentSlot.next;
    }

    public abstract void consume(T payload);

    @Override
    public void close() {
        run.setRelease(false);
    }

    @Setter
    @Accessors(chain = true)
    private static class Slot<T> {
        private final AtomicReference<T> payload = new AtomicReference<>();
        private Slot<T> next;
    }
}
