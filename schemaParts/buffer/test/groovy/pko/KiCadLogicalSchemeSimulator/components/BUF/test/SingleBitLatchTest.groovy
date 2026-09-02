/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package pko.KiCadLogicalSchemeSimulator.components.BUF.test


import pko.KiCadLogicalSchemeSimulator.components.BUF.singleBit.SingleBitBufferSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class SingleBitLatchTest extends ChipSpec {
    @Override
    protected ChipDefinition chip() {
        return new ChipDefinition(
                new SingleBitBufferSpi(),
                "latch;reverse",
                ["WR", "OE", "D"],
                ["Q"]
        )
    }

    @Unroll("#optimized | WR:#wr, OE:#oe, D:#d -> Q:#q")
    def "Latch"() {
        given:
        useChip(optimized)
        when:
        setInputs(wr, oe, d)

        then:
        checkOutputs(q)

        where:
        optimized << [RAW, OPT]

        combined:
        wr | oe | d || q
        1 | 1 | 1 || 'h'
        0 | 1 | 1 || 'h'
        1 | 1 | 1 || 'h'
        1 | 0 | 1 || 1
        1 | 1 | 1 || 'h'
        1 | 1 | 0 || 'h'
        1 | 0 | 0 || 1
        0 | 0 | 0 || 0
    }
}


