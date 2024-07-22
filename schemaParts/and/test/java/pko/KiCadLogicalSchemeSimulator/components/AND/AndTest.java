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
package pko.KiCadLogicalSchemeSimulator.components.AND;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AndTest {
    AndGate gate;
    InPin inPin;
    OutPin out;

    public void initializeGate(int size) {
        gate = new AndGate("AndGate", "size=" + size);
        gate.initOuts();
        inPin = gate.inMap.get("IN");
        out = gate.outMap.get("OUT");
        InPin dest = new InPin("dest", gate) {
            @Override
            public void onChange(long newState, boolean hiImpedance, boolean strong) {
            }
        };
        inPin.mask = Utils.getMaskForSize(size);
        dest.mask = 1;
        out.addDestination(dest);
    }

    @Test
    @DisplayName("Both input Lo - out Lo")
    public void bothLo() {
        initializeGate(2);
        inPin.onChange(0, false, true);
        assertEquals(0, out.state, "With no input output needs to be Lo");
    }

    @Test
    @DisplayName("Only one input Hi - out Lo")
    public void oneHi() {
        initializeGate(2);
        inPin.onChange(1, false, true);
        assertEquals(0, out.state, "With Hi on only one input output needs to be Lo");
    }

    @Test
    @DisplayName("Both input Hi - out Hi")
    public void bothHi() {
        initializeGate(2);
        inPin.onChange(3, false, true);
        assertEquals(1, out.state, "With Hi on both inputs output needs to be Hi");
    }

    @Test
    @DisplayName("Floating pin exception")
    public void floatPin() {
        initializeGate(2);
        assertThrows(FloatingPinException.class, () -> inPin.onChange(1, true, true), "Floating input must throw FloatingPinException");
    }

    @Test
    @DisplayName("Multiple input sizes")
    public void multipleInputSizes() {
        for (int size = 1; size <= 5; size++) {
            initializeGate(size);
            long allHi = (1 << size) - 1;
            inPin.onChange(allHi, false, true);
            assertEquals(1, out.state, "With Hi on all inputs output needs to be Hi for size " + size);
        }
    }

    @Test
    @DisplayName("Boundary condition: Minimum inputs")
    public void boundaryMinInputs() {
        initializeGate(1);
        inPin.onChange(0, false, true);
        assertEquals(0, out.state, "With single Lo input output needs to be Lo");
        inPin.onChange(1, false, true);
        assertEquals(1, out.state, "With single Hi input output needs to be Hi");
    }

    @Test
    @DisplayName("Boundary condition: Maximum inputs")
    public void boundaryMaxInputs() {
        initializeGate(64);
        long allHi = -1;
        inPin.onChange(allHi, false, true);
        assertEquals(1, out.state, "With Hi on all inputs output needs to be Hi for max size");
    }

    @Test
    @DisplayName("All but one input Hi - out Lo")
    public void allButOneHi() {
        initializeGate(2);
        inPin.onChange(2, false, true);
        assertEquals(0, out.state, "With Hi on all but one input output needs to be Lo");
    }
}
