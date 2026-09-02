/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
    private Pin out1;
    private Pin out2;

    static void main(String[] args) throws Throwable {
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
        for (int i = 0; i < 10000000; i++) {
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
