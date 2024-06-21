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
package lv.pko.KiCadLogicalSchemeSimulator.components.decoder;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DecoderTest {
    private final Decoder decoder;
    private final InPin aPin;
    private final InPin csPin;
    private final TriStateOutPin qPin;

    public DecoderTest() {
        decoder = new Decoder("dec", "size=2");
        decoder.initOuts();
        aPin = decoder.inMap.get("A");
        csPin = decoder.inMap.get("CS");
        qPin = (TriStateOutPin) decoder.outMap.get("Q");
        InPin dest = new InPin("dest", decoder) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        dest.mask = 1;
        qPin.addDest(dest);
    }

    @Test
    @DisplayName("default values")
    public void defaultValues() {
        assertTrue(qPin.hiImpedance, "Q out default impedance must be Hi");
    }

    @Test
    @DisplayName("value decode")
    public void valueDecode() {
        csPin.state = 1;
        csPin.onChange(1, false);
        aPin.state = 0;
        aPin.onChange(0, false);
        assertEquals(1, qPin.state, "with 0 on A input Q out be 1");
        aPin.state = 1;
        aPin.onChange(1, false);
        assertEquals(2, qPin.state, "with 1 on A input Q out be 2");
        aPin.state = 2;
        aPin.onChange(2, false);
        assertEquals(4, qPin.state, "with 2 on A input Q out be 4");
        aPin.state = 3;
        aPin.onChange(3, false);
        assertEquals(8, qPin.state, "with 3 on A input Q out be 8");
        csPin.state = 0;
        csPin.onChange(0, false);
        assertTrue(qPin.hiImpedance, "with Lo CS Q impedance must be Hi");
        aPin.state = 2;
        aPin.onChange(2, false);
        assertTrue(qPin.hiImpedance, "with Lo CS Q impedance must be Hi");
        csPin.state = 1;
        csPin.onChange(1, false);
        assertEquals(4, qPin.state, "with Lo CS A state must be stored internally");
    }
}
