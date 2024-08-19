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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.wire.NCWire;
import pko.KiCadLogicalSchemeSimulator.net.wire.WireToBusesAdapter;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.HashMap;
import java.util.Map;

public class OutPin extends Pin {
    private final Map<Byte, WireToBusesAdapter> adapters = new HashMap<>();
    public Pin[] destinations = new Pin[0];
    //for wire merger(don't want export from module whole wire merger package)
    public int weakState;
    //for passive pin (don't lost it on optimisation)
    public OutPin merger;

    public OutPin(String id, SchemaPart parent) {
        super(id, parent);
    }

    /*Optimiser constructor unroll destination:destinations*/
    public OutPin(OutPin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    @Override
    public Pin copyState(IModelItem<Pin> oldPin) {
        if (oldPin instanceof OutPin outPin) {
            merger = outPin.merger;
        }
        return super.copyState(oldPin);
    }

    public void addDestination(Pin pin) {
        assert pin != this;
        destinations = Utils.addToArray(destinations, pin);
    }

    public void addDestination(Bus bus, byte offset) {
        if (adapters.containsKey(offset)) {
            adapters.get(offset).addDestination(bus);
        } else {
            WireToBusesAdapter adapter = new WireToBusesAdapter(this.id, this.parent, bus, offset);
            adapters.put(offset, adapter);
            addDestination(adapter);
        }
    }

    @Override
    public void setState(boolean newState) {
        for (Pin destination : destinations) {
            destination.setState(state);
        }
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this;
        for (Pin destination : destinations) {
            destination.setHiImpedance();
        }
    }

    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else {
            //noinspection ConstantValue,AssertWithSideEffects
            assert !(hiImpedance = false);
            setHiImpedance();
        }
    }

    @Override
    public Pin getOptimised() {
        if (destinations.length == 0) {
            return new NCWire(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised().copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return new ClassOptimiser<>(this).unroll(destinations.length).build();
        }
    }
}
