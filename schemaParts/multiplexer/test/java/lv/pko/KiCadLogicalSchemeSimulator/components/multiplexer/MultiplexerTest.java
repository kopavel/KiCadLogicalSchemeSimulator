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
package lv.pko.KiCadLogicalSchemeSimulator.components.multiplexer;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiplexerTest {
    private final Multiplexer multiplexer;
    private final InPin aPin;
    private final InPin bPin;
    private final InPin nPin;
    private final OutPin qPin;

    public MultiplexerTest() {
        multiplexer = new Multiplexer("mpx", "size=8;nSize=1");
        multiplexer.initOuts();
        aPin = multiplexer.inMap.get("0");
        bPin = multiplexer.inMap.get("1");
        nPin = multiplexer.inMap.get("N");
        qPin = multiplexer.outMap.get("Q");
        InPin qDest = new InPin("qDest", multiplexer) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        qDest.mask = 0xff;
        qPin.addDest(qDest);
    }

    @Test
    @DisplayName("defaultState")
    public void defaultState() {
        assertEquals(0, qPin.state, "default Q state must be 0");
    }

    @Test
    @DisplayName("mutilex test")
    public void multiplexTest() {
        aPin.state = 0x24;
        aPin.onChange(aPin.state, false);
        bPin.state = 0xAC;
        bPin.onChange(bPin.state, false);
        assertEquals(aPin.state, qPin.state, "with n=0 Q state must be equal with A pin state");
        nPin.state = 1;
        nPin.onChange(nPin.state, false);
        assertEquals(bPin.state, qPin.state, "with n=1 Q state must be equal with B pin state");
    }
}
