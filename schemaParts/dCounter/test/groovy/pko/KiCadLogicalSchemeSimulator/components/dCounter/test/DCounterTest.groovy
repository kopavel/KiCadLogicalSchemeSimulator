/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package pko.KiCadLogicalSchemeSimulator.components.dCounter.test

import pko.KiCadLogicalSchemeSimulator.components.dCounter.DCounterSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class DCounterTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(new DCounterSpi(),
                "reverse;eReverse;carryReverse", //4029
                ["C", "CI", "UD", "BD", "PE", "J",],
                ["Q", "CO"]
        );
    }

    @Unroll("#optimized | C:#c, CI:#ci, UD:#ud, BD:#bd, PE:#pe, J:#j -> Q:#q, CO:#co")
    def "DCCounter_4029"() {
        given:
        useChip(optimized)

        when:
        setInputs(c, ci, ud, bd, pe, j)

        then:
        checkOutputs(q, co)

        where:
        optimized << [RAW, OPT]

        combined:
        // @formatter:off
        c | ci | ud | bd | pe | j || q | co
        0 |  0 |  1 |  1 |  1 | 5 || 5 |  1
        1 |  0 |  1 |  1 |  0 | 5 || 5 |  1
        0 |  0 |  1 |  1 |  0 | 5 || 6 |  1
        1 |  0 |  1 |  1 |  0 | 5 || 6 |  1
        0 |  0 |  1 |  1 |  0 | 5 || 7 |  1
        1 |  0 |  1 |  1 |  0 | 5 || 7 |  1
        0 |  0 |  1 |  1 |  0 | 5 || 8 |  1
        1 |  0 |  1 |  1 |  0 | 5 || 8 |  1
        0 |  0 |  1 |  1 |  0 | 5 || 9 |  1
        1 |  0 |  1 |  1 |  0 | 5 || 9 |  1
        0 |  0 |  1 |  1 |  0 | 5 ||10 |  1
        1 |  0 |  1 |  1 |  0 | 5 ||10 |  1
        0 |  0 |  1 |  1 |  0 | 5 ||11 |  1
        1 |  0 |  1 |  1 |  0 | 5 ||11 |  1
        0 |  0 |  1 |  1 |  0 | 5 ||12 |  1
        1 |  0 |  1 |  1 |  0 | 5 ||12 |  1
        0 |  0 |  1 |  1 |  0 | 5 ||13 |  1
        1 |  0 |  1 |  1 |  0 | 5 ||13 |  1
        0 |  0 |  1 |  1 |  0 | 5 ||14 |  1
        1 |  0 |  1 |  1 |  0 | 5 ||14 |  1
        0 |  0 |  1 |  1 |  0 | 5 ||15 |  0
        1 |  0 |  1 |  1 |  0 | 5 ||15 |  0
        0 |  0 |  0 |  1 |  1 | 9 || 9 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 9 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 8 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 8 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 7 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 7 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 6 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 6 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 5 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 5 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 4 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 4 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 3 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 3 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 2 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 2 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 1 |  1
        1 |  0 |  0 |  1 |  0 | 9 || 1 |  1
        0 |  1 |  0 |  1 |  0 | 9 || 0 |  1
        1 |  1 |  0 |  1 |  0 | 9 || 0 |  1
        0 |  0 |  0 |  1 |  0 | 9 || 0 |  0
        1 |  0 |  0 |  1 |  0 | 9 || 0 |  0
        0 |  0 |  0 |  1 |  0 | 9 ||15 |  1
        1 |  0 |  0 |  1 |  0 | 9 ||15 |  1
        // @formatter:on
    }
}
