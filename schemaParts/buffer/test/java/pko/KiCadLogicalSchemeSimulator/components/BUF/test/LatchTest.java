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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LatchTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/buffer.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    @DisplayName("Q pin should be in high-impedance state with Hi CS")
    void qPinHighImpedanceWithHighCs() {
        setPin("RegOe", true);
        assertTrue(inBus("RegIn").hiImpedance, "With Hi CS pin, Q must be in high-impedance state");
    }

    @Test
    @DisplayName("Q pin should remain high-impedance with Hi CS and changing D pin")
    void qPinRemainsHighImpedanceWithHighCsAndChangingD() {
        setPin("RegOe", true);
        setBus("RegOut", 1);
        assertTrue(inBus("RegIn").hiImpedance, "With Hi CS pin, Q must remain in high-impedance state when D pin changes");
    }

    @Test
    @DisplayName("Latch should store and read correctly")
    void latchWriteAndRead() {
        setPin("RegOe", true);
        setBus("RegOut", 1);
        setPin("RegWr", false);
        setPin("RegOe", false);
        assertEquals(1, inBus("RegIn").state, "Q pin should reflect the state of D pin after OE falling edge");
        setPin("RegOe", true);
        assertTrue(inBus("RegIn").hiImpedance, "Q pin should be in high-impedance state after OE raising edge");
    }
}
