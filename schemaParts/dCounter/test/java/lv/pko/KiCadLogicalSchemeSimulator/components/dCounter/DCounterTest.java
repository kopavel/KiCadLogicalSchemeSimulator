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
package lv.pko.KiCadLogicalSchemeSimulator.components.dCounter;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DCounterTest {
    private DCounter counter;
    private InPin cPin;
    private InPin ciPin;
    private InPin jPin;
    private InPin pePin;
    private InPin udPin;
    private InPin bdPin;
    private InPin rPin;
    private OutPin qPin;
    private OutPin coPin;

    @BeforeEach
    void setUp() {
        counter = new DCounter("counter", "size=4");
        counter.initOuts();
        cPin = counter.getInPin("C");
        ciPin = counter.getInPin("CI");
        jPin = counter.getInPin("J");
        pePin = counter.getInPin("PE");
        udPin = counter.getInPin("UD");
        bdPin = counter.getInPin("BD");
        rPin = counter.getInPin("R");
        qPin = counter.getOutPin("Q");
        coPin = counter.getOutPin("CO");
        InPin qDest = new InPin("qDest", counter) {
            @Override
            public void onChange(long newState, boolean hiImpedance, boolean strong) {
            }
        };
        qDest.mask = 15;
        InPin coDest = new InPin("coDest", counter) {
            @Override
            public void onChange(long newState, boolean hiImpedance, boolean strong) {
            }
        };
        coDest.mask = 1;
        qPin.addDestination(qDest);
        coPin.addDestination(coDest);
    }

    @Test
    @DisplayName("Default counter behavior - count up, binary mode")
    void defaultCounterBehavior() {
        ciPin.onChange(1, false, true);
        assertEquals(0, qPin.state, "Initial state should be 0");
        assertEquals(0, coPin.state, "Initial carry-out state should be 0");
        for (int i = 0; i < 15; i++) {
            assertEquals(qPin.state, i, "Output state should be equal to the count");
            cPin.onChange(0, false, true);
            cPin.onChange(1, false, true);
        }
        assertEquals(1, coPin.state, "Carry-out should be 1 after overflow");
    }

    @Test
    @DisplayName("Reset counter")
    void resetCounter() {
        ciPin.onChange(1, false, true);
        for (int i = 0; i < 15; i++) {
            cPin.onChange(0, false, true);
            cPin.onChange(1, false, true);
        }
        rPin.onChange(0, false, true);
        assertEquals(15, qPin.state, "State should be preserved after reset pin set to 0");
        assertEquals(1, coPin.state, "Carry-out should remain high after reset pin set to 0");
        rPin.onChange(1, false, true);
        assertEquals(0, qPin.state, "State should be reset to 0 after reset pin set to 1");
        assertEquals(0, coPin.state, "Carry-out should reset to lo after reset pin set to 1");
    }

    @Test
    @DisplayName("Presetting the counter")
    void presetCounter() {
        jPin.state = 5;
        jPin.onChange(5, false, true);
        pePin.onChange(1, false, true);
        assertEquals(5, qPin.state, "State should be set to preset value when preset is enabled");
        pePin.onChange(0, false, true);
        jPin.onChange(0, false, true);
        assertEquals(5, qPin.state, "State should be preserved when preset is disabled");
    }

    @Test
    @DisplayName("Count down behavior")
    void countDownBehavior() {
        ciPin.onChange(1, false, true);
        udPin.onChange(0, false, true);
        assertEquals(0, qPin.state, "Initial state should be 0");
        assertEquals(1, coPin.state, "Initial carry-out state should be 1");
        for (int i = 15; i >= 0; i--) {
            cPin.onChange(0, false, true);
            cPin.onChange(1, false, true);
            assertEquals(qPin.state, i, "Output state should be equal to the count");
        }
        assertEquals(1, coPin.state, "Carry-out should be 1 after underflow");
    }

    @Test
    @DisplayName("Toggle between binary and decimal modes")
    void toggleMode() {
        ciPin.onChange(1, false, true); // Enable counting
        // Initial state assertions
        assertEquals(0, qPin.state, "Initial state should be 0");
        assertEquals(0, coPin.state, "Initial carry-out state should be 0");
        // Binary mode (default)
        assertEquals(15, counter.maxCount, "Maximum count should be 15 in binary mode");
        // Toggle to decimal mode
        bdPin.onChange(0, false, true);
        assertEquals(9, counter.maxCount, "Maximum count should be 9 in decimal mode");
        // Toggle back to binary mode
        bdPin.onChange(1, false, true);
        assertEquals(15, counter.maxCount, "Maximum count should be 15 in binary mode after toggling back");
    }

    @Test
    @DisplayName("Toggle between counting up and counting down")
    void toggleCountDirection() {
        ciPin.onChange(1, false, true); // Enable counting
        // Initial state assertions
        assertEquals(0, qPin.state, "Initial state should be 0");
        assertEquals(0, coPin.state, "Initial carry-out state should be 0");
        // Counting up (default)
        for (int i = 0; i < 4; i++) {
            cPin.onChange(0, false, true);
            cPin.onChange(1, false, true);
        }
        assertEquals(qPin.state, 4, "Count should be equal 4");
        // Toggle to count down mode
        udPin.onChange(0, false, true);
        for (int i = 0; i < 2; i++) {
            cPin.onChange(0, false, true);
            cPin.onChange(1, false, true);
        }
        assertEquals(qPin.state, 2, "Count should be equal 2");
        for (int i = 0; i < 3; i++) {
            cPin.onChange(0, false, true);
            cPin.onChange(1, false, true);
        }
        assertEquals(qPin.state, 15, "Count should be equal after underflow 15");
        udPin.onChange(1, false, true);
        cPin.onChange(0, false, true);
        cPin.onChange(1, false, true);
        assertEquals(qPin.state, 0, "Count should be equal 0 after overflow");
        ciPin.onChange(0, false, true);
        cPin.onChange(0, false, true);
        cPin.onChange(1, false, true);
        assertEquals(qPin.state, 0, "Clock must be ignored with carry in set to Lo");
    }
}