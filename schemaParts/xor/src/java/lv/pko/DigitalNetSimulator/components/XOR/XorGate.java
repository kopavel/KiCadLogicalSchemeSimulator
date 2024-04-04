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
package lv.pko.DigitalNetSimulator.components.XOR;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.pins.in.FloatingPinException;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;

public class XorGate extends Chip {
    private OutPin out;
    private boolean in1Active;
    private boolean in2Active;

    protected XorGate(String id, String sParam) {
        super(id, sParam);
        addInPin(new InPin("IN0", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                in1Active = (newState & mask) > 0;
                out.setState((in1Active ^ in2Active) ? hiState : loState);
            }
        });
        addInPin(new InPin("IN1", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (hiImpedance) {
                    throw new FloatingPinException(this);
                }
                in2Active = (newState & mask) > 0;
                out.setState((in1Active ^ in2Active) ? hiState : loState);
            }
        });
        addOutPin("OUT", 1);
    }

    @Override
    public void initOuts() {
        out = getOutPin("OUT");
        out.state = hiState;
        out.useBitPresentation = true;
    }
}
