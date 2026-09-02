/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.benchmarks;
import org.openjdk.jmh.annotations.*;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.TriStateOutPin;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class OutPinBenchmark {
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
        Pin out;

        @Setup(Level.Trial)
        public void setUp() {
            SchemaPart testPart = new SchemaPart("Optimiser", "") {
                @Override
                public void initOuts() {
                }
            };
            out = new TriStateOutPin("test", testPart);
            for (int i = 0; i < 5; i++) {
                ((OutPin) out).addDestination(testPart.addInPin("in" + i));
            }
//            out = out.getOptimised(null);
        }
    }
}
