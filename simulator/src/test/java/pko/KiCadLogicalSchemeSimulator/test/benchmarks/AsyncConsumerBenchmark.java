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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pko.KiCadLogicalSchemeSimulator.tools.ringBuffers.blocking.AsyncConsumer;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

@State(Scope.Benchmark)
public class AsyncConsumerBenchmark {
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder().include(AsyncConsumerBenchmark.class.getSimpleName()).warmupIterations(2).warmupTime(TimeValue.seconds(3))
                                              .measurementIterations(3).measurementTime(TimeValue.seconds(5))
                                              .forks(1)
                                              .build();
        new Runner(options).run();
/*
        AsyncConsumerBenchmark atomicReferenceBenchmark = new AsyncConsumerBenchmark();
        for (int i = 0; i < 1000; i++) {
            atomicReferenceBenchmark.counter();
        }
*/
    }

    @Benchmark
    public void counter() throws IOException {
//                     Consumer<AsyncConsumer.Slot> consumer = this::process;
        Consumer<Long> consumer1 = new AsyncConsumer<>(100) {
            @Override
            public void consume(Long payload) {
//                process(payload);
            }
        };
//        Consumer<Long> consumer2 = new AsyncConsumer<>(100) {
//            @Override
//            public void consume(Long payload) {
//                process(payload);
//            }
//        };
        for (int i = 0; i < 200000; i++) {
            Long payload1 = (long) i;
//            Long  payload2 = (long) i;
//            Long  payload3 = (long) i;
            consumer1.accept(payload1);
//            consumer2.accept(payload3);
//            process(payload2);
            consumer1.accept(payload1);
//            consumer2.accept(payload3);
//            process(payload2);
            consumer1.accept(payload1);
//            consumer2.accept(payload3);
//            process(payload2);
            consumer1.accept(payload1);
//            consumer2.accept(payload3);
//            process(payload2);
            consumer1.accept(payload1);
//            consumer2.accept(payload3);
//            process(payload2);
        }
        //noinspection ConstantValue
        if (consumer1 instanceof Closeable) {
            ((Closeable) consumer1).close();
        }
        //noinspection ConstantValue
//        if (consumer2 instanceof Closeable) {
//            ((Closeable) consumer2).close();
//        }
    }

    private void process(Long payload) {
        for (int i = 0; i < 3; i++) {
            payload = Thread.currentThread().threadId();
        }
    }
}
