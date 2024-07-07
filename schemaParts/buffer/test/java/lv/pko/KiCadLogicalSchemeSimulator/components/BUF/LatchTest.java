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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LatchTest {
    final Buffer buffer;
    final InPin dPin;
    final TriStateOutPin qPin;
    final InPin csPin;
    final InPin wrPin;

    public LatchTest() {
        buffer = new Buffer("buf", "size=1;latch");
        dPin = buffer.inMap.get("D");
        qPin = (TriStateOutPin) buffer.outMap.get("Q");
        csPin = buffer.inMap.get("~{OE}");
        wrPin = buffer.inMap.get("~{WR}");
        InPin dest = new InPin("dest", buffer) {
            @Override
            public void onChange(long newState, boolean hiImpedance, boolean strong) {
            }
        };
        qPin.addDestination(dest);
        dPin.mask = 1;
        dest.mask = 1;
        buffer.initOuts();
        csPin.onChange(1, false, true);
        wrPin.onChange(1, false, true);
        buffer.reset();
    }

    @Test
    @DisplayName("Q pin should be in high-impedance state with Hi CS")
    void qPinHighImpedanceWithHighCs() {
        csPin.onChange(1, false, true);
        assertTrue(qPin.hiImpedance, "With Hi CS pin, Q must be in high-impedance state");
    }

    @Test
    @DisplayName("Q pin should remain high-impedance with Hi CS and changing D pin")
    void qPinRemainsHighImpedanceWithHighCsAndChangingD() {
        csPin.onChange(1, false, true);
        dPin.onChange(1, true, true);
        assertTrue(qPin.hiImpedance, "With Hi CS pin, Q must remain in high-impedance state when D pin changes");
    }

    @Test
    @DisplayName("Latch should store and read correctly")
    void latchWriteAndRead() {
        csPin.onChange(1, false, true);
        dPin.state = 1;
        dPin.onChange(1, false, true);
        wrPin.onChange(0, false, true);
        csPin.onChange(0, false, true);
        assertEquals(1, qPin.state, "Q pin should reflect the state of D pin after OE falling edge");
        csPin.onChange(1, false, true);
        assertTrue(qPin.hiImpedance, "Q pin should be in high-impedance state after OE raising edge");
    }

    @Test
    @DisplayName("Floating D pin should throw FloatingPinException")
    void floatingDPinThrowsException() {
        assertThrows(FloatingPinException.class, () -> csPin.onChange(1, true, true), "Floating input must throw exception with Hi CS");
    }
}
