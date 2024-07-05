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
package lv.pko.KiCadLogicalSchemeSimulator.components.AND;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NandTest {
    final AndGate gate;
    final InPin inPin;
    final OutPin out;

    public NandTest() {
        gate = new AndGate("AndGate", "size=2;reverse");
        gate.initOuts();
        inPin = gate.inMap.get("IN");
        out = gate.outMap.get("OUT");
        InPin dest = new InPin("dest", gate) {
            @Override
            public void onChange(long newState, boolean hiImpedance, boolean strong) {
            }
        };
        inPin.mask = 3;
        dest.mask = 1;
        out.addDest(dest);
    }

    @Test
    @DisplayName("Both input Lo - out Hi")
    public void bothLo() {
        inPin.onChange(0, false, true);
        assertEquals(1, out.state, "With no input output need to be Lo");
    }

    @Test
    @DisplayName("Only one input Hi - out Hi")
    public void oneHi() {
        inPin.onChange(1, false, true);
        assertEquals(1, out.state, "With Hi on only one input output need to be Lo");
    }

    @Test
    @DisplayName("Both input Hi - out Lo")
    public void bothHi() {
        inPin.onChange(3, false, true);
        assertEquals(0, out.state, "With Hi on both inputs output need to be Hi");
    }
}
