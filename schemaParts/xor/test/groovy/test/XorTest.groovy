/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package test


import pko.KiCadLogicalSchemeSimulator.components.XOR.XorGateSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class XorTest extends ChipSpec {

    @Override
    protected ChipDefinition chip() {
        new ChipDefinition(
                new XorGateSpi(),
                "size=2",
                ["IN0", "IN1"],
                ["OUT"]
        )
    }

    @Unroll("#optimized | #a XOR #b -> #expected")
    def "XorGate"() {
        given:
        useChip(optimized)
        when:
        setInputs(a, b)

        then:
        checkOutputs(expected)

        where:
        optimized << [RAW, OPT]

        combined:
        a | b || expected
        0 | 0 || 0
        0 | 1 || 1
        1 | 0 || 1
        1 | 1 || 0
    }
}