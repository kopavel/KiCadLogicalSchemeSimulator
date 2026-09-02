/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.shifter.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class ShifterTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/shifter.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    protected void shifterTest() {
        setBus("dBus", 0b10011001);
        setHi("PL");
        setHi("CP");
        setLo("PL");
        checkPin(5, false, "after parallel load 5 pin must be Lo");
        checkPin(6, false, "after parallel load 6 pin must be Lo");
        checkPin(7, true, "after parallel load 7 pin must be Hi");
        setHi("CP");
        checkPin(5, true, "after 1 shift 5 pin must be Hi");
        checkPin(6, false, "after 1 shift 6 pin must be Lo");
        checkPin(7, false, "after 1 shift 7 pin must be Lo");
        setHi("CP");
        checkPin(5, true, "after 2 shift 5 pin must be Lo");
        checkPin(6, true, "after 2 shift 6 pin must be Hi");
        checkPin(7, false, "after 2 shift 7 pin must be Lo");
        setHi("CP");
        checkPin(5, false, "after 3 shift 5 pin must be Lo");
        checkPin(6, true, "after 3 shift 6 pin must be Hi");
        checkPin(7, true, "after 3 shift 7 pin must be Hi");
        setHi("CP");
        checkPin(5, false, "after 4 shift 5 pin must be Lo");
        checkPin(6, false, "after 4 shift 6 pin must be Lo");
        checkPin(7, true, "after 4 shift 7 pin must be Hi");
        setHi("CP");
        checkPin(5, true, "after 5 shift 5 pin must be Hi");
        checkPin(6, false, "after 5 shift 6 pin must be Lo");
        checkPin(7, false, "after 5 shift 7 pin must be Lo");
        setHi("DS");
        for (int i = 0; i < 5; i++) {
            setHi("CP");
        }
        checkPin(5, false, "after 10 shift with DS hi 5 pin must be Lo");
        checkPin(6, false, "after 10 shift with DS hi 6 pin must be Lo");
        checkPin(7, false, "after 10 shift with DS hi 7 pin must be Lo");
        setHi("CP");
        checkPin(5, true, "after 11 shift 5 pin must be Hi");
        checkPin(6, false, "after 11 shift 6 pin must be Lo");
        checkPin(7, false, "after 11 shift 7 pin must be Lo");
        setHi("CP");
        checkPin(5, true, "after 12 shift 5 pin must be Hi");
        checkPin(6, true, "after 12 shift 6 pin must be Hi");
        checkPin(7, false, "after 12 shift 7 pin must be Lo");
        setHi("CP");
        checkPin(5, true, "after 13 shift 5 pin must be Hi");
        checkPin(6, true, "after 13 shift 6 pin must be Hi");
        checkPin(7, true, "after 13 shift 7 pin must be Hi");
    }
}
