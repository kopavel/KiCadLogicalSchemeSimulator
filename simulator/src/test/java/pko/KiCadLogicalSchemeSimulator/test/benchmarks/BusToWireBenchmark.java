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
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.bus.BusToWiresAdapter;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class BusToWireBenchmark {
    @Benchmark
    @Fork(value = 1)
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
