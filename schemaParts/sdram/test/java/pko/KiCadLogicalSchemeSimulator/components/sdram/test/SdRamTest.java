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
