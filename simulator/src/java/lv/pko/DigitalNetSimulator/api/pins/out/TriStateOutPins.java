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
package lv.pko.DigitalNetSimulator.api.pins.out;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.tools.Utils;

public class TriStateOutPins extends TriStateOutPin {
    protected InPin[] dest = new InPin[0];

    public TriStateOutPins(TriStateOutPin oldPin) {
        super(oldPin.id, oldPin.parent, oldPin.size);
        aliases = oldPin.aliases;
    }

    public void addDest(InPin pin) {
        dest = Utils.addToArray(dest, pin);
    }

    public void setState(long newState) {
        long oldState = this.state;
        if (hiImpedance) {
            hiImpedance = false;
            this.state = newState;
            for (InPin InPin : dest) {
                InPin.transit(oldState, newState, false);
            }
        } else {
            if (newState != oldState) {
                this.state = newState;
                for (InPin InPin : dest) {
                    InPin.transit(oldState, newState, false);
                }
            }
        }
    }

    public void reSendState() {
        for (InPin pin : dest) {
            pin.transit(state, state, hiImpedance);
        }
    }

    public void setHiImpedance() {
        if (!hiImpedance) {
            hiImpedance = true;
            for (InPin InPin : dest) {
                InPin.transit(state, state, true);
            }
        }
    }
}
