package pko.KiCadLogicalSchemeSimulator.components.AND.test


import pko.KiCadLogicalSchemeSimulator.components.AND.AndGate
import pko.KiCadLogicalSchemeSimulator.components.AND.AndGateSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec

class AndTest extends ChipSpec {

    @Override
    protected ChipDefinition chip() {
        new ChipDefinition(
                new AndGate("nand", "size=2"),
                new AndGateSpi(),
                ["IN0", "IN1"],
                ["OUT"]
        )
    }

    def "AND: #a AND #b -> #expected"() {
        when:
        ins[0].setState(a)
        ins[1].setState(b)

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