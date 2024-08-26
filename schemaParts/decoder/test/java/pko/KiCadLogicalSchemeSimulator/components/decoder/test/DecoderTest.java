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
package pko.KiCadLogicalSchemeSimulator.components.decoder.test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecoderTest extends NetTester {
    @Test
    @DisplayName("default values")
    public void defaultValues() {
        setPin("cs", true);
        for (int i = 0; i < 8; i++) {
            assertEquals(-1, checkOut(), "Q out default impedance must be Hi");
        }
    }

    @Test
    @DisplayName("value decode")
    public void valueDecode() {
        setBus("Src", 0);
        setPin("cs", false);
        assertEquals(1, checkOut(), "with 0 on A input Q out be 1");
        setBus("Src", 1);
        assertEquals(2, checkOut(), "with 1 on A input Q out be 2");
        setBus("Src", 2);
        assertEquals(3, checkOut(), "with 2 on A input Q out be 4");
        setBus("Src", 3);
        assertEquals(4, checkOut(), "with 3 on A input Q out be 8");
        setPin("cs", true);
        assertEquals(-1, checkOut(), "with Hi CS Q impedance must be Hi");
        setBus("Src", 2);
        assertEquals(-1, checkOut(), "with Hi CS Q impedance must be Hi");
        setPin("cs", false);
        assertEquals(3, checkOut(), "with Lo CS A state must be stored internally");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/decoder.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    private int checkOut() {
        for (int i = 1; i < 9; i++) {
            if (!inPin(i).hiImpedance && !inPin(i).state) {
                return i;
            }
        }
        return -1;
    }
}
