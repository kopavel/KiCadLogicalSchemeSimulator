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
package lv.pko.KiCadLogicalSchemeSimulator.v2.model.pins;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pins.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pins.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pins.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pins.ShortcutException;

public class MaskGroupPins extends OutPin {
    public MaskGroupPins(OutPin source, long mask) {
        super(source);
        this.mask = mask;
    }

    public MaskGroupPins(MaskGroupPins oldPin) {
        super(oldPin);
        mask = oldPin.mask;
    }

    public void addDestination(Pin pin) {
        destinations = Utils.addToArray(destinations, pin);
    }

    public void setState(long newState, boolean strong) {
        long maskState = newState & mask;
        if (state != maskState) {
            state = maskState;
            for (Pin destination : destinations) {
                destination.setState(maskState, strong);
            }
        }
    }

    public void resend(long newState, boolean strong) {
        RuntimeException result = null;
        long maskState = newState & mask;
        for (Pin destination : destinations) {
            try {
                destination.setState(maskState, strong);
            } catch (FloatingPinException | ShortcutException e) {
                if (result == null) {
                    result = e;
                }
            }
        }
        if (result != null) {
            throw result;
        }
        state = maskState;
    }

    @Override
    public Pin getOptimised() {
        if (destinations == null) {
            return new NCOutPin(this);
        } else {
            return this;
        }
    }
}
