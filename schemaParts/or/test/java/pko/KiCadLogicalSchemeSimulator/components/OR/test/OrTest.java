/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.OR.test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class OrTest extends NetTester {
    @Test
    @DisplayName("Both input Lo - out Lo")
    public void bothLo() {
        setLo(1);
        setLo(2);
        checkPin(1, false, "With no input output needs to be Lo");
    }

    @Test
    @DisplayName("Only one input Hi - out Hi")
    public void oneHi() {
        setLo(1);
        setHi(2);
        checkPin(1, true, "With Hi on only one input output needs to be Hi");
    }

    @Test
    @DisplayName("Both input Hi - out Hi")
    public void bothHi() {
        setHi(1);
        setHi(2);
        checkPin(1, true, "With Hi on both inputs output needs to be Hi");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/Or.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }
}
