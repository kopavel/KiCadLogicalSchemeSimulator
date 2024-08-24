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
package pko.KiCadLogicalSchemeSimulator.components.dCounter.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.components.dCounter.DCounter;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DCounterTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/dCounter.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @BeforeEach
    void reset() {
        DCounter counter = (DCounter) net.schemaParts.get("U1");
        InPin rPin = (InPin) counter.getInItem("R");
        counter.reset();
        setPin("UD", true);
        setPin("BD", true);
        rPin.state = false;
        rPin.setState(false);
    }

    @Test
    @DisplayName("Default counter behavior - count up, binary mode")
    void defaultCounterBehavior() {
        setPin("Cin", false);
        assertEquals(0, inBus("qBus").state, "Initial state should be 0");
        assertTrue(inPin("Cout").state, "Initial carry-out state should be 1");
        for (int i = 0; i < 15; i++) {
            assertEquals(i, inBus("qBus").state, "Output state should be equal to the count");
            setPin("CK", false);
        }
        assertFalse(inPin("Cout").state, "Carry-out should be 0 after overflow");
    }

    @Test
    @DisplayName("Reset counter")
    void resetCounter() {
        InPin rPin = (InPin) net.schemaParts.get("U1").getInItem("R");
        setPin("Cin", false);
        setPin("UD", true);
        for (int i = 0; i < 15; i++) {
            setPin("CK", false);
        }
        rPin.state = false;
        rPin.setState(false);
        assertEquals(15, inBus("qBus").state, "State should be preserved after reset pin set to 0");
        assertFalse(inPin("Cout").state, "Carry-out should remain lo after reset pin set to 0");
        rPin.state = true;
        rPin.setState(true);
        assertEquals(0, inBus("qBus").state, "State should be reset to 0 after reset pin set to 1");
        assertTrue(inPin("Cout").state, "Carry-out should reset to hi after reset pin set to 1");
        rPin.state = false;
        rPin.setState(false);
    }

    @Test
    @DisplayName("Presetting the counter")
    void presetCounter() {
        setBus("jBus", 5);
        setPin("PE", true);
        assertEquals(5, inBus("qBus").state, "State should be set to preset value when preset is enabled");
        setPin("PE", false);
        setBus("jBus", 0);
        assertEquals(5, inBus("qBus").state, "State should be preserved when preset is disabled");
    }

    @Test
    @DisplayName("Count down behavior")
    void countDownBehavior() {
        setPin("Cin", false);
        setPin("UD", false);
        assertEquals(0, inBus("qBus").state, "Initial state should be 0");
        setPin("CK", false);
        assertTrue(inPin("Cout").state, "Initial carry-out state should be 1");
        for (int i = 15; i > 0; i--) {
            assertEquals(inBus("qBus").state, i, "Output state should be equal to the count");
            setPin("CK", false);
        }
        assertFalse(inPin("Cout").state, "Carry-out should be 0 after underflow");
    }

    @Test
    @DisplayName("Toggle between binary and decimal modes")
    void toggleMode() {
        DCounter counter = (DCounter) net.schemaParts.get("U1");
        setPin("Cin", true);
        // Initial state assertions
        assertEquals(0, inBus("qBus").state, "Initial state should be 0");
        assertTrue(inPin("Cout").state, "Initial carry-out state should be 1");
        // Binary mode (default)
        assertEquals(15, counter.maxCount, "Maximum count should be 15 in binary mode");
        // Toggle to decimal mode
        setPin("BD", false);
        assertEquals(9, counter.maxCount, "Maximum count should be 9 in decimal mode");
        // Toggle back to binary mode
        setPin("BD", true);
        assertEquals(15, counter.maxCount, "Maximum count should be 15 in binary mode after toggling back");
    }

    @Test
    @DisplayName("Toggle between counting up and counting down")
    void toggleCountDirection() {
        setPin("Cin", false);
        setPin("UD", true);
        // Initial state assertions
        assertEquals(0, inBus("qBus").state, "Initial state should be 0");
        assertTrue(inPin("Cout").state, "Initial carry-out state should be 1");
        // Counting up (default)
        for (int i = 0; i < 4; i++) {
            setPin("CK", false);
        }
        assertEquals(inBus("qBus").state, 4, "Count should be equal 4");
        // Toggle to count down mode
        setPin("UD", false);
        for (int i = 0; i < 2; i++) {
            setPin("CK", false);
        }
        assertEquals(inBus("qBus").state, 2, "Count should be equal 2");
        for (int i = 0; i < 3; i++) {
            setPin("CK", false);
        }
        assertEquals(inBus("qBus").state, 15, "Count should be equal after underflow 15");
        setPin("UD", true);
        setPin("CK", false);
        assertEquals(inBus("qBus").state, 0, "Count should be equal 0 after overflow");
        setPin("Cin", true);
        setPin("CK", false);
        assertEquals(inBus("qBus").state, 0, "Clock must be ignored with carry in set to Lo");
    }
}