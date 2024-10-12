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
package pko.KiCadLogicalSchemeSimulator.components.jnCounter.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class RingCounterTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/jnCounter.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @BeforeEach
    void reset() {
        net.schemaParts.get("U2").reset();
        setPin("Cin", false);
    }

    @Test
    @DisplayName("Initial count is zero")
    void initialCountIsZero() {
        checkBus("qBus", 1, "Initial count must be 1");
    }

    @Test
    @DisplayName("Count increments on clock signal")
    void countIncrementsOnClock() {
        checkPin("Cout", true, "Carry out initial state must be 1");
        for (int i = 0; i < 8; i++) {
            checkBus("qBus", (long) Math.pow(2, i), "Count should increment on clock signal");
            if (i < 4) {
                checkPin("Cout", true, "Carry out must be 1 when count are " + Math.pow(2, i) + "; i = " + i);
            } else {
                checkPin("Cout", false, "Carry out must be 0 when count are " + Math.pow(2, i) + "; i = " + i);
            }
            setPin("C", true);
        }
        setPin("C", true);
        checkBus("qBus", 1, "Count should reset after reaching maximum");
    }

    @Test
    @DisplayName("Reset pin resets the counter")
    void resetPinResetsCounter() {
        for (int i = 1; i <= 3; i++) {
            setPin("C", true);
        }
        checkBus("qBus", 8, "Count should be 8 before reset");
        setPin("R", true);
        checkBus("qBus", 1, "Count should reset on rising edge of reset pin");
    }

    @Test
    @DisplayName("Count does not change on reset pin falling edge")
    void countDoesNotChangeOnResetFallingEdge() {
        setPin("C", true);
        checkBus("qBus", 2, "Count should be 2 before reset");
        setPin("R", false);
        checkBus("qBus", 2, "Count should not change on falling edge of reset pin");
    }

    @Test
    @DisplayName("Count does not increment on clock falling edge")
    void countDoesNotIncrementOnClockFallingEdge() {
        setPin("C", true);
        checkBus("qBus", 2, "Count should be 2 before test");
        setPin("C", false);
        checkBus("qBus", 2, "Count should not increment on falling edge of clock signal");
    }

    @Test
    @DisplayName("Count does not increment on Hi CarryIn")
    void countDoesNotIncrementOnHiCi() {
        setPin("C", true);
        checkBus("qBus", 2, "Count should be 2 before test");
        setPin("Cin", true);
        setPin("C", true);
        checkBus("qBus", 2, "Count should not increment on falling edge of clock signal");
    }
}