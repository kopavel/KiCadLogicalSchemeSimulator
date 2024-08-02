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
package pko.KiCadLogicalSchemeSimulator.api.bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.model.bus.BusToWiresAdapter;
import pko.KiCadLogicalSchemeSimulator.model.bus.MaskGroupBus;
import pko.KiCadLogicalSchemeSimulator.model.bus.NCBus;
import pko.KiCadLogicalSchemeSimulator.model.bus.OffsetBus;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OutBus extends Bus {
    private final List<Pin> pins = new ArrayList<>();
    public Bus[] destinations = new Bus[0];
    public long mask;

    public OutBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        mask = Utils.getMaskForSize(size);
    }

    public OutBus(OutBus oldBus, String variantId) {
        super(oldBus, variantId);
        mask = oldBus.mask;
    }

    public void addDestination(Bus bus, long mask, byte offset) {
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

    public void addDestination(Pin pin, long mask) {
        assert !pins.contains(pin);
        pins.add(pin);
        Arrays.stream(destinations)
                .filter(d -> d instanceof BusToWiresAdapter)
                .map(d -> ((BusToWiresAdapter) d))
                .filter(d -> d.mask == mask).findFirst().orElseGet(() -> {
                  BusToWiresAdapter adapter = new BusToWiresAdapter(this, mask);
                  destinations = Utils.addToArray(destinations, adapter);
                  return adapter;
              }).addDestination(pin);
    }

    @Override
    public void setState(long newState) {
        for (Bus destination : destinations) {
            destination.setState(state);
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
            Bus optimised = destinations[0].getOptimised();
            optimised.state = state;
            optimised.hiImpedance = hiImpedance;
            return optimised;
        } else if (destinations.length == 2) {
            Bus d1 = destinations[0].getOptimised();
            Bus d2 = destinations[1].getOptimised();
            return new OutBus(this, "unroll2") {
                @Override
                public void setState(long state) {
                    d1.setState(state);
                    d2.setState(state);
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                }
            };
        } else if (destinations.length == 3) {
            Bus d1 = destinations[0].getOptimised();
            Bus d2 = destinations[1].getOptimised();
            Bus d3 = destinations[2].getOptimised();
            return new OutBus(this, "unroll3") {
                @Override
                public void setState(long state) {
                    d1.setState(state);
                    d2.setState(state);
                    d3.setState(state);
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                }
            };
        } else if (destinations.length == 4) {
            Bus d1 = destinations[0].getOptimised();
            Bus d2 = destinations[1].getOptimised();
            Bus d3 = destinations[2].getOptimised();
            Bus d4 = destinations[3].getOptimised();
            return new OutBus(this, "unroll4") {
                @Override
                public void setState(long state) {
                    d1.setState(state);
                    d2.setState(state);
                    d3.setState(state);
                    d4.setState(state);
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                }
            };
        } else if (destinations.length == 5) {
            Bus d1 = destinations[0].getOptimised();
            Bus d2 = destinations[1].getOptimised();
            Bus d3 = destinations[2].getOptimised();
            Bus d4 = destinations[3].getOptimised();
            Bus d5 = destinations[4].getOptimised();
            return new OutBus(this, "unroll5") {
                @Override
                public void setState(long state) {
                    d1.setState(state);
                    d2.setState(state);
                    d3.setState(state);
                    d4.setState(state);
                    d5.setState(state);
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                }
            };
        } else if (destinations.length == 6) {
            Bus d1 = destinations[0].getOptimised();
            Bus d2 = destinations[1].getOptimised();
            Bus d3 = destinations[2].getOptimised();
            Bus d4 = destinations[3].getOptimised();
            Bus d5 = destinations[4].getOptimised();
            Bus d6 = destinations[5].getOptimised();
            return new OutBus(this, "unroll6") {
                @Override
                public void setState(long state) {
                    d1.setState(state);
                    d2.setState(state);
                    d3.setState(state);
                    d4.setState(state);
                    d5.setState(state);
                    d6.setState(state);
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                    d6.setHiImpedance();
                }
            };
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return this;
        }
    }
}
