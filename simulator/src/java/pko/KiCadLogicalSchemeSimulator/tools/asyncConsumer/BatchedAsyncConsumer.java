/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;

public abstract class BatchedAsyncConsumer implements AutoCloseable {
    private final int maxIndex;
    private final Collection<Thread> consumerThreads = new ArrayList<>();
    private volatile boolean run = true;
    private int pos;
    private Slot writeSlot;
    private int[] writePayload;

    protected BatchedAsyncConsumer(int batches, int batchSize) {
        pos = maxIndex = (batchSize - 1);
        registerShutdown();
        Slot ring = createRings(batches);
        consumerThreads.add(Thread.ofPlatform().start(() -> {
            try {
                Slot currentSlot = ring;
                while (run) {
                    while (currentSlot.full) {
                        int[] ints = currentSlot.payload;
                        for (int i = ints.length - 1; i >= 0; i--) {
                            consume(ints[i]);
                        }
                        currentSlot.full = false;
                        currentSlot = currentSlot.nextSlot;
                    }
                    Thread.onSpinWait();
                }
            } catch (Throwable e) {
                if (run) {
                    throw new RuntimeException(e);
                }
            }
        }));
        writeSlot = ring;
        writePayload = writeSlot.payload;
        VarHandle.releaseFence();
    }

    public abstract void consume(int payload);

    @Override
    public void close() {
        run = false;
        //noinspection SynchronizeOnThis
        synchronized (this) {
            try {
                for (Thread consumerThread : consumerThreads) {
                    consumerThread.join();
                }
            } catch (InterruptedException e) {
                Log.error(BatchedAsyncConsumer.class, "Error waiting consumer for close", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void accept(int payload) {
        if (pos == 0) {
            writePayload[0] = payload;
            pos = maxIndex;
            writeSlot.full = true;
            if ((writeSlot = writeSlot.nextSlot).full) {
//                long wait=0;
                while (writeSlot.full) {
                    Thread.onSpinWait();
//                    wait++;
                }
//                System.out.println("wait time:"+wait);
            }
            writePayload = writeSlot.payload;
        } else {
            writePayload[pos--] = payload;
        }
    }

    private Slot createRings(int size) {
        Slot head = new Slot(maxIndex + 1);
        Slot currentSlot = head;
        for (int i = 1; i < size; i++) {
            Slot consumerSlot = new Slot(maxIndex + 1);
            currentSlot.nextSlot = consumerSlot;
            currentSlot = consumerSlot;
        }
        currentSlot.nextSlot = head;
        VarHandle.releaseFence();
        return head;
    }

    private void registerShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    static class Slot {
        private final int[] payload;
        private Slot nextSlot;
        private volatile boolean full;

        Slot(int size) {
            payload = new int[size];
        }
    }
}
