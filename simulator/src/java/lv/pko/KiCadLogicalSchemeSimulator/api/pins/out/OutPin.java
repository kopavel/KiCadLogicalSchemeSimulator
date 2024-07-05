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
package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class OutPin extends Pin {
    public InPin dest;

    public OutPin(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        mask = Utils.getMaskForSize(size);
    }

    public void addDest(InPin pin) {
        dest = pin;
    }

    public void setState(long newState) {
        state = newState & dest.mask;
        if (dest.state != state) {
            dest.state = state;
            dest.onChange(state, false, true);
        }
    }

    public void setStateForce(long newState) {
        state = newState & dest.mask;
        dest.state = state;
        dest.onChange(state, false, true);
    }

    public void reSendState() {
        if (dest != null) {
            dest.state = state & dest.mask;
            dest.onChange(dest.state, false, true);
        }
    }

    public boolean noDest() {
        return dest == null;
    }
}
