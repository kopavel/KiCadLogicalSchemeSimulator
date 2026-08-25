package pko.KiCadLogicalSchemeSimulator.components.BUF.test

import pko.KiCadLogicalSchemeSimulator.components.BUF.BufferSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class LatchTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(
                new BufferSpi(),
                "size=8;latch;reverse",
                ["WR", "OE", "D"],
                ["Q"]
        )
    }

    @Unroll("#optimized | WR:#wr, OE:#oe, D:#d -> Q:#q")
    def "Latch"() {
        given:
        useChip(optimized)
        when:
        setInputs(wr, oe, d)

        then:
        checkOutputs(q)

        where:
        optimized << [RAW, OPT]

        combined:
        wr | oe | d || q
        1 | 1 | 10 || 'h'
        0 | 1 | 10 || 'h'
        1 | 1 | 10 || 'h'
        1 | 0 | 10 || 10
        1 | 1 | 10 || 'h'
        1 | 1 | 20 || 'h'
        1 | 0 | 20 || 10
        0 | 0 | 20 || 20
    }
}


