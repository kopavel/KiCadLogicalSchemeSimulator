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
package lv.pko.DigitalNetSimulator.components.BUF;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.pins.in.FallingEdgeInPin;
import lv.pko.DigitalNetSimulator.api.pins.in.FloatingPinException;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.api.pins.out.TriStateOutPin;

public class Buffer extends Chip {
    private final int pinAmount;
    private final boolean isLatch;
    private final InPin dPin;
    private TriStateOutPin qPin;
    private long latch;
    private boolean oeState;

    public Buffer(String id, String sParam) {
        super(id, sParam);
        isLatch = params.containsKey("latch");
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        try {
            pinAmount = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (pinAmount < 1) {
            throw new RuntimeException("Component " + id + " size  must be positive number");
        }
        if (pinAmount > 64) {
            throw new RuntimeException("Component " + id + " size  must be less then 64");
        }
        addInPin(new InPin(isLatch ? "~{OE}" : "~{CS}", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                oeState = (newState == 0);
                if (oeState) {
                    qPin.setState(isLatch ? latch : dPin.rawState);
                } else {
                    qPin.setHiImpedance();
                }
            }
        });
        dPin = addInPin(new InPin("D", this, pinAmount) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (!isLatch && oeState) {
                    if (hiImpedance) {
                        throw new FloatingPinException(this);
                    }
                    qPin.setState(newState);
                }
            }
        });
        addTriStateOutPin("Q", pinAmount);
        if (isLatch) {
            addInPin(new FallingEdgeInPin("~{WR}", this) {
                @Override
                public void onFallingEdge() {
                    latch = dPin.rawState;
                    if (oeState) {
                        qPin.setState(latch);
                    }
                }
            });
        }
    }

    @Override
    public String extraState() {
        return "D:" + String.format("%" + (int) Math.ceil(pinAmount / 4d) + "X", dPin.rawState) + "\nQ:" +
                String.format("%" + (int) Math.ceil(pinAmount / 4d) + "X", qPin.state);
    }

    @Override
    public void initOuts() {
        qPin = (TriStateOutPin) getOutPin("Q");
    }
}
