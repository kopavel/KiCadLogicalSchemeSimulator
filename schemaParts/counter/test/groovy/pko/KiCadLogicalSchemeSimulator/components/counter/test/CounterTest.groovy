package pko.KiCadLogicalSchemeSimulator.components.counter.test

import pko.KiCadLogicalSchemeSimulator.components.counter.CounterSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class CounterTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(
                new CounterSpi(),
                "size=4",
                ["C", "R"],
                ["Q"]
        );
    }

    @Unroll("#optimized | C:#c, R:#r -> Q:#q")
    def "Counter"() {
        given:
        useChip(optimized)

        when:
        setInputs(c, r)

        then:
        checkOutputs(q)

        where:
        optimized << [RAW, OPT]

        combined:
        // @formatter:off
        c | r  || q
        0 | 0  || 0
        1 | 0  || 1
        0 | 0  || 1
        0 | 1  || 0
        1 | 1  || 0
        0 | 1  || 0
        0 | 0  || 0
        1 | 0  || 1
        // @formatter:on
    }
}
