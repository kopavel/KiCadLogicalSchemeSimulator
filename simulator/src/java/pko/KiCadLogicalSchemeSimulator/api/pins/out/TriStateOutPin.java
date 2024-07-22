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
package pko.KiCadLogicalSchemeSimulator.api.pins.out;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.EdgeInPin;
import pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class TriStateOutPin extends OutPin {
    public boolean hiImpedance = true;

    public TriStateOutPin(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
    }

    @Override
    public void addDestination(InPin pin) {
        if (pin instanceof EdgeInPin) {
            throw new RuntimeException("Edge pin on tri-state out");
        }
        destination = pin;
    }

    @Override
    public void setState(long newState) {
        if (hiImpedance) {
            hiImpedance = false;
            state = newState;
            destination.state = newState & destination.mask;
            destination.onChange(destination.state, false, true);
        } else if (newState != this.state) {
            state = newState;
            destination.state = newState & destination.mask;
            destination.onChange(destination.state, false, true);
        }
    }

    @Override
    public void setStateForce(long newState) {
        hiImpedance = false;
        state = newState;
        destination.state = newState & destination.mask;
        destination.onChange(destination.state, false, true);
    }

    @Override
    public void reSendState() {
        destination.state = state & destination.mask;
        destination.onChange(destination.state, hiImpedance, true);
    }

    public void setHiImpedance() {
        if (!hiImpedance) {
            hiImpedance = true;
            destination.state = 0;
            destination.onChange(destination.state, true, true);
        }
    }
}
