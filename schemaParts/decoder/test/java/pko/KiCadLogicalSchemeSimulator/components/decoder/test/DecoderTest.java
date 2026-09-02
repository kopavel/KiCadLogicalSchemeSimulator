/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder.test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecoderTest extends NetTester {
    @Test
    @DisplayName("default values")
    public void defaultValues() {
        setHi("cs");
        for (int i = 0; i < 8; i++) {
            assertEquals(-1, checkOut(), "Q out default impedance must be Hi");
        }
    }

    @Test
    @DisplayName("value decode")
    public void valueDecode() {
        setBus("Src", 0);
        setLo("cs");
        assertEquals(1, checkOut(), "with 0 on A input Q out be 1");
        setBus("Src", 1);
        assertEquals(2, checkOut(), "with 1 on A input Q out be 2");
        setBus("Src", 2);
        assertEquals(3, checkOut(), "with 2 on A input Q out be 4");
        setBus("Src", 3);
        assertEquals(4, checkOut(), "with 3 on A input Q out be 8");
        setHi("cs");
        assertEquals(-1, checkOut(), "with Hi CS Q impedance must be Hi");
        setBus("Src", 2);
        assertEquals(-1, checkOut(), "with Hi CS Q impedance must be Hi");
        setLo("cs");
        assertEquals(3, checkOut(), "with Lo CS A state must be stored internally");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/decoder.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    private int checkOut() {
        for (int i = 1; i < 9; i++) {
            if (!inPin(i).hiImpedance && !inPin(i).state) {
                return i;
            }
        }
        return -1;
    }
}
