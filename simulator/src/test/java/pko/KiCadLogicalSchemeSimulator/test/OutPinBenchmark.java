package pko.KiCadLogicalSchemeSimulator.test;
import org.openjdk.jmh.annotations.*;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class OutPinBenchmark {
    public static void main(String[] args) throws IOException {
        String[] benchmarks = {//
//                "pko.KiCadLogicalSchemeSimulator.test.OutPinBenchmark.iterator",//
//                "pko.KiCadLogicalSchemeSimulator.test.OutPinBenchmark.javac",//
                "pko.KiCadLogicalSchemeSimulator.test.OutPinBenchmark.optimiser"//
        };
        org.openjdk.jmh.Main.main(benchmarks);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3, time = 15)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void iterator(StateForIterator state) {
        doWork(state.out);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3, time = 15)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void javac(StateForJavac state) {
        doWork(state.out);
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3, time = 15)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void optimiser(StateForOptimiser state) {
        doWork(state.out);
    }

    private static void doWork(Pin out) {
        for (int i = 0; i < 100; i++) {
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
            out.state = true;
            out.setState(true);
            out.state = false;
            out.setState(false);
        }
    }

    @State(Scope.Thread)
    public static class StateForIterator {
        OutPin out;

        @Setup(Level.Trial)
        public void setUp() {
            SchemaPart testPart = new SchemaPart("iterator", "") {
                @Override
                public void initOuts() {
                }
            };
            out = new OutPin("test", testPart);
            for (int i = 0; i < 5; i++) {
                out.addDestination(testPart.addInPin("in" + i));
            }
        }
    }

    @State(Scope.Thread)
    public static class StateForJavac {
        Pin out;

        @Setup(Level.Trial)
        public void setUp() {
            SchemaPart testPart = new SchemaPart("javac", "") {
                @Override
                public void initOuts() {
                }
            };
            out = new OutPin("test", testPart);
            for (int i = 0; i < 5; i++) {
                ((OutPin) out).addDestination(testPart.addInPin("in" + i));
            }
            out = out.getOptimised();
        }
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
            out = new OutPin("test", testPart);
            for (int i = 0; i < 5; i++) {
                ((OutPin) out).addDestination(testPart.addInPin("in" + i));
            }
            out = out.getOptimised();
        }
    }
}
