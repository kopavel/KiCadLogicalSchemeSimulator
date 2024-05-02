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
package lv.pko.DigitalNetSimulator.components.shifter;
import lv.pko.DigitalNetSimulator.api.pins.in.FloatingPinException;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;
import lv.pko.DigitalNetSimulator.api.schemaPart.SchemaPart;

public class Shifter extends SchemaPart {
    private final InPin inPins;
    private long latch = 0;
    private OutPin out;
    private int pos;
    private boolean wrActive;

    protected Shifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        inPins = addInPin("D", dSize);
        addInPin(new InPin("~{WR}", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                wrActive = (newState > 0) ^ reverse;
            }
        });
        addInPin(new RisingEdgeInPin("CLK", this) {
            @Override
            public void onRisingEdge() {
                if (pos < dSize) {
                    if (wrActive) {
                        latch = inPins.getState();
                        pos = 0;
                    } else {
                        pos++;
                    }
                    out.setState(latch >> pos & 1);
                } else if (pos == dSize) {
                    out.setState(0);
                }
            }
        });
        addOutPin("Q", 1);
    }

    @Override
    public void initOuts() {
        out = getOutPin("Q");
    }

    @Override
    public String extraState() {
        return "latch" + "\n" + Long.toBinaryString(latch);
    }
}
