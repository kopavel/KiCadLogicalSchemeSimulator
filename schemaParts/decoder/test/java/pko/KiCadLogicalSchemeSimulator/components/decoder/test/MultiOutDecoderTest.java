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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiOutDecoderTest extends NetTester {
    @Test
    @DisplayName("default values")
    public void defaultValues() {
        assertTrue(inBus("outA").hiImpedance, "initial outA bus state must be hiImpedance ");
        assertTrue(inBus("outB").hiImpedance, "initial outB bus state must be hiImpedance ");
    }

    @Test
    @DisplayName("value decode")
    public void valueDecode() {
        setBus("A", 0);
        setBus("Eb", 0b11);
        assertTrue(inBus("outA").hiImpedance, "without CS pins outA bus state must be hiImpedance ");
        assertTrue(inBus("outB").hiImpedance, "without CS pins bus state must be hiImpedance ");
        setBus("Ea", 0b11);
        assertTrue(inBus("outA").hiImpedance, "with partial CS pins state outA bus state must be hiImpedance ");
        assertTrue(inBus("outB").hiImpedance, "without outB bus state must be hiImpedance ");
        setBus("Ea", 0b01);
        assertFalse(inBus("outA").hiImpedance, "with CS pins set  outA bus state must not be hiImpedance ");
        assertEquals(0b1110, inBus("outA").state, "outA bus state must be 0b1110");
        assertTrue(inBus("outB").hiImpedance, "without outB bus state must be hiImpedance ");
        setBus("Ea", 0b11);
        setBus("Eb", 0b10);
        assertTrue(inBus("outA").hiImpedance, "with partial CS pins state outA bus state must be hiImpedance ");
        assertTrue(inBus("outB").hiImpedance, "with partial CS pins state outB bus state must be hiImpedance ");
        setBus("Eb", 0b00);
        assertTrue(inBus("outA").hiImpedance, "with partial CS pins state outA bus state must be hiImpedance ");
        assertFalse(inBus("outB").hiImpedance, "with CS pins set  outB bus state must not be hiImpedance ");
        assertEquals(0b1110, inBus("outB").state, "outB bus state must be 0b1110");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/MultiOutDecoder.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }
}
