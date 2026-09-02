/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.multiplexer.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class MaskedMultiplexerTest extends NetTester {
    @Test
    @DisplayName("defaultState")
    public void defaultState() {
        checkPin("Qa", false, "default Qa state must be 0");
        checkPin("Qb", false, "default Qb state must be 0");
    }

    @Test
    public void multiplexTest() {
        setBus("aBus", 0b0101);
        setBus("bBus", 0b1100);
        checkPin("Qa", true, "with n=0 Qa state must be equal with A 1 pin state");
        checkPin("Qb", false, "with n=0 Qb state must be equal with A 1 pin state");
        setHi("Ea");
        checkPin("Qa", false, "with Ea Lo Qa state must be Lo");
        checkPin("Qb", false, "with n=0 Qb state must be equal with A 1 pin state");
        setLo("Ea");
        setBus("nBus", 1);
        checkPin("Qa", false, "with n=1 Qa state must be equal with A 2 pin state");
        checkPin("Qb", false, "with n=1 Qb state must be equal with A 2 pin state");
        setBus("nBus", 2);
        checkPin("Qa", true, "with n=2 Qa state must be equal with A 3 pin state");
        checkPin("Qb", true, "with n=2 Qb state must be equal with A 3 pin state");
        setHi("Eb");
        checkPin("Qa", true, "with n=2 Qa state must be equal with A 3 pin state");
        checkPin("Qb", false, "with Eb Lo Qb state must be Lo");
        setLo("Eb");
        setBus("nBus", 3);
        checkPin("Qa", false, "with n=3 Qa state must be equal with A 4 pin state");
        checkPin("Qb", true, "with n=3 Qb state must be equal with A 4 pin state");
    }

    @BeforeEach
    protected void reset() {
        setBus("aBus", 0);
        setBus("bBus", 0);
        setBus("nBus", 0);
        setLo("Ea");
        setLo("Eb");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/maskedMultiplexer.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }
}
