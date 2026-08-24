package pko.KiCadLogicalSchemeSimulator.components.BUF.test


import pko.KiCadLogicalSchemeSimulator.components.BUF.BufferSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

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

    @Unroll("WR:#wr, OE:#oe, D:#d -> Q:#q")
    def "Latch"() {
        when:
        ins[0].syncState(wr)
        ins[1].syncState(oe)
        ins[2].syncState(d)

        then:
        out[0].hiImpedance && q == 'h' ||
                out[0].getState() == q && !out[0].hiImpedance

        where:
        wr | oe | d  || q
        1  | 1  | 10 || 'h'
        0  | 1  | 10 || 'h'
        1  | 1  | 10 || 'h'
        1  | 0  | 10 || 10
        1  | 1  | 10 || 'h'
        1  | 1  | 20 || 'h'
        1  | 0  | 20 || 10
        0  | 0  | 20 || 20
    }
}


