/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package pko.KiCadLogicalSchemeSimulator.components.AND.test

import pko.KiCadLogicalSchemeSimulator.components.AND.AndGateSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

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

    @Unroll("#optimized | #a AND #b -> #expected")
    def "AndGate"() {
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
        0 | 1 || 0
        1 | 0 || 0
        1 | 1 || 1
    }
}