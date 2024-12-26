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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pko.KiCadLogicalSchemeSimulator.tools.ringBuffers.blocking.AsyncConsumer;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class AsyncConsumerBenchmark {
    public static final int THREADS = 1;
    public static final int DISRUPTOR_BUF_SIZE = 1 << 10;
    public static final int ASYNC_BUF_SIZE = 1000;
    public static final int PAYLOAD = 10000;
    AsyncConsumer<Long> asyncConsumer;
    Long payload = 10L;
    private Blackhole blackhole;

    public static void main(String[] args) throws Throwable {
        Options options = new OptionsBuilder()//
//                                              .include(AsyncConsumerBenchmark.class.getSimpleName())
                                              .include(ZigConsumerBenchmark.class.getSimpleName())
//                                              .include(DisruptorConsumerBenchmark.class.getSimpleName())
                                              .warmupIterations(3)
                                              .warmupTime(TimeValue.seconds(1))
                                              .measurementIterations(5)
                                              .measurementTime(TimeValue.seconds(1))
                                              .mode(Mode.Throughput)
                                              .timeUnit(TimeUnit.SECONDS)
                                              .forks(1)
                                              .build();
        new Runner(options).run();
//        ZigConsumerBenchmark atomicReferenceBenchmark = new ZigConsumerBenchmark();
//        atomicReferenceBenchmark.setup(null);
//        for (int i = 0; i < 10000; i++) {
//        atomicReferenceBenchmark.asyncConsumer();
//        }
    }

    @Setup
    public void setup(Blackhole blackhole) {
        this.blackhole = blackhole;
        asyncConsumer = new AsyncConsumer<>(ASYNC_BUF_SIZE, THREADS) {
            @Override
            public void consume(Long payload) {
                //process(payload);
            }
        };
    }

    @TearDown
    public void tearDown() throws Exception {
        asyncConsumer.close();
    }

    @Benchmark()
    public void asyncConsumer() throws Exception {
        for (int i = 0; i < 100000; i++) {
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
            asyncConsumer.accept(payload);
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
