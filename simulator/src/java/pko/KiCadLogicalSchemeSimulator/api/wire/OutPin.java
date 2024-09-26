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

    public OutPin(String id, SchemaPart parent) {
        super(id, parent);
    }

    /*Optimiser constructor unroll destination:destinations*/
    public OutPin(OutPin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    public void addDestination(Pin pin) {
        assert pin != this;
        pin.triState = triState;
        if (!(pin instanceof NCWire)) {
            destinations = Utils.addToArray(destinations, pin);
        }
    }

    public void addDestination(Bus bus, byte offset) {
        bus.triState = triState;
        if (adapters.containsKey(offset)) {
            adapters.get(offset).addDestination(bus);
        } else {
            WireToBusesAdapter adapter = new WireToBusesAdapter(this.id, this.parent, bus, offset);
            adapter.triState = triState;
            adapters.put(offset, adapter);
            addDestination(adapter);
        }
    }

    @Override
    public void setState(boolean newState) {
        state = newState;
        for (Pin destination : destinations) {
            destination.setState(state);
        }
    }

    @Override
    public void setHiImpedance() {
        throw new RuntimeException("setImpedance on non tri-state OutPin");
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
            return destinations[0].getOptimised(true).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(false);
            }
            ClassOptimiser<OutPin> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
            return optimiser.build();
        }
    }
}
