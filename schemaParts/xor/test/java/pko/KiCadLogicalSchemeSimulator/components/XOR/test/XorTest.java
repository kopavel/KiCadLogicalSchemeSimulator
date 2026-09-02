/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.XOR.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class XorTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/xor.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    protected void xorTest() {
        checkPin(1, false, "Initial out state must be Lo");
        setHi(1);
        checkPin(1, true, "With one input Hi output state must be Hi");
        setHi(2);
        checkPin(1, false, "With both input Hi output state must be Lo");
        setLo(1);
        checkPin(1, true, "With one input Hi output state must be Hi");
    }
}
