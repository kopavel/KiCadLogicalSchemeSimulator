/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package pko.KiCadLogicalSchemeSimulator.components.stateMachine.test


import pko.KiCadLogicalSchemeSimulator.components.stateMachine.StateMachineSpi
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.ChipSpec
import spock.lang.Unroll

import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.OPT
import static pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.Optimisation.RAW

class StateMachineTest extends ChipSpec {

    private static final int OUT_MASK = 127
    private static final List<Integer> STATES =
            [126, 48, 109, 121, 51, 91, 95, 112, 127, 123, 14, 55, 103, 119, 1, 0]

    @Override
    protected ChipDefinition chip() {
        new ChipDefinition(
                new StateMachineSpi(),
                "size=4;latch;outSize=7;latch;states=${STATES.join(',')}",
                ["IN", "C", "R"],
                ["OUT"]
        )
    }

    @Unroll("#optimized | IN:#IN; C:#C; R:#R -> #OUT")
    def "StateMachine"() {
        given:
        useChip(optimized)
        when:
        setInputs(IN, C, R)

        then:
        checkOutputs(OUT)

        where:
        optimized << [RAW, OPT]

        combined:
        [IN, C, R, OUT] << transitions()
    }

    private static List<List<Integer>> transitions() {
        def rows = new ArrayList<List<Integer>>();
        (1..<STATES.size()).each { i ->
            rows << [i, 1, 0, STATES[i]]
            rows << [i + 1, 0, 1, (~STATES[i]) & OUT_MASK]
        }
        return rows
    }
}
