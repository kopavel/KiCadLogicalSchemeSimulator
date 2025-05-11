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
package pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AsyncConsumers<T> implements Consumer<T>, AutoCloseable {
    final Queue<T> queue;
    private final Collection<Thread> consumerThreads = new ArrayList<>();
    boolean run;

    protected AsyncConsumers(int size, int threads) {
        registerShutdown();
        Slot<T>[] rings = createSlots(size, threads);
        if (threads == 0) {
            queue = null;
        } else {
            for (Slot<T> head : rings) {
                consumerThreads.add(Thread.ofPlatform().start(() -> {
                    try {
                        long sleepCounter = 0;
                        Slot<T> currentSlot = head;
                        run = true;
                        T payload;
                        while (run) {
                            while ((payload = currentSlot.payload.getOpaque()) != null) {
                                sleepCounter = 0;
                                AtomicReference<T> sharedPayload = currentSlot.payload;
                                consume(payload);
                                sharedPayload.setOpaque(null);
                                currentSlot = currentSlot.nextSlot;
                            }
                            if (sleepCounter > 1000000) {
                                //noinspection BusyWait
                                Thread.sleep(0, 1);
                            } else {
                                sleepCounter++;
                            }
                            Thread.onSpinWait();
                        }
                    } catch (Throwable e) {
                        if (run) {
                            throw new RuntimeException(e);
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
        try {
            for (Thread consumerThread : consumerThreads) {
                consumerThread.join();
            }
        } catch (InterruptedException e) {
            Log.error(AsyncConsumers.class, "Error waitng consumer for close", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(T payload) {
        do {
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
        } while (run);
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
/*
        Thread producerThread = Thread.currentThread();
        asyncConsumerStop = Thread.ofPlatform().name("AsyncConsumerStop").start(() -> {
            try {
                producerThread.join();
            } catch (InterruptedException ignore) {
            } finally {
                run = false;
            }
        });
*/
    }

    public static final class Queue<T> {
        public final Queue<T> next;
        public Slot<T> writeSlot;

        private Queue(Slot<T> writeSlot, Queue<T> next) {
            this.writeSlot = writeSlot;
            this.next = next;
        }
    }

    public static final class Slot<T> {
        public final AtomicReference<T> payload = new AtomicReference<>(null);
        public Slot<T> nextSlot;
    }
}
