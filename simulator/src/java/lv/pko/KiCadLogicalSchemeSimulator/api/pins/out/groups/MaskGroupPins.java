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
package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.groups;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MaskGroupPins extends MaskGroupPin {
    public InPin[] dest;

    public MaskGroupPins(MaskGroupPin oldItem) {
        super(oldItem.dest);
        this.dest = new InPin[]{oldItem.dest};
    }

    public void addDest(InPin pin) {
        dest = Utils.addToArray(dest, pin);
    }

    public void onChange(long newState, boolean hiImpedance) {
        long maskState = newState & mask;
        if (oldVal != maskState || oldImpedance != hiImpedance) {
            oldVal = maskState;
            oldImpedance = hiImpedance;
            for (InPin inPin : dest) {
                inPin.state = maskState;
                inPin.onChange(maskState, hiImpedance);
            }
        }
    }

    public void onChange(long newState) {
        long maskState = newState & mask;
        if (oldVal != maskState) {
            oldVal = maskState;
            for (InPin inPin : dest) {
                inPin.state = maskState;
                inPin.onChange(maskState, false);
            }
        }
    }

    @Override
    public void resend(long newState, boolean hiImpedance) {
        RuntimeException result = null;
        long maskState = newState & mask;
        for (InPin inPin : dest) {
            try {
                inPin.state = maskState;
                inPin.onChange(maskState, hiImpedance);
            } catch (FloatingPinException | ShortcutException e) {
                if (result == null) {
                    result = e;
                }
            }
        }
        if (result != null) {
            throw result;
        }
        oldVal = maskState;
        oldImpedance = hiImpedance;
    }
}
