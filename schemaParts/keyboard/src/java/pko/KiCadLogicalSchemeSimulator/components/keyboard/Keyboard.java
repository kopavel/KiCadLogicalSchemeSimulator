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
package pko.KiCadLogicalSchemeSimulator.components.keyboard;
import lombok.AllArgsConstructor;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.util.HashMap;
import java.util.Map;

public class Keyboard extends SchemaPart implements InteractiveSchemaPart {
    private final InBus in;
    private final InPin enable;
    private final int[] busMap = new int[256];
    private final Map<String, KeyDescriptor> keyDescriptors = new HashMap<>();
    private final KeyboardUiComponent keyboardUiComponent;
    private Bus out;
    private Pin event;

    protected Keyboard(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("map")) {
            throw new RuntimeException("Component " + id + " must have \"map\" parameter");
        }
        String[] keys = params.get("map").split("\\|");
        for (String key : keys) {
            String[] keyDesc = key.split("_");
            int rMask = 1 << (keyDesc[1].charAt(0) - '0');
            int cMask = 1 << (keyDesc[1].charAt(1) - '0');
            keyDescriptors.put(keyDesc[0], new KeyDescriptor(rMask, cMask, ~cMask));
        }
        enable = addInPin(new InPin("En", this) {
            @Override
            public void setState(boolean newState) {
                state = newState;
                hiImpedance = false;
                if (newState) {
                    out.hiImpedance = true;
                    out.setHiImpedance();
                } else {
                    if (out.state != busMap[(int) in.state] || out.hiImpedance) {
                        out.state = busMap[(int) in.state];
                        out.setState(out.state);
                    }
                }
            }
        });
        in = addInBus(new InBus("In", this, 8) {
            @Override
            public void setState(long newState) {
                state = newState;
                hiImpedance = false;
                if (!enable.state && (out.state != busMap[(int) newState] || out.hiImpedance)) {
                    out.state = busMap[(int) newState];
                    out.setState(out.state);
                }
            }
        });
        addOutBus("Out", 8);
        addOutPin("Ev");
        keyboardUiComponent = new KeyboardUiComponent(id, 50, this);
    }

    public void keyEvent(String text, boolean pressed) {
        if (keyDescriptors.containsKey(text)) {
            event.setState(true);
            KeyDescriptor descriptor = keyDescriptors.get(text);
            for (int i = 0; i < 255; i++) {
                if ((i & descriptor.rMask) > 0) {
                    if (pressed) {
                        busMap[i] |= descriptor.cMask;
                    } else {
                        busMap[i] &= descriptor.ncMask;
                    }
                }
            }
        }
    }

    @Override
    public void initOuts() {
        out = getOutBus("Out");
        event = getOutPin("Ev");
        event.hiImpedance = false;
    }

    @Override
    public AbstractUiComponent getComponent() {
        return keyboardUiComponent;
    }

    @AllArgsConstructor
    private static class KeyDescriptor {
        public int rMask;
        public int cMask;
        public int ncMask;
    }
}
