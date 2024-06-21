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
package lv.pko.KiCadLogicalSchemeSimulator.components.jkTrigger;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JkTriggerTest {
    private final InPin jPin;
    private final InPin kPin;
    private final InPin rPin;
    private final InPin sPin;
    private final InPin cPin;
    private final OutPin qOut;
    private final OutPin iqOut;
    private final JkTrigger trigger;

    public JkTriggerTest() {
        trigger = new JkTrigger("jkt", "");
        trigger.initOuts();
        jPin = trigger.inMap.get("J");
        kPin = trigger.inMap.get("K");
        rPin = trigger.inMap.get("R");
        sPin = trigger.inMap.get("S");
        cPin = trigger.inMap.get("C");
        qOut = trigger.outMap.get("Q");
        iqOut = trigger.outMap.get("~{Q}");
        InPin dest = new InPin("dest", trigger) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        dest.mask = 1;
        qOut.addDest(dest);
        iqOut.addDest(dest);
    }

    @Test
    @DisplayName("main states")
    public void mainStates() {
        //Lo j and k - store
        assertEquals(0, qOut.state, "Default Q state must be 0");
        assertEquals(1, iqOut.state, "Default ~{Q} state must be 1;");
        cPin.state = 1;
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "after C pin raise  with Lo on 'J' and 'K' Q state must remain 0");
        assertEquals(1, iqOut.state, "after C pin raise with Lo on 'J' and 'K' ~{Q} state must remain 1");
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "after C pin raise  with Lo on 'J' and 'K' Q state must remain 0");
        assertEquals(1, iqOut.state, "after C pin raise with Lo on 'J' and 'K' ~{Q} state must remain 1");
        //Hi j Lo k - set
        jPin.state = 1;
        jPin.onChange(1, false);
        cPin.onChange(1, false);
        assertEquals(1, qOut.state, "after C pin raise  with Hi on 'J' and Lo on 'K' Q state must be 1");
        assertEquals(0, iqOut.state, "after C pin raise with Hi on 'J' and Lo on 'K' ~{Q} state must be 0");
        cPin.onChange(1, false);
        assertEquals(1, qOut.state, "after C pin raise  with Hi on 'J' and Lo on 'K' Q state must be 1");
        assertEquals(0, iqOut.state, "after C pin raise with Hi on 'J' and Lo on 'K' ~{Q} state must be 0");
        //Hi j and k - toggle
        kPin.state = 1;
        kPin.onChange(1, false);
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "after C pin raise  with Hi on 'J' and Lo on 'K' Q state must be toggled");
        assertEquals(1, iqOut.state, "after C pin raise with Hi on 'J' and Lo on 'K' ~{Q} state must be toggled");
        cPin.onChange(1, false);
        assertEquals(1, qOut.state, "after C pin raise  with Hi on 'J' and Lo on 'K' Q state must be toggled");
        assertEquals(0, iqOut.state, "after C pin raise with Hi on 'J' and Lo on 'K' ~{Q} state must be toggled");
        //Lo j Hi k - reset
        jPin.state = 0;
        jPin.onChange(0, false);
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "after C pin raise  with Lo on 'J' and Hi on 'K' Q state must be resetted");
        assertEquals(1, iqOut.state, "after C pin raise with Lo on 'J' and Hi on 'K' ~{Q} state must be toggled");
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "after C pin raise  with Lo on 'J' and Hi on 'K' Q state must be resetted");
        assertEquals(1, iqOut.state, "after C pin raise with Lo on 'J' and Hi on 'K' ~{Q} state must be toggled");
    }

    @Test
    @DisplayName("RS states")
    public void rsStates() {
        sPin.state = 1;
        sPin.onChange(1, false);
        assertEquals(1, qOut.state, "after S pin set to Hi Q state must be 1");
        assertEquals(0, iqOut.state, "after S pin set to Hi 'D' ~{Q} state must be 0");
        jPin.state = 1;
        jPin.onChange(1, false);
        kPin.state = 1;
        kPin.onChange(1, false);
        cPin.onChange(1, false);
        assertEquals(1, qOut.state, "with Hi S pin C pin state change must be ignored");
        assertEquals(0, iqOut.state, "with Hi S pin C pin state change must be ignored");
        rPin.state = 1;
        rPin.onChange(1, false);
        assertEquals(1, qOut.state, "after R pin set to Hi with S pin Hi too Q state must be 1");
        assertEquals(1, iqOut.state, "after R pin set to Hi with S pin Hi too ~{Q] state must be 1");
        sPin.state = 0;
        sPin.onChange(0, false);
        assertEquals(0, qOut.state, "after S pin set to Lo with Hi R  Q state must be 0");
        assertEquals(1, iqOut.state, "after S pin set to Lo with Hi R  ~{Q} state must be 0");
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "with Hi R pin C pin state change must be ignored");
        assertEquals(1, iqOut.state, "with Hi R pin C pin state change must be ignored");
    }
}
