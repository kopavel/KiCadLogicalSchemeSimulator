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
package pko.KiCadLogicalSchemeSimulator.components.BUF.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class BuffTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/buffer.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @BeforeEach
    void reset() {
        setBus("BufOut", 0);
        setHi("BufCs");
    }

    @Test
    @DisplayName("Hi CS")
    void noCs() {
        setHi("BufCs");
        checkBusImpedance("BufIn", "With Hi CS pin Q must be hiImpedance");
        setBus("BufOut", 1);
        checkBusImpedance("BufIn", "With Hi CS pin Q must be hiImpedance");
    }

    @Test
    @DisplayName("Lo CS and Hi D")
    void trueInput() {
        setLo("BufCs");
        checkBus("BufIn", 0, "With Lo CS pin Q must not be hiImpedance");
        setBus("BufOut", 1);
        checkBus("BufIn", 1, "With Lo CS and Hi D pin Q must be Hi");
    }

    @Test
    @DisplayName("Lo CS and Lo D")
    void falseInput() {
        setLo("BufCs");
        setBus("BufOut", 0);
        assertFalse(inBus("BufIn").hiImpedance, "With Lo CS pin Q must not be hiImpedance");
        checkBus("BufIn", 0, "With Lo CS and Hi D pin Q must be Hi");
    }

    @Test
    @DisplayName("Toggle CS")
    void toggleCs() {
        setBus("BufOut", 3);
        setLo("BufCs");
        checkBus("BufIn", 3, "With Lo CS and Hi D pin Q must be Hi");
        setHi("BufCs");
        checkBusImpedance("BufIn", "With Hi CS pin Q must be hiImpedance");
        setLo("BufCs");
        checkBus("BufIn", 3, "With Lo CS again pin Q must be Hi");
    }
}
