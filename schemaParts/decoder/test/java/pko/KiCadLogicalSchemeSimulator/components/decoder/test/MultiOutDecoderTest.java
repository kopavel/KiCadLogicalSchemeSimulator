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

import static org.junit.jupiter.api.Assertions.assertFalse;

public class MultiOutDecoderTest extends NetTester {
    @Test
    @DisplayName("value decode")
    public void valueDecode() {
        setBus("A", 0);
        setBus("Eb", 0b11);
        checkBusImpedance("outA", "without CS pins outA bus state must be hiImpedance ");
        checkBusImpedance("outB", "without CS pins bus state must be hiImpedance ");
        setBus("Ea", 0b11);
        checkBusImpedance("outA", "with partial CS pins state outA bus state must be hiImpedance ");
        checkBusImpedance("outB", "without outB bus state must be hiImpedance ");
        setBus("Ea", 0b01);
        assertFalse(inBus("outA").hiImpedance, "with CS pins set outA bus state must not be hiImpedance ");
        checkBus("outA", 0b1110, "outA bus state must be 0b1110");
        checkBusImpedance("outB", "without outB bus state must be hiImpedance ");
        setBus("Ea", 0b11);
        setBus("Eb", 0b10);
        checkBusImpedance("outA", "with partial CS pins state outA bus state must be hiImpedance ");
        checkBusImpedance("outB", "with partial CS pins state outB bus state must be hiImpedance ");
        setBus("Eb", 0b00);
        checkBusImpedance("outA", "with partial CS pins state outA bus state must be hiImpedance ");
        assertFalse(inBus("outB").hiImpedance, "with CS pins set outB bus state must not be hiImpedance ");
        checkBus("outB", 0b1110, "outB bus state must be 0b1110");
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
