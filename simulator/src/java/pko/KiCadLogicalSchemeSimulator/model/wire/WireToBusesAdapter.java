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
package pko.KiCadLogicalSchemeSimulator.model.wire;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class WireToBusesAdapter extends OutPin {
    public long mask;
    private Bus[] destinations;

    public WireToBusesAdapter(Pin src, Bus[] destinations, byte offset) {
        this(src.id, src.parent, destinations, offset);
    }

    public WireToBusesAdapter(String id, SchemaPart parent, Bus[] destinations, byte offset) {
        super(id, parent);
        mask = 1L << offset;
        this.destinations = destinations;
    }

    @Override
    public void addDestination(Bus bus, byte offset) {
        if (mask != (1L << offset)) {
            throw new RuntimeException("Can't add bus with offset " + offset + " to adapter with mask " + mask);
        }
        destinations = Utils.addToArray(destinations, bus);
    }

    @Override
    public void addDestination(Pin pin) {
        throw new UnsupportedOperationException("Can't add Pin as destination to WireToBusesAdapter");
    }

    @Override
    public Pin getOptimised() {
        if (destinations.length == 0) {
            return new NCWire(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return this;
        }
    }

    @Override
    public void setState(boolean newState, boolean strong) {
        for (Bus destination : destinations) {
            destination.setState(newState ? mask : 0);
        }
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this;
        for (Bus destination : destinations) {
            destination.setHiImpedance();
        }
    }
}
