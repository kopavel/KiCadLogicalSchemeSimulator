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
package pko.KiCadLogicalSchemeSimulator.components.multiplexer.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiplexerTest extends NetTester {
    @Test
    @DisplayName("defaultState")
    public void defaultState() {
        assertFalse(inPin("Qa").state, "default Qa state must be 0");
        assertFalse(inPin("Qb").state, "default Qb state must be 0");
    }

    @Test
    public void multiplexTest() {
        setBus("aBus", 0b0101);
        setBus("bBus", 0b1100);
        assertTrue(inPin("Qa").state, "with n=0 Qa state must be equal with A 1 pin state");
        assertFalse(inPin("Qb").state, "with n=0 Qb state must be equal with A 1 pin state");
        setBus("nBus", 1);
        assertFalse(inPin("Qa").state, "with n=1 Qa state must be equal with A 2 pin state");
        assertFalse(inPin("Qb").state, "with n=1 Qb state must be equal with A 2 pin state");
        setBus("nBus", 2);
        assertTrue(inPin("Qa").state, "with n=2 Qa state must be equal with A 3 pin state");
        assertTrue(inPin("Qb").state, "with n=2 Qb state must be equal with A 3 pin state");
        setBus("nBus", 3);
        assertFalse(inPin("Qa").state, "with n=3 Qa state must be equal with A 4 pin state");
        assertTrue(inPin("Qb").state, "with n=3 Qb state must be equal with A 4 pin state");
    }

    @BeforeEach
    protected void reset() {
        setBus("aBus", 0);
        setBus("bBus", 0);
        setBus("nBus", 0);
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/multiplexer.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }
}
