package pko.KiCadLogicalSchemeSimulator.components.busDriver.test

import pko.KiCadLogicalSchemeSimulator.components.busDriver.BusDriverSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class BusDriverTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(
                new BusDriverSpi(),
                "size=2,4",
                ["OEa", "Ia", "OEb", "Ib"],
                ["Oa", "Ob"]
        )
    }

    @Unroll("#optimized | OEa:#oea, Ia:#ia, OEb:#oeb, Ib:#ib -> Oa:#oa, Ob:#ob")
    def "BusDriver"() {
        given:
        useChip(optimized)

        when:
        setInputs(oea, ia, oeb, ib)

        then:
        checkOutputs(oa, ob)

        where:
        optimized << [RAW, OPT]

        combined:
        // @formatter:off
        oea | ia | oeb | ib || oa  | ob
        0   | 0  | 0   | 0  || 'h' | 'h'
        1   | 10 | 0   | 0  || 10  | 'h'
        0   | 10 | 1   | 20 || 'h' | 20
        1   | 10 | 0   | 20 || 10  | 'h'
        // @formatter:on
    }
}
