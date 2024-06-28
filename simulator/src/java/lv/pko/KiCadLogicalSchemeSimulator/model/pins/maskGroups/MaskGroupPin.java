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
package lv.pko.KiCadLogicalSchemeSimulator.model.pins.maskGroups;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;

public class MaskGroupPin {
    public final InPin dest;
    public final long mask;
    public long state;

    public MaskGroupPin(InPin dest) {
        this.mask = dest.mask;
        this.dest = dest;
    }

    public void setHiImpedance() {
        state = 0;
        dest.state = 0;
        dest.onChange(0, true);
    }

    public void onChange(long newState) {
        long maskState = newState & mask;
        if (state != maskState) {
            state = maskState;
            dest.state = maskState;
            dest.onChange(maskState, false);
        }
    }

    public void onChangeForce(long newState) {
        long maskState = newState & mask;
        state = maskState;
        dest.state = maskState;
        dest.onChange(maskState, false);
    }

    public void resend(long newState, boolean hiImpedance) {
        long maskState = newState & mask;
        dest.state = maskState;
        dest.onChange(maskState, hiImpedance);
        state = maskState;
    }
}
