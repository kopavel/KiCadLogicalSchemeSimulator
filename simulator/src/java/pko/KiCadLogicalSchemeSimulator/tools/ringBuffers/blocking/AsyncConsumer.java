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
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AsyncConsumer<T> implements Consumer<T>, AutoCloseable {
    private final List<Thread> consumerThreads = new ArrayList<>();
    private final Queue<T> queue;
    private final int threads;
    private Thread asyncConsumerStop;
    private boolean run;

    public AsyncConsumer(int size, int threads) {
        this.threads = threads;
        registerShutdown();
        Slot<T>[] rings = createSlots(size, threads);
        if (threads == 0) {
            queue = null;
        } else {
            for (Slot<T> head : rings) {
                consumerThreads.add(Thread.ofPlatform().start(() -> {
                    try {
                        Slot<T> currentSlot = head;
                        run = true;
                        T payload;
                        while (run) {
                            while ((payload = currentSlot.payload.getOpaque()) != null) {
                                final AtomicReference<T> sharedPayload = currentSlot.payload;
                                consume(payload);
                                sharedPayload.setOpaque(null);
                                currentSlot = currentSlot.nextSlot;
                            }
                            Thread.onSpinWait();
                        }
                    } catch (Exception e) {
                        if (run) {
                            throw e;
                        }
                    }
                }));
            }
            Queue<T> currentQueue = null;
            for (int i = threads - 1; i >= 0; i--) {
                currentQueue = new Queue<>(rings[i], currentQueue);
            }
            queue = currentQueue;
        }
    }

    public abstract void consume(T payload);

    @Override
    public synchronized void close() {
        run = false;
        VarHandle.releaseFence();
        asyncConsumerStop.interrupt();
        try {
            asyncConsumerStop.join();
            for (Thread consumerThread : consumerThreads) {
                consumerThread.join();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(T payload) {
        if (threads == 1) {
            final Slot<T> slot = queue.writeSlot;
            final AtomicReference<T> currentPayload = slot.payload;
            if (currentPayload.getOpaque() == null) {
                currentPayload.setOpaque(payload);
                queue.writeSlot = slot.nextSlot;
                return;
            }
        } else if (threads > 1) {
            Queue<T> currentQueue = queue;
            while (currentQueue != null) {
                final Slot<T> slot = currentQueue.writeSlot;
                final AtomicReference<T> currentPayload = slot.payload;
                if (currentPayload.getOpaque() == null) {
                    currentPayload.setOpaque(payload);
                    currentQueue.writeSlot = slot.nextSlot;
                    return;
                }
                currentQueue = currentQueue.next;
            }
        }
        consume(payload);
    }

    private Slot<T>[] createSlots(int size, int threads) {
        List<Slot<T>> rings = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Slot<T> head = new Slot<>();
            rings.add(head);
        }
        for (Slot<T> head : rings) {
            Slot<T> currentSlot = head;
            for (int i = 1; i < size; i++) {
                Slot<T> consumerSlot = new Slot<>();
                currentSlot.nextSlot = consumerSlot;
                currentSlot = consumerSlot;
            }
            currentSlot.nextSlot = head;
        }
        //noinspection unchecked
        return rings.toArray(new Slot[0]);
    }

    private void registerShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        Thread producerThread = Thread.currentThread();
        asyncConsumerStop = Thread.ofPlatform().name("AsyncConsumerStop").start(() -> {
            try {
                producerThread.join();
            } catch (InterruptedException ignore) {
            } finally {
                run = false;
            }
        });
    }

    private static class Queue<T> {
        final private Queue<T> next;
        private Slot<T> writeSlot;

        private Queue(Slot<T> writeSlot, Queue<T> next) {
            this.writeSlot = writeSlot;
            this.next = next;
        }
    }

    private static class Slot<T> {
        private final AtomicReference<T> payload = new AtomicReference<>(null);
        private Slot<T> nextSlot;
    }
}
