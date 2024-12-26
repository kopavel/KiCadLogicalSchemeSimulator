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
package pko.KiCadLogicalSchemeSimulator.test.benchmarks.consumers;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import pko.KiCadLogicalSchemeSimulator.tools.ringBuffers.blocking.DisruptorConsumer;

import static pko.KiCadLogicalSchemeSimulator.test.benchmarks.consumers.AsyncConsumerBenchmark.DISRUPTOR_BUF_SIZE;
import static pko.KiCadLogicalSchemeSimulator.test.benchmarks.consumers.AsyncConsumerBenchmark.PAYLOAD;
import static pko.KiCadLogicalSchemeSimulator.test.benchmarks.consumers.AsyncConsumerBenchmark.THREADS;

@State(Scope.Benchmark)
public class DisruptorConsumerBenchmark {
    DisruptorConsumer<Long> disruptorConsumer;
    Long payload = 10L;
    private Blackhole blackhole;

    @Setup
    public void setup(Blackhole blackhole) {
        this.blackhole = blackhole;
        disruptorConsumer = new DisruptorConsumer<>(DISRUPTOR_BUF_SIZE, THREADS) {
            @Override
            public void consume(Long payload) {
                process(payload);
            }
        };
    }

    @TearDown
    public void tearDown() throws Exception {
        disruptorConsumer.close();
    }

    @Benchmark()
    public void disruptorConsumer() throws Exception {
        for (int i = 0; i < 100000; i++) {
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
            disruptorConsumer.accept(payload);
        }
    }

    private void process(Long payload) {
        long acumulator = 0;
        for (int i = 0; i < PAYLOAD; i++) {
            acumulator++;
        }
        blackhole.consume(acumulator);
    }
}
