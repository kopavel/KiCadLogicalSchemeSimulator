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
package pko.KiCadLogicalSchemeSimulator.api_v2.wire;
import pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.ModelOutItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.model.wire.NCWire;
import pko.KiCadLogicalSchemeSimulator.model.wire.WireToBusAdapter;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

//FixMe use one destination with splitter
public class OutPin extends Pin implements ModelOutItem {
    public Pin[] destinations = new Pin[0];

    public OutPin(String id, SchemaPart parent) {
        super(id, parent);
    }

    public OutPin(OutPin oldPin, String variantId) {
        super(oldPin, variantId);
        strong = oldPin.strong;
    }

    public void addDestination(IModelItem item, long mask, byte offset) {
        if (destinations.length == 1 && destinations[0] instanceof PassivePin passivePin) {
            passivePin.addDestination(item, mask, offset);
        } else {
            switch (item) {
                case PassivePin passivePin -> {
                    passivePin.destinations = destinations;
                    destinations = new Pin[]{passivePin};
                }
                case Pin pin -> {
                    destinations = Utils.addToArray(destinations, pin);
                }
                case Bus bus -> destinations = Utils.addToArray(destinations, new WireToBusAdapter(this, bus, offset));
                default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
            }
        }
    }

    @Override
    public void setState(boolean newState, boolean strong) {
        for (Pin destination : destinations) {
            destination.setState(newState, strong);
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
            for (Pin destination : destinations) {
                destination.state = state;
                destination.strong = strong;
                destination.setState(state, strong);
            }
        }
    }

    @Override
    public Pin getOptimised() {
        if (destinations.length == 0) {
            return new NCWire(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised();
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return this;
        }
    }
}
