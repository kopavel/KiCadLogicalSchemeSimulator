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
package lv.pko.KiCadLogicalSchemeSimulator.components.BUF;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuffTest {
    Buffer buffer;
    InPin dPin;
    TriStateOutPin qPin;
    InPin csPin;

    public BuffTest() {
        initializeBuffer(1);
    }

    private void initializeBuffer(int size) {
        buffer = new Buffer("buf", "size=" + size);
        dPin = buffer.inMap.get("D");
        qPin = (TriStateOutPin) buffer.outMap.get("Q");
        csPin = buffer.inMap.get("~{CS}");
        InPin dest = new InPin("dest", buffer) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        qPin.addDest(dest);
        dPin.mask = 0;
        for (int i = 0; i < size; i++) {
            dPin.mask = dPin.mask << 1 | 1;
        }
        dest.mask = 1;
        buffer.initOuts();
    }

    @Test
    @DisplayName("Hi CS")
    void noCs() {
        csPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        dPin.onChange(1, true);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        dPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
    }

    @Test
    @DisplayName("Lo CS and Hi D")
    void trueInput() {
        csPin.onChange(0, false);
        assertFalse(qPin.hiImpedance, "With Lo CS pin Q must not be hiImpedance");
        dPin.onChange(1, false);
        assertEquals(1, qPin.state, "With Lo CS and Hi D pin Q must be Hi");
    }

    @Test
    @DisplayName("Lo CS and Lo D")
    void falseInput() {
        csPin.onChange(0, false);
        dPin.onChange(0, false);
        assertFalse(qPin.hiImpedance, "With Lo CS pin Q must not be hiImpedance");
        assertEquals(0, qPin.state, "With Lo CS and Lo D pin Q must be Lo");
    }

    @Test
    @DisplayName("Float D pin exception")
    void floatD() {
        csPin.onChange(1, false);
        assertDoesNotThrow(() -> dPin.onChange(1, true), "Floating input must not throw exception with Hi CS");
        csPin.onChange(0, false);
        assertThrows(FloatingPinException.class, () -> dPin.onChange(1, true), "Floating input must throw exception with Lo CS");
    }

    @Test
    @DisplayName("Multiple input sizes")
    void multipleInputSizes() {
        for (int size = 1; size <= 8; size++) {
            initializeBuffer(size);
            long allHi = 0;
            for (int i = 0; i < size; i++) {
                allHi = allHi << 1 | 1;
            }
            csPin.onChange(0, false);
            dPin.onChange(allHi, false);
            assertEquals(allHi, qPin.state, "With Lo CS and Hi D pin Q must match D for size " + size);
        }
    }

    @Test
    @DisplayName("Boundary condition: Minimum inputs")
    void boundaryMinInputs() {
        initializeBuffer(1);
        csPin.onChange(0, false);
        dPin.onChange(0, false);
        assertEquals(0, qPin.state, "With Lo CS and Lo D pin Q must be Lo");
        dPin.onChange(1, false);
        assertEquals(1, qPin.state, "With Lo CS and Hi D pin Q must be Hi");
    }

    @Test
    @DisplayName("Boundary condition: Maximum inputs")
    void boundaryMaxInputs() {
        initializeBuffer(64);
        long allHi = 0;
        for (int i = 0; i < 64; i++) {
            allHi = allHi << 1 | 1;
        }
        csPin.onChange(0, false);
        dPin.onChange(allHi, false);
        assertEquals(allHi, qPin.state, "With Lo CS and Hi D pin Q must match D for max size");
    }

    @Test
    @DisplayName("Partial Hi D with Lo CS")
    void partialHiD() {
        initializeBuffer(4);
        csPin.onChange(0, false);
        dPin.onChange(0b1010, false);
        assertEquals(0b1010, qPin.state, "With Lo CS and partial Hi D pin Q must match D");
    }

    @Test
    @DisplayName("Toggle CS")
    void toggleCs() {
        initializeBuffer(2);
        dPin.state = 3;
        dPin.onChange(3, false);
        csPin.onChange(0, false);
        assertEquals(3, qPin.state, "With Lo CS and Hi D pin Q must be Hi");
        csPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        csPin.onChange(0, false);
        assertEquals(3, qPin.state, "With Lo CS again pin Q must be Hi");
    }
}
