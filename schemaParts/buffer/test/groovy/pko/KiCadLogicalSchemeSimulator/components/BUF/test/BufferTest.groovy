package pko.KiCadLogicalSchemeSimulator.components.BUF.test


import pko.KiCadLogicalSchemeSimulator.components.BUF.BufferSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

class BufferTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(
                new BufferSpi(),
                "size=8",
                ["CS", "D"],
                ["Q"]
        )
    }

    @Unroll("CS:#cs, D:#d -> Q:#q")
    def "Buffer"() {
        when:
        setInputs(cs, d)

        then:
        checkOutputs(q)

        where:
        cs | d   || q
        0  | 0   || 'h'
        1  | 0   || 0
        0  | 10  || 'h'
        1  | 255 || 255
    }
}


