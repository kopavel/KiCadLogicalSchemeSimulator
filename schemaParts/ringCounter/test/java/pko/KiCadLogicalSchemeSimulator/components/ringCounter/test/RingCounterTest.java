/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.ringCounter.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class RingCounterTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/ringCounter.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @BeforeEach
    void reset() {
        net.schemaParts.get("U2").reset();
        setLo("Cin");
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
            checkBus("qBus", (int) Math.pow(2, i), "Count should increment on clock signal");
            if (i < 4) {
                checkPin("Cout", true, "Carry out must be 1 when count are " + Math.pow(2, i) + "; i = " + i);
            } else {
                checkPin("Cout", false, "Carry out must be 0 when count are " + Math.pow(2, i) + "; i = " + i);
            }
            setHi("C");
        }
        setHi("C");
        checkBus("qBus", 1, "Count should reset after reaching maximum");
    }

    @Test
    @DisplayName("Reset pin resets the counter")
    void resetPinResetsCounter() {
        for (int i = 1; i <= 3; i++) {
            setHi("C");
        }
        checkBus("qBus", 8, "Count should be 8 before reset");
        setHi("R");
        checkBus("qBus", 1, "Count should reset on rising edge of reset pin");
    }

    @Test
    @DisplayName("Count does not change on reset pin falling edge")
    void countDoesNotChangeOnResetFallingEdge() {
        setHi("C");
        checkBus("qBus", 2, "Count should be 2 before reset");
        setLo("R");
        checkBus("qBus", 2, "Count should not change on falling edge of reset pin");
    }

    @Test
    @DisplayName("Count does not increment on clock falling edge")
    void countDoesNotIncrementOnClockFallingEdge() {
        setHi("C");
        checkBus("qBus", 2, "Count should be 2 before test");
        setLo("C");
        checkBus("qBus", 2, "Count should not increment on falling edge of clock signal");
    }

    @Test
    @DisplayName("Count does not increment on Hi CarryIn")
    void countDoesNotIncrementOnHiCi() {
        setHi("C");
        checkBus("qBus", 2, "Count should be 2 before test");
        setHi("Cin");
        setHi("C");
        checkBus("qBus", 2, "Count should not increment on falling edge of clock signal");
    }
}