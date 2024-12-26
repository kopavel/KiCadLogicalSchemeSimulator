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
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

public abstract class DisruptorConsumer<T> implements Consumer<T>, AutoCloseable {
    private final RingBuffer<Slot<T>> ringBuffer;
    private final Disruptor<Slot<T>> disruptor;

    public DisruptorConsumer(int size, int threads) {
        disruptor = new Disruptor<>(Slot::new,                     // Event factory
                size,                          // Buffer size
                new SimpleThreadFactory(), // Thread factory
                ProducerType.SINGLE,           // Single producer
                new BusySpinWaitStrategy()     // Wait strategy
        );
        // Add multiple event handlers (one per thread)
        EventHandler<Slot<T>>[] handlers = new EventHandler[threads];
        Arrays.fill(handlers, (EventHandler<Slot<T>>) (event, sequence, endOfBatch) -> consume(event.payload));
        disruptor.handleEventsWith(handlers);
        // Start the disruptor
        ringBuffer = disruptor.start();
    }

    @Override
    public void accept(T payload) {
        long sequence = ringBuffer.next(); // Claim the next sequence
        Slot<T> event = ringBuffer.get(sequence); // Get the event
        event.payload = payload; // Set data
        ringBuffer.publish(sequence); // Publish the event
    }

    public abstract void consume(T payload);

    @Override
    public void close() {
        disruptor.shutdown();
    }

    private static class Slot<T> {
        private T payload;
    }

    static class SimpleThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return Thread.ofPlatform().unstarted(r);
        }
    }
}
