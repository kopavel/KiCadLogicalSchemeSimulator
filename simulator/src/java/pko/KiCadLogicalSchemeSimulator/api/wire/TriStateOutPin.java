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
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.wire.NCWire;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class TriStateOutPin extends OutPin {
    public TriStateOutPin(String id, SchemaPart parent) {
        super(id, parent);
        hiImpedance = true;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public TriStateOutPin(TriStateOutPin oldPin, String variantId) {
        super(oldPin, variantId);
        hiImpedance = oldPin.hiImpedance;
    }

    @Override
    public Pin copyState(IModelItem<Pin> oldPin) {
        super.copyState(oldPin);
        hiImpedance = oldPin.isHiImpedance();
        return this;
    }

    @Override
    public void setState(boolean newState) {
        hiImpedance = false;
        state = newState;
        for (Pin destination : destinations) {
            destination.setState(state);
        }
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this;
        hiImpedance = true;
        for (Pin destination : destinations) {
            destination.setHiImpedance();
        }
    }

    public void resend() {
        if (!hiImpedance) {
            setState(state);
        }
    }

    @Override
    public Pin getOptimised(boolean keepSetters) {
        if (destinations.length == 0) {
            return new NCWire(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised(keepSetters).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(false);
            }
            ClassOptimiser<TriStateOutPin> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
            return optimiser.build();
        }
    }
}
