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
package pko.KiCadLogicalSchemeSimulator.components.OR.test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class Benchmark extends NetTester {
    private final static boolean TEST = true;
    private final static int cycles = 100000000;
    private Pin out1;
    private Pin out2;

    public static void main(String[] args) throws Throwable {
        if (TEST) {
            Options options = new OptionsBuilder()//
                                                  .include(Benchmark.class.getSimpleName())
                                                  .warmupIterations(10)
                                                  .warmupTime(TimeValue.seconds(3))
                                                  .measurementIterations(10)
                                                  .measurementTime(TimeValue.seconds(3))
                                                  .mode(Mode.Throughput)
                                                  .timeUnit(TimeUnit.SECONDS)
                                                  .forks(1)
                                                  .build();
            new Runner(options).run();
        } else {
            Benchmark benchmark = new Benchmark();
            benchmark.setup();
            for (int i = 0; i < 1000; i++) {
                benchmark.bench();
            }
        }
    }

    @Setup
    public void setup() throws Exception {
        loadNet();
        out1 = outPin("OutPin1");
        out2 = outPin("OutPin2");
    }

    @org.openjdk.jmh.annotations.Benchmark
    @Fork(1)
    @Warmup(iterations = 3, time = 10)
    @Measurement(iterations = 3, time = 10)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void bench() {
        for (int i = 0; i < cycles; i++) {
            out1.setHi();
            out2.setHi();
            out1.setLo();
            out2.setLo();
            out1.setHi();
            out2.setHi();
            out1.setLo();
            out2.setLo();
            out1.setHi();
            out1.setLo();
        }
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/Or.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }
}
