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
package pko.KiCadLogicalSchemeSimulator.components.shifter.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        setPin("PL", true);
        setPin("PL", false);
        System.out.println(Long.toBinaryString(net.schemaParts.get("U1").outPins.get("Q").getState()));
        assertFalse(inPin(5).state, "after parallel load 5 pin must be Lo");
        assertFalse(inPin(6).state, "after parallel load 6 pin must be Lo");
        assertTrue(inPin(7).state, "after parallel load 7 pin must be Hi");
        setPin("CP", true);
        assertTrue(inPin(5).state, "after 1 shift 5 pin must be Hi");
        assertFalse(inPin(6).state, "after 1 shift 6 pin must be Lo");
        assertFalse(inPin(7).state, "after 1 shift 7 pin must be Lo");
        setPin("CP", true);
        assertTrue(inPin(5).state, "after 2 shift 5 pin must be Lo");
        assertTrue(inPin(6).state, "after 2 shift 6 pin must be Hi");
        assertFalse(inPin(7).state, "after 2 shift 7 pin must be Lo");
        setPin("CP", true);
        assertFalse(inPin(5).state, "after 3 shift 5 pin must be Lo");
        assertTrue(inPin(6).state, "after 3 shift 6 pin must be Hi");
        assertTrue(inPin(7).state, "after 3 shift 7 pin must be Hi");
        setPin("CP", true);
        assertFalse(inPin(5).state, "after 4 shift 5 pin must be Lo");
        assertFalse(inPin(6).state, "after 4 shift 6 pin must be Lo");
        assertTrue(inPin(7).state, "after 4 shift 7 pin must be Hi");
        setPin("CP", true);
        assertTrue(inPin(5).state, "after 5 shift 5 pin must be Hi");
        assertFalse(inPin(6).state, "after 5 shift 6 pin must be Lo");
        assertFalse(inPin(7).state, "after 5 shift 7 pin must be Lo");
        setPin("DS", true);
        for (int i = 0; i < 5; i++) {
            setPin("CP", true);
            System.out.println(Long.toBinaryString(net.schemaParts.get("U1").outPins.get("Q").getState()));
        }
        assertFalse(inPin(5).state, "after 10 shift with DS hi 5 pin must be Lo");
        assertFalse(inPin(6).state, "after 10 shift with DS hi 6 pin must be Lo");
        assertFalse(inPin(7).state, "after 10 shift with DS hi 7 pin must be Lo");
        setPin("CP", true);
        assertTrue(inPin(5).state, "after 11 shift 5 pin must be Hi");
        assertFalse(inPin(6).state, "after 11 shift 6 pin must be Lo");
        assertFalse(inPin(7).state, "after 11 shift 7 pin must be Lo");
        setPin("CP", true);
        assertTrue(inPin(5).state, "after 12 shift 5 pin must be Hi");
        assertTrue(inPin(6).state, "after 12 shift 6 pin must be Hi");
        assertFalse(inPin(7).state, "after 12 shift 7 pin must be Lo");
        setPin("CP", true);
        assertTrue(inPin(5).state, "after 13 shift 5 pin must be Hi");
        assertTrue(inPin(6).state, "after 13 shift 6 pin must be Hi");
        assertTrue(inPin(7).state, "after 13 shift 7 pin must be Hi");
    }
}
