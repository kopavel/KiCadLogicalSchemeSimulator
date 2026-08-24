package pko.KiCadLogicalSchemeSimulator.components.AND.test


import pko.KiCadLogicalSchemeSimulator.components.AND.AndGateSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

class AndTest extends ChipSpec {

    @Override
    protected ChipDefinition chip() {
        new ChipDefinition(
                new AndGateSpi(),
                "size=2",
                ["IN0", "IN1"],
                ["OUT"]
        )
    }

    @Unroll("#a AND #b -> #expected")
    def "AndGate"() {
        when:
        ins[0].syncState(a)
        ins[1].syncState(b)

        then:
        out[0].getState() == expected

        where:
        a | b || expected
        0 | 0 || 0
        0 | 1 || 0
        1 | 0 || 0
        1 | 1 || 1
    }
}