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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.io.IOException;

public class BenchmarkRunner {
    public static void main(String[] args) throws IOException {
        String[] benchmarks = {//
//                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.OutPinBenchmark.javac",//
//                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.OutBusBenchmark.javac",//
//                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.OutPinBenchmark.optimiser",//
//                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.OutBusBenchmark.optimiser",//
//                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.MaskGroupBenchmark.optimiser",//
//                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.OffsetBusBenchmark.optimiser",//
//                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.WireToBusBenchmark.optimiser",//
                "pko.KiCadLogicalSchemeSimulator.test.benchmarks.BusToWireBenchmark.optimiser"//
        };
        org.openjdk.jmh.Main.main(benchmarks);
    }

    // <editor-fold desc="pin state iterator">
    public static void doWork(Pin out) {
        for (int i = 0; i < 10000; i++) {
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
            out.setHi();
            out.setLo();
            out.setHiImpedance();
            out.setHi();
            out.setLo();
        }
    }
// </editor-fold>

    // <editor-fold desc="bus state iterator">
    public static void doWork(Bus out) {
        for (int i = 0; i < 10000; i++) {
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
            out.setState(1);
            out.setState(2);
            out.setHiImpedance();
            out.setState(1);
            out.setState(2);
        }
    }
// </editor-fold>
}
