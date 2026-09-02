/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.sdram.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class SdRamTest extends NetTester {
    @BeforeEach
    public void reset() {
        setHi("~{WE}");
        setHi("~{RAS}");
        setHi("~{CAS}");
        if (!outBus("aBus").hiImpedance) {
            outBus("aBus").setHiImpedance();
        }
        if (!outBus("dOut").hiImpedance) {
            outBus("dOut").setHiImpedance();
        }
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/sdRam.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    void testMultipleWritesAndReads() {
        int[] testValues = {0x00, 0xFF, 0xA5, 0x5A};
        int[] testAddresses = {0x00, 0x01, 0xee, 0x88};
        setLo("~{WE}");
        checkBusImpedance("SDRAM64K1", "D", "with hi ~{CAS} or lo ~{WR} D bus must be in hiImpedance");
        for (int i = 0; i < testValues.length; i++) {
            setBus("dOut", testValues[i]);
            setBus("aBus", testAddresses[i]);
            setLo("~{RAS}");
            setBus("aBus", testAddresses[i] + 1);
            setLo("~{CAS}");
        }
        outBus("dOut").setHiImpedance();
        checkBusImpedance("SDRAM64K1", "D", "with hi ~{CAS} or lo ~{WR} D bus must be in hiImpedance");
        setHi("~{WE}");
        for (int i = 0; i < testValues.length; i++) {
            setBus("aBus", testAddresses[i]);
            setLo("~{RAS}");
            setBus("aBus", testAddresses[i] + 1);
            setLo("~{CAS}");
            checkBus("dIn", testValues[i], "The value read from RAM does not match the value written.");
        }
    }
}
