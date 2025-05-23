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
package pko.KiCadLogicalSchemeSimulator.components.stateMachine;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class StateMachine extends SchemaPart {
    private final int[] states;
    private final InBus in;
    private final InPin dPin;
    private final int mask;
    private Bus out;
    private boolean cActive;
    private int outState;
    private int outMask;

    public StateMachine(String id, String sParam) {
        super(id, sParam);
        //size
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize;
        try {
            inSize = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        //outsize
        if (!params.containsKey("outSize")) {
            throw new RuntimeException("Component " + id + " has no parameter \"outSize\"");
        }
        int outSize;
        try {
            outSize = Integer.parseInt(params.get("outSize"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        mask = Utils.getMaskForSize(outSize);
        //states
        if (!params.containsKey("states")) {
            throw new RuntimeException("Component " + id + " has no parameter \"states\"");
        }
        try {
            String[] split = params.get("states").split(",");
            int stateAmount = (int) Math.pow(2, inSize);
            if (stateAmount != split.length) {
                throw new RuntimeException("State amount not equal with inputs possible combinations amount");
            }
            states = new int[stateAmount];
            for (int i = 0; i < split.length; i++) {
                states[i] = Integer.parseInt(split[i]);
            }
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " state must be positive number");
        }
        addOutBus("OUT", outSize);
        dPin = addInPin(new InPin("D", this) {
            @Override
            public void setHi() {
                state = true;
                int newOutState = outMask;
                if (out.state != newOutState) {
                    out.setState(newOutState);
                }
            }

            @Override
            public void setLo() {
                state = false;
                int newOutState = (outState) ^ outMask;
                if (out.state != newOutState) {
                    out.setState(newOutState);
                }
            }
        });
        addInPin(new InPin("R", this) {
            @Override
            public void setHi() {
                state = true;
                {
                    outMask = mask;
                    int newOutState = (dPin.state ? 0 : outState) ^ outMask;
                    if (out.state != newOutState) {
                        out.setState(newOutState);
                    }
                }
            }

            @Override
            public void setLo() {
                state = false;
                outMask = 0;
                int newOutState = (dPin.state ? 0 : outState) ^ outMask;
                if (out.state != newOutState) {
                    out.setState(newOutState);
                }
            }
        });
        if (params.containsKey("cReverse")) {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    cActive = false;
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    cActive = true;
                    outState = states[in.state];
                    int newOutState = (dPin.state ? 0 : outState) ^ outMask;
                    if (out.state != newOutState) {
                        out.setState(newOutState);
                    }
                }
            });
            cActive = true;
        } else {
            addInPin(new InPin("C", this) {
                @Override
                public void setHi() {
                    state = true;
                    cActive = true;
                    outState = states[in.state];
                    int newOutState = (dPin.state ? 0 : outState) ^ outMask;
                    if (out.state != newOutState) {
                        out.setState(newOutState);
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    cActive = false;
                }
            });
        }
        if (params.containsKey("latch")) {
            in = addInBus("IN", inSize);
        } else {
            in = addInBus(new InBus("IN", this, inSize) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    if (cActive) {
                        outState = states[newState];
                        int newOutState = (dPin.state ? 0 : outState) ^ outMask;
                        if (out.state != newOutState) {
                            out.setState(newOutState);
                        }
                    }
                }
            });
        }
        in.useBitPresentation = true;
    }

    @Override
    public void initOuts() {
        out = getOutBus("OUT");
        out.state = reverse ? mask : 0;
        out.useBitPresentation = true;
    }
}
