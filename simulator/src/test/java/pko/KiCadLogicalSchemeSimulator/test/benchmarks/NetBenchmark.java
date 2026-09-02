/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.benchmarks;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class NetBenchmark extends NetTester {
    private final static boolean TEST = true;
    private Bus out;

    static void main(String[] args) throws Throwable {
        if (TEST) {
            Options options = new OptionsBuilder()//
                                                  .include(NetBenchmark.class.getSimpleName())
                                                  .warmupIterations(5)
                                                  .warmupTime(TimeValue.seconds(3))
                                                  .measurementIterations(5)
                                                  .measurementTime(TimeValue.seconds(3))
                                                  .mode(Mode.Throughput)
                                                  .timeUnit(TimeUnit.SECONDS)
                                                  .forks(1)
                                                  .build();
            new Runner(options).run();
        } else {
            NetBenchmark benchmark = new NetBenchmark();
            benchmark.setup();
            for (int i = 0; i < 1000; i++) {
                benchmark.netBench();
            }
        }
    }

    @Setup
    public void setup() throws Exception {
        loadNet();
        out = outBus("OutBus");
    }

    @Benchmark
    public void netBench() {
        for (int i = 0; i < 10000000; i++) {
            out.setState(0);
            out.setState(0xff);
            out.setHiImpedance();
            out.setState(0xff);
            out.setState(0);
            out.setState(0);
            out.setState(0xff);
            out.setHiImpedance();
            out.setState(0xff);
            out.setState(0);
        }
    }

    protected String getNetFilePath() {
        return "simulator/src/test/resources/netBench.net";
    }

    @Override
    protected String getRootPath() {
        return ".";
    }
}
