/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.rom.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RomTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/rom.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    void testMultipleWritesAndReads() {
        setHi("~{CS}");
        checkBusImpedance("dBus", "with Hi ~{CS} D bus must be in hiImpedance");
        setLo("~{CS}");
        assertFalse(inBus("dBus").hiImpedance, "with lo ~{CS} D bus must nod be in hiImpedance");
        for (int i = 0; i < 5; i++) {
            setBus("aBus", i);
            checkBus("dBus", i + 1, "The value read from ROM does not match the value from data file.");
        }
    }
}
