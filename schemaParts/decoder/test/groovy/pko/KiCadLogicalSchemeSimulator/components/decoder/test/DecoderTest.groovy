/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package pko.KiCadLogicalSchemeSimulator.components.decoder.test

import pko.KiCadLogicalSchemeSimulator.components.decoder.DecoderSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class DecoderTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(new DecoderSpi(),
                "size=3;reverse;outReverse",
                ["A", "CS"],
                ["Q"])
    }

    @Unroll("#optimise | A:#a, CS:#cs -> Q:#q")
    def "Decoder 3-bit"() {
        given:
        useChip(optimise)

        when:
        setInputs(a, cs)

        then:
        checkOutputs(q)

        where:
        optimise << [RAW, OPT]

        combined:
        // @formatter:off
        a | cs || q
        0 | 1  || 'h'
        1 | 1  || 'h'
        2 | 1  || 'h'
        3 | 1  || 'h'
        4 | 1  || 'h'
        5 | 1  || 'h'
        6 | 1  || 'h'
        7 | 1  || 'h'
        0 | 0  || 0b11111110
        1 | 0  || 0b11111101
        2 | 0  || 0b11111011
        3 | 0  || 0b11110111
        4 | 0  || 0b11101111
        5 | 0  || 0b11011111
        6 | 0  || 0b10111111
        7 | 0  || 0b01111111
        // @formatter:on
    }
}
