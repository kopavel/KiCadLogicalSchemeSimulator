/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AsyncIntConsumers implements AutoCloseable {
    final Queue queue;
    private final Collection<Thread> consumerThreads = new ArrayList<>();
    boolean run;

    protected AsyncIntConsumers(int size, int threads) {
        registerShutdown();
        Slot[] rings = createSlots(size, threads);
        if (threads == 0) {
            queue = null;
        } else {
            for (Slot head : rings) {
                consumerThreads.add(Thread.ofPlatform().start(() -> {
                    try {
//                        long seepCounter = 0;
                        Slot currentSlot = head;
                        run = true;
                        int payload;
                        while (run) {
                            while ((payload = currentSlot.payload) != -1L) {
//                                seepCounter = 0;
                                consume(payload);
                                currentSlot.payload = -1;
                                currentSlot = currentSlot.nextSlot;
                            }
/*
                            if (seepCounter > 1000000) {
                                //noinspection BusyWait
                                Thread.sleep(0, 1);
                            } else {
                                seepCounter++;
                            }
*/
                            Thread.onSpinWait();
                        }
                    } catch (Throwable e) {
                        if (run) {
                            throw new RuntimeException(e);
                        }
                    }
                }));
            }
            Queue currentQueue = null;
            for (int i = threads - 1; i >= 0; i--) {
                currentQueue = new Queue(rings[i], currentQueue);
            }
            queue = currentQueue;
        }
    }

    public abstract void consume(int payload);

    @Override
    public synchronized void close() {
        run = false;
        VarHandle.releaseFence();
        try {
            for (Thread consumerThread : consumerThreads) {
                consumerThread.join();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void accept(int payload) {
        do {
            Queue currentQueue = queue;
            while (currentQueue != null) {
                Slot slot = currentQueue.writeSlot;
                if (slot.payload == -1L) {
                    slot.payload = payload;
                    currentQueue.writeSlot = slot.nextSlot;
                    return;
                }
                currentQueue = currentQueue.next;
            }
        } while (run);
    }

    private static Slot[] createSlots(int size, int threads) {
        List<Slot> rings = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Slot head = new Slot();
            rings.add(head);
        }
        for (Slot head : rings) {
            Slot currentSlot = head;
            for (int i = 1; i < size; i++) {
                Slot consumerSlot = new Slot();
                currentSlot.nextSlot = consumerSlot;
                currentSlot = consumerSlot;
            }
            currentSlot.nextSlot = head;
        }
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

    public final static class Queue {
        public final Queue next;
        public Slot writeSlot;

        private Queue(Slot writeSlot, Queue next) {
            this.writeSlot = writeSlot;
            this.next = next;
        }
    }

    public static class Slot {
        public int payload = -1;
        public Slot nextSlot;
    }
}
