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
package pko.KiCadLogicalSchemeSimulator.components.counter.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class MultiPartCounterTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/MultiPartCounter.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @BeforeEach
    void reset() {
        setPin("R0", true);
        setPin("R1", true);
        setPin("R0", false);
        setPin("R1", false);
    }

    @Test
    @DisplayName("Reset pin resets the counter")
    void resetPinResetsCounter() {
        setPin("Ca", false);
        setPin("Cb", false);
        checkBus("inB", 1, "Count should be 1 before reset");
        checkPin("inA", true, "Count should be 1 before reset");
        setPin("R0", true);
        setPin("R1", true);
        checkPin("inA", false, "Count should reset on rising edge of reset pin");
        checkBus("inB", 0, "Count should reset on rising edge of reset pin");
    }

    @Test
    @DisplayName("Count increments on clock signal")
    void countIncrementsOnClock() {
        for (int i = 1; i < 7; i++) {
            if (i == 3) {
                continue;
            }
            setPin("Cb", false);
            checkBus("inB", i, "Count should increment on clock signal");
        }
        setPin("Cb", false);
        checkBus("inB", 0, "Count should reset after reaching maximum");
        setPin("Ca", false);
        checkPin("inA", true, "Count should increment on clock signal");
        setPin("Ca", false);
        checkPin("inA", false, "Count should reset after reaching maximum");
    }

    @Test
    @DisplayName("Count does not change with active reset pin")
    void countDoesNotChangeOnResetFallingEdge() {
        setPin("R0", true);
        setPin("R1", true);
        checkBus("inB", 0, "Count should be 1 before reset");
        checkPin("inA", false, "Count should be 1 before reset");
        setPin("Ca", false);
        setPin("Cb", false);
        checkBus("inB", 0, "Count should not change on falling edge of reset pin");
        checkPin("inA", false, "Count should not change on falling edge of reset pin");
    }

    @Test
    @DisplayName("Count does not increment on clock raising edge")
    void countDoesNotIncrementOnClockFallingEdge() {
        setPin("Ca", true);
        checkPin("inA", false, "Count should not increment on raising edge of clock signal");
        setPin("Cb", true);
        checkBus("inB", 0, "Count should not increment on raising edge of clock signal");
    }
}
