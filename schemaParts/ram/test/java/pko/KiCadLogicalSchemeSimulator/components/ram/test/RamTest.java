package pko.KiCadLogicalSchemeSimulator.components.ram.test;/*
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RamTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/ram.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @BeforeEach
    void reset() {
        setPin("~{CS}", true);
        setPin("~{OE}", true);
        setPin("~{WE}", true);
    }

    @Test
    void testMultipleWritesAndReads() {
        long[] testValues = {0x00, 0xFF, 0xA5, 0x5A};
        long[] testAddresses = {0x00, 0x01, 0xFF, 0x88};
        checkBusImpedance("RAM1", "D", "with hi ~{CS} D bus must be in hiImpedance");
        setPin("~{CS}", false);
        checkBusImpedance("RAM1", "D", "with lo ~{CS} and hi ~{OE} D bus must be in hiImpedance");
        for (int i = 0; i < testValues.length; i++) {
            setBus("aBus", testAddresses[i]);
            setBus("dOut", testValues[i]);
            setPin("~{WE}", false);
        }
        outBus("dOut").setHiImpedance();
        setPin("~{OE}", false);
        assertFalse(inBus("dIn").hiImpedance, "with lo ~{OE} D bus must not be in hiImpedance");
        for (int i = 0; i < testValues.length; i++) {
            setBus("aBus", testAddresses[i]);
            checkBus("dIn", testValues[i], "The value read from RAM does not match the value written.");
        }
        setPin("~{CS}", true);
        checkBusImpedance("RAM1", "D", "with hi ~{CS} D bus must be in hiImpedance even with lo ~{OE}");
        setPin("~{OE}", true);
        setBus("dOut", 0);
        for (int i = 0; i < testValues.length; i++) {
            setBus("aBus", testAddresses[i]);
            setPin("~{WE}", false);
        }
        outBus("dOut").setHiImpedance();
        setPin("~{CS}", false);
        setPin("~{OE}", false);
        assertFalse(inBus("dIn").hiImpedance, "with lo ~{CS} D bus must not be in hiImpedance");
        for (int i = 0; i < testValues.length; i++) {
            setBus("aBus", testAddresses[i]);
            checkBus("dIn", testValues[i], "With hi ~{CS} RAM should ignore write operation");
        }
    }
}
