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
package pko.KiCadLogicalSchemeSimulator.test.benchmarks;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer.BatchedAsyncConsumer;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class AsyncConsumerBenchmark {
    public static final int THREADS = 1;
    public static final int ASYNC_BUF_SIZE = 4;
    public static final int PAYLOAD = 1000;
    public static final boolean TEST = true;
    final int payload = 10;
    private int cycles = 100000;
    BatchedAsyncConsumer asyncConsumer;
    private Blackhole blackhole;

    public static void main(String[] args) throws Throwable {
        if (TEST) {
            Options options = new OptionsBuilder()//
                                                  .include(AsyncConsumerBenchmark.class.getSimpleName())
                                                  .warmupIterations(5)
                                                  .warmupTime(TimeValue.seconds(2))
                                                  .measurementIterations(10)
                                                  .measurementTime(TimeValue.seconds(3))
                                                  .mode(Mode.Throughput)
                                                  .timeUnit(TimeUnit.SECONDS)
                                                  .forks(1)
                                                  .build();
            new Runner(options).run();
        } else {
            AsyncConsumerBenchmark atomicReferenceBenchmark = new AsyncConsumerBenchmark();
            atomicReferenceBenchmark.cycles = Integer.MAX_VALUE;
            try {
                atomicReferenceBenchmark.setup(null);
                for (int i = 0; i < 10000; i++) {
                    atomicReferenceBenchmark.asyncConsumer();
                }
            } finally {
                atomicReferenceBenchmark.tearDown();
            }
        }
    }

    @Setup
    public void setup(Blackhole blackhole) throws Exception {
        this.blackhole = blackhole;
/*
        if (THREADS == 0) {
            asyncConsumer = this::process;
        } else if (THREADS == 1) {
*/
        asyncConsumer = new BatchedAsyncConsumer(4,512) {
            @Override
            public void consume(int payload) {
                    blackhole.consume(payload);
//                process(payload);
            }
        };
/*
        } else {
            asyncConsumer = new AsyncConsumers<>(ASYNC_BUF_SIZE, THREADS) {
                @Override
                public void consume(Long payload) {
                    process(payload);
                }
            };
*/
//        }
    }

    @TearDown
    public void tearDown() throws Exception {
        if (asyncConsumer instanceof AutoCloseable autoCloseable) {
            autoCloseable.close();
        }
    }

    @Benchmark()
    public void asyncConsumer() {
        for (int i = 0; i < cycles; i++) {
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
            asyncConsumer.accept(i);
/*
            process(true);
            process(true);
            process(true);
            process(true);
            process(true);
            process(true);
            process(true);
            process(true);
            process(true);
            process(true);
*/
        }
    }

    private void process(int payload) {
        int accumulator = payload;
        for (int i = 0; i < PAYLOAD; i++) {
            accumulator++;
        }
        if (blackhole != null) {
            blackhole.consume(accumulator);
        }
    }
}
