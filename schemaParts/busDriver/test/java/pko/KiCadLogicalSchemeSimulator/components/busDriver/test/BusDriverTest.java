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
package pko.KiCadLogicalSchemeSimulator.components.busDriver.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusDriverTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/busDriver.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    protected void testBusDriver() {
        setBus("oA", 0b1010);
        assertTrue(inBus("iA").hiImpedance, "Initial state of out bus iA must be hiImpedance");
        assertTrue(inPin("iB0").hiImpedance, "Initial state of out pin iB0 must be hiImpedance");
        assertTrue(inPin("iB1").hiImpedance, "Initial state of out pin iB1 must be hiImpedance");
        setPin("OEa", false);
        assertFalse(inBus("iA").hiImpedance, "with OEa in Hi state of out bus iA must not be hiImpedance");
        assertEquals(0b1010, inBus("iA").state, "with OEa in Lo state of out bus iA must 10");
        setPin("oB0", true);
        setPin("oB1", false);
        setPin("OEb", false);
        assertFalse(inPin("iB0").hiImpedance, "with OEb in Hi state of out pin iB0 must not be hiImpedance");
        assertFalse(inPin("iB1").hiImpedance, "with OEb in Hi state of out pin iB1 must not be hiImpedance");
        assertTrue(inPin("iB0").state, "with OEb in Lo state of out bus iB0 must true");
        assertFalse(inPin("iB1").state, "with OEb in Lo state of out bus iB1 must false");
    }
}
