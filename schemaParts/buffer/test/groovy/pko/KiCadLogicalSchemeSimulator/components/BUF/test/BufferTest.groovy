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
        ins[0].syncState(cs)
        ins[1].syncState(d)

        then:
        out[0].hiImpedance && q == 'h' ||
                out[0].getState() == q && !out[0].hiImpedance

        where:
        cs | d   || q
        0  | 0   || 'h'
        1  | 0   || 0
        0  | 10  || 'h'
        1  | 255 || 255
    }
}


