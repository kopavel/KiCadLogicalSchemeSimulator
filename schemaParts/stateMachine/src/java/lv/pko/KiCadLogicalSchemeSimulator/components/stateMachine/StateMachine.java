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
package lv.pko.KiCadLogicalSchemeSimulator.components.stateMachine;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.EdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class StateMachine extends SchemaPart {
    private final long[] states;
    private final InPin in;
    private boolean strobeActive;
    private boolean disabled;
    private OutPin out;
    private long latch;
    private long outMash;

    public StateMachine(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize;
        try {
            inSize = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (!params.containsKey("outSize")) {
            throw new RuntimeException("Component " + id + " has no parameter \"outSize\"");
        }
        int outSize;
        try {
            outSize = Integer.parseInt(params.get("outSize"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        addOutPin("OUT", outSize);
        if (!params.containsKey("states")) {
            throw new RuntimeException("Component " + id + " has no parameter \"states\"");
        }
        try {
            String[] split = params.get("states").split(",");
            int stateAmount = (int) Math.pow(2, inSize);
            if (stateAmount != split.length) {
                throw new RuntimeException("State amount not equal with inputs possible combinations amount");
            }
            states = new long[stateAmount];
            for (int i = 0; i < split.length; i++) {
                states[i] = Long.parseLong(split[i]);
            }
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " state must be positive number");
        }
        addOutPin("OUT", outSize);
        addInPin(new InPin("D", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                disabled = newState == 1;
                out.setState((disabled ? 0 : states[(int) latch]) ^ outMash);
            }
        });
        addInPin(new InPin("S", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                strobeActive = newState > 0;
                if (strobeActive) {
                    latch = in.getState();
                    out.setState((disabled ? 0 : states[(int) latch]) ^ outMash);
                }
            }
        });
        addInPin(new EdgeInPin("R", this) {
            @Override
            public void onFallingEdge() {
                outMash = 0;
                out.setState((disabled ? 0 : states[(int) latch]) ^ outMash);
            }

            @Override
            public void onRisingEdge() {
                outMash = -1;
                out.setState((disabled ? 0 : states[(int) latch]) ^ outMash);
            }
        });
        in = addInPin(new InPin("IN", this, inSize) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (strobeActive) {
                    if (hiImpedance) {
                        throw new FloatingPinException(this);
                    }
                    latch = newState;
                    out.setState((disabled ? 0 : states[(int) latch]) ^ outMash);
                }
            }
        });
        in.useBitPresentation = true;
    }

    @Override
    public void initOuts() {
        out = getOutPin("OUT");
        out.state = loState;
        out.useBitPresentation = true;
    }
}
