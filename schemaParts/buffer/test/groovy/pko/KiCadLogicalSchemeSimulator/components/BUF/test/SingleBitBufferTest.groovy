package pko.KiCadLogicalSchemeSimulator.components.BUF.test


import pko.KiCadLogicalSchemeSimulator.components.BUF.singleBit.SingleBitBufferSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class SingleBitBufferTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(new SingleBitBufferSpi(),
                null,
                ["CS", "D"],
                ["Q"])
    }

    @Unroll("#optimise | CS:#cs, D:#d -> Q:#q")
    def "Buffer"() {
        given:
        useChip(optimise)

        when:
        setInputs(cs, d)

        then:
        checkOutputs(q)

        where:
        optimise << [RAW, OPT]

        combined:
        // @formatter:off
        cs | d   || q
        0  | 0   || 'h'
        1  | 0   || 0
        0  | 1  || 'h'
        1  | 1 || 1
        // @formatter:on

    }
}


