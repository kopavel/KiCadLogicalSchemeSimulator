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
