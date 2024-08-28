/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package pko.KiCadLogicalSchemeSimulator.components.stateMachine.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StateMachineTest extends NetTester {
    long[] states =
            new long[]{0b1111110, 0b0110000, 0b1101101, 0b1111001, 0b0110011, 0b1011011, 0b1011111, 0b1110000, 0b1111111, 0b1111011, 0b0001110, 0b0110111, 0b1100111,
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
        long mask = Utils.getMaskForSize(7);
        for (int i = 0; i < states.length; i++) {
            setBus("in", i);
            if (i > 0) {
                assertEquals(states[i - 1], inBus("out").state, "Out must be preserved until clock pulse");
            }
            setPin("S", true);
            assertEquals(states[i], inBus("out").state, "State must change on strobe front");
            setPin("F", true);
            assertEquals(states[i] ^ mask, inBus("out").state, "State must be in reverse, if R is Hi");
            setPin("F", false);
        }
    }
}
