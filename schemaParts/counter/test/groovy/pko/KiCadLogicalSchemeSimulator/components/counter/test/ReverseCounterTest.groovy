package pko.KiCadLogicalSchemeSimulator.components.counter.test

import pko.KiCadLogicalSchemeSimulator.components.counter.CounterSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class ReverseCounterTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(new CounterSpi(),
                "size=4;reverse",
                ["C"],
                ["Q"]);
    }

    @Unroll("#optimized | C:#c -> Q:#q")
    def "ReverseCounter"() {
        given:
        useChip(optimized)

        when:
        setInputs(c)

        then:
        checkOutputs(q)

        where:
        optimized << [RAW, OPT]

        combined:
        [c, q] << ([[1, 0]] + (1..15).collectMany { q -> [[0, q], [1, q]] } + [[0, 0], [1, 0]])
    }
}
