/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.benchmarks;
import org.openjdk.jmh.annotations.*;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.bus.BusToWiresAdapter;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class BusToWireBenchmark {
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
        BusToWiresAdapter out;

        @Setup(Level.Trial)
        public void setUp() {
            SchemaPart testPart = new SchemaPart("Optimiser", "") {
                @Override
                public void initOuts() {
                }
            };
            /* fix me if needed
            out = new BusToWiresAdapter(new OutBus("test", testPart, 4), 2);
//            for (int i = 0; i < 5; i++) {
            out.addDestination(testPart.addInPin("IN"));
*/
//            }
            out = out.getOptimised(null);
        }
    }
}
