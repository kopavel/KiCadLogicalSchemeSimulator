package pko.KiCadLogicalSchemeSimulator.components.counter.test


import pko.KiCadLogicalSchemeSimulator.components.counter.multipart.MultiPartCounterSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class MultiPartCounterTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(
                new MultiPartCounterSpi(),
                "size=1,3;reverse;skip=,2;resetAmount=2", //74LS92
                ["Ca", "Cb", "R0", "R1"],
                ["Qa", "Qb"]
        );
    }

    @Unroll("#optimized | Ca:#ca, Cb:#cb, R0:#r0, R1:#r1 -> Qa:#qa,Qb:#qb")
    def "Counter"() {
        given:
        useChip(optimized)

        when:
        setInputs(ca, cb, r0, r1)

        then:
        checkOutputs(qa, qb)

        where:
        optimized << [RAW, OPT]

        combined:
        // @formatter:off
        ca | cb | r0 | r1 || qa | qb
        1  |  1 |  0 |  0 ||  0 |  0
        0  |  0 |  0 |  0 ||  1 |  1
        1  |  1 |  0 |  0 ||  1 |  1
        0  |  0 |  0 |  0 ||  0 |  2
        1  |  1 |  1 |  0 ||  0 |  2
        0  |  1 |  0 |  1 ||  1 |  2
        1  |  1 |  1 |  1 ||  0 |  0
        0  |  1 |  0 |  1 ||  0 |  0
        1  |  0 |  0 |  1 ||  0 |  1
        1  |  1 |  0 |  1 ||  0 |  1
        1  |  0 |  0 |  1 ||  0 |  2
        1  |  1 |  0 |  1 ||  0 |  2
        1  |  0 |  0 |  1 ||  0 |  4
        1  |  1 |  0 |  1 ||  0 |  4
        1  |  0 |  0 |  1 ||  0 |  5
        1  |  1 |  0 |  1 ||  0 |  5
        1  |  0 |  0 |  1 ||  0 |  6
        1  |  1 |  0 |  1 ||  0 |  6
        1  |  0 |  0 |  1 ||  0 |  0
        1  |  1 |  0 |  1 ||  0 |  0
        // @formatter:on
    }
}
