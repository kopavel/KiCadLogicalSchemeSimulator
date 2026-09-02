/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.benchmarks;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.io.IOException;

public class BenchmarkRunner {
    static void main(String[] args) throws IOException {
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
