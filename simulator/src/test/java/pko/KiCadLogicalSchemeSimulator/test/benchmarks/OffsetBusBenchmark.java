/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.benchmarks;
import org.openjdk.jmh.annotations.*;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class OffsetBusBenchmark {
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1, time = 15)
    @Measurement(iterations = 3, time = 15)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void optimiser(StateForOptimiser state) {
        BenchmarkRunner.doWork(state.out);
    }

    @State(Scope.Thread)
    public static class StateForOptimiser {
        Bus out;

        @Setup(Level.Trial)
        public void setUp() {
            SchemaPart testPart = new SchemaPart("Optimiser", "") {
                @Override
                public void initOuts() {
                }
            };
            out = new OutBus("test", testPart, 5);
            for (int i = 0; i < 5; i++) {
                ((OutBus) out).addDestination(testPart.addInBus("in" + i, 7), 0b11111, (byte) 2);
            }
            out = out.getOptimised(null);
        }
    }
}
