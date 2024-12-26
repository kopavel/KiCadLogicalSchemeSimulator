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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AsyncConsumer<T> implements Consumer<T>, AutoCloseable {
    private final List<Thread> consumerThreads = new ArrayList<>();
    private Thread asyncConsumerStop;
    private boolean run;
    private Slot<T> currentSlot;

    public AsyncConsumer(int size, int threads) {
        registerShutdown();
        Collection<Slot<T>> heads = createSlots(size, threads);
        for (Slot<T> head : heads) {
            consumerThreads.add(Thread.ofPlatform().daemon().start(() -> {
                try {
                    Slot<T> currentSlot = head;
                    run = true;
                    AtomicReference<T> current = currentSlot.payload;
                    T payload;
                    while (run) {
                        while ((payload = current.getOpaque()) != null) {
                            consume(payload);
                            current.setOpaque(null);
//                            VarHandle.releaseFence();
                            currentSlot = currentSlot.nextConsumer;
                            current = currentSlot.payload;
                        }
//                        VarHandle.acquireFence();
                        Thread.onSpinWait();
                    }
                } catch (Exception e) {
                    if (run) {
                        throw e;
                    }
                }
            }));
        }
    }

    @Override
    public void accept(T payload) {
        AtomicReference<T> current = currentSlot.payload;
        while (current.getOpaque() != null) {
//            VarHandle.acquireFence();
            Thread.onSpinWait();
        }
        current.setOpaque(payload);
//        VarHandle.releaseFence();
        currentSlot = currentSlot.nextProducer;
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

    private Collection<Slot<T>> createSlots(int size, int threads) {
        Map<Integer, Slot<T>> heads = new HashMap<>();
        Map<Integer, Slot<T>> tails = new HashMap<>();
        currentSlot = new Slot<>();
        Slot<T> producerHead = currentSlot;
        heads.put(0, producerHead);
        tails.put(0, producerHead);
        for (int i = 1; i < threads; i++) {
            Slot<T> consumerHead = new Slot<>();
            heads.put(i, consumerHead);
            tails.put(i, consumerHead);
            currentSlot.nextProducer = consumerHead;
            currentSlot = consumerHead;
        }
        for (int i = 1; i < size; i++) {
            for (Map.Entry<Integer, Slot<T>> head : heads.entrySet()) {
                Slot<T> consumerSlot = new Slot<>();
                currentSlot.nextProducer = consumerSlot;
                tails.get(head.getKey()).nextConsumer = consumerSlot;
                currentSlot = consumerSlot;
                tails.put(head.getKey(), consumerSlot);
            }
        }
        for (Map.Entry<Integer, Slot<T>> head : heads.entrySet()) {
            tails.get(head.getKey()).nextConsumer = head.getValue();
        }
        currentSlot.nextProducer = producerHead;
        currentSlot = producerHead;
        return heads.values();
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

    private static class Slot<T> {
        private final AtomicReference<T> payload = new AtomicReference<>();
        private Slot<T> nextProducer;
        private Slot<T> nextConsumer;
    }
}
