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
package pko.KiCadLogicalSchemeSimulator.api_v2.bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.ModelOutItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.model.bus.BusToWireAdapter;
import pko.KiCadLogicalSchemeSimulator.model.bus.MaskGroupBus;
import pko.KiCadLogicalSchemeSimulator.model.bus.NCBus;
import pko.KiCadLogicalSchemeSimulator.model.bus.OffsetBus;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Arrays;

public class OutBus extends Bus implements ModelOutItem {
    public Bus[] destinations = new Bus[0];
    public long mask;

    public OutBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        mask = Utils.getMaskForSize(size);
    }

    public OutBus(OutBus oldPin, String variantId) {
        super(oldPin, variantId);
        mask = oldPin.mask;
    }

    public void addDestination(IModelItem item, long mask, byte offset) {
        switch (item) {
            case Pin pin -> {
                BusToWireAdapter bus = new BusToWireAdapter(pin, mask);
                Arrays.stream(destinations)
                        .filter(d -> d instanceof MaskGroupBus)
                        .map(d -> ((MaskGroupBus) d))
                        .filter(d -> d.mask == bus.mask).findFirst().orElseGet(() -> {
                          MaskGroupBus groupBus = new MaskGroupBus(this, bus.mask, "Mask" + mask);
                          destinations = Utils.addToArray(destinations, groupBus);
                          return groupBus;
                      }).addDestination(bus);
            }
            case Bus bus -> {
                //FixMe group by offset
                if (bus instanceof CorrectedInBus && offset != 0) {
                    bus = new OffsetBus(bus, offset);
                }
                if (mask != this.mask) {
                    Arrays.stream(destinations)
                            .filter(d -> d instanceof MaskGroupBus)
                            .map(d -> ((MaskGroupBus) d))
                            .filter(d -> d.mask == mask).findFirst().orElseGet(() -> {
                              MaskGroupBus groupBus = new MaskGroupBus(this, mask, "Mask" + mask);
                              destinations = Utils.addToArray(destinations, groupBus);
                              return groupBus;
                          }).addDestination(bus);
                } else {
                    destinations = Utils.addToArray(destinations, bus);
                }
            }
            default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
        }
    }

    @Override
    public void setState(long newState) {
        for (Bus destination : destinations) {
            destination.setState(newState);
        }
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this;
        for (Bus destination : destinations) {
            destination.setHiImpedance();
        }
    }

    public void resend() {
        if (!hiImpedance) {
            for (Bus destination : destinations) {
                destination.state = state;
                destination.setState(state);
            }
        }
    }

    @Override
    public Bus getOptimised() {
        if (destinations.length == 0) {
            return new NCBus(this);
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
