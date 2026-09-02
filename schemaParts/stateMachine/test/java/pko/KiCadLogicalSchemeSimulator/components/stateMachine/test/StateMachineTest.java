/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.stateMachine.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class StateMachineTest extends NetTester {
    final int[] states = {0b1111110, 0b0110000, 0b1101101, 0b1111001, 0b0110011, 0b1011011, 0b1011111, 0b1110000, 0b1111111, 0b1111011, 0b0001110, 0b0110111, 0b1100111,
            0b1110111, 0b0000001, 0b0};

    @Override
    protected String getNetFilePath() {
        return "test/resources/state.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    protected void testStates() {
        int mask = Utils.getMaskForSize(7);
        for (int i = 0; i < states.length; i++) {
            setBus("in", i);
            if (i > 0) {
                checkBus("out", states[i - 1], "Out must be preserved until clock pulse");
            }
            setHi("S");
            checkBus("out", states[i], "State must change on strobe front");
            setHi("F");
            checkBus("out", states[i] ^ mask, "State must be in reverse, if R is Hi");
            setLo("F");
        }
    }
}
