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
package lv.pko.KiCadLogicalSchemeSimulator.components.jnCounter;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.EdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FallingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class JnCounter extends SchemaPart {
    private OutPin outPin;
    private OutPin carryOutPin;
    private final int coMax;
    private int countMax = 1;
    private int count = 1;
    private boolean clockEnabled = true;

    protected JnCounter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int pinAmount;
        try {
            pinAmount = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must positive >=2 number");
        }
        if (pinAmount < 2) {
            throw new RuntimeException("Component " + id + " size must positive >=2 number");
        }
        for (int i = 1; i < pinAmount; i++) {
            countMax = countMax << 1;
        }
        coMax = countMax / 2;
        addOutPin("Q", pinAmount);
        addOutPin("CO", pinAmount);
        addInPin(new EdgeInPin("CI", this) {
            @Override
            public void onFallingEdge() {
                clockEnabled = true;
            }

            @Override
            public void onRisingEdge() {
                clockEnabled = false;
            }
        });
        if (reverse) {
            addInPin(new FallingEdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    if (clockEnabled) {
                        if (count >= countMax) {
                            count = 1;
                            carryOutPin.setState(0);
                        } else {
                            count = count << 1;
                            carryOutPin.setState(count >= coMax ? 1 : 0);
                        }
                        outPin.setState(count);
                    }
                }
            });
            addInPin(new EdgeInPin("R", this) {
                @Override
                public void onFallingEdge() {
                    clockEnabled = false;
                    count = 1;
                    outPin.setState(count);
                }

                @Override
                public void onRisingEdge() {
                    clockEnabled = true;
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("C", this) {
                @Override
                public void onRisingEdge() {
                    if (clockEnabled) {
                        if (count >= countMax) {
                            count = 1;
                            carryOutPin.setState(0);
                        } else {
                            count = count << 1;
                            carryOutPin.setState(count >= coMax ? 1 : 0);
                        }
                        outPin.setState(count);
                    }
                }
            });
            addInPin(new EdgeInPin("R", this) {
                @Override
                public void onFallingEdge() {
                    clockEnabled = true;
                }

                @Override
                public void onRisingEdge() {
                    clockEnabled = false;
                    count = 1;
                    outPin.setState(count);
                }
            });
        }
    }

    @Override
    public void initOuts() {
        outPin = getOutPin("Q");
        outPin.useBitPresentation = true;
        carryOutPin = getOutPin("CO");
    }

    @Override
    public void reset() {
        count = 1;
        outPin.setState(1);
    }
}
