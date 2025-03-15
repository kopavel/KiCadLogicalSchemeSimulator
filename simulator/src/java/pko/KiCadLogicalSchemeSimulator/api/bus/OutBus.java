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
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.bus.BusToWiresAdapter;
import pko.KiCadLogicalSchemeSimulator.net.bus.MaskGroupBus;
import pko.KiCadLogicalSchemeSimulator.net.bus.NCBus;
import pko.KiCadLogicalSchemeSimulator.net.bus.OffsetBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

public class OutBus extends Bus {
    private final Map<Long, Map<Byte, OffsetBus>> corrected = new HashMap<>();
    public Bus[] destinations = new Bus[0];
    public long mask;

    public OutBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        mask = Utils.getMaskForSize(size);
    }

    /*Optimiser constructor unroll destination:destinations*/
    public OutBus(OutBus oldBus, String variantId) {
        super(oldBus, variantId);
        mask = oldBus.mask;
        hiImpedance = oldBus.hiImpedance;
        triState = oldBus.triState;
    }

    public void addDestination(Bus bus, long mask, byte offset) {
        used = true;
        bus.used = true;
        bus.triState = triState;
        if (offset != 0) {
            if (corrected.containsKey(mask) && corrected.get(mask).containsKey(offset)) {
                corrected.get(mask).get(offset).addDestination(bus);
                return;
            } else {
                bus = new OffsetBus(this, bus, offset);
                corrected.computeIfAbsent(mask, m -> new HashMap<>()).put(offset, (OffsetBus) bus);
            }
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
        sort();
    }

    public void addDestination(Pin pin, long mask) {
        used = true;
        pin.used = true;
        pin.triState = triState;
        MaskGroupBus maskGroup = Arrays.stream(destinations)
                .filter(d -> d instanceof MaskGroupBus)
                .map(d -> ((MaskGroupBus) d))
                .filter(d -> d.mask == mask).findFirst().orElseGet(() -> {
                    MaskGroupBus groupBus = new MaskGroupBus(this, mask, "Mask" + mask);
                    destinations = Utils.addToArray(destinations, groupBus);
                    return groupBus;
                });
        BusToWiresAdapter bus = Arrays.stream(maskGroup.destinations)
                .filter(d -> d instanceof BusToWiresAdapter)
                .map(d -> (BusToWiresAdapter) d).findFirst().orElseGet(() -> {
                    BusToWiresAdapter busToWiresAdapter = new BusToWiresAdapter(this, mask);
                    maskGroup.destinations = Utils.addToArray(maskGroup.destinations, busToWiresAdapter);
                    return busToWiresAdapter;
                });
        bus.addDestination(pin);
        sort();
    }

    @Override
    public void setState(long newState) {
        /*Optimiser line ts*/
        hiImpedance = false;
        state = newState;
        /*Optimiser block allRecurse*/
        if (processing) {
            /*Optimiser line recurse*/
            if (hasQueue) {
                if (recurseError()) {
                    return;
                }
                /*Optimiser block recurse*/
            }
            hasQueue = true;
            /*Optimiser blockEnd recurse*/
        } else {
            processing = true;
            /*Optimiser blockEnd allRecurse*/
            for (Bus destination : destinations) {
                destination.setState(newState);
            }
            /*Optimiser block recurse block allRecurse*/
            while (hasQueue) {
                hasQueue = false;
                /*Optimiser block ts*/
                if (hiImpedance) {
                    for (Bus destination : destinations) {
                        destination.setHiImpedance();
                    }
                } else {
                    /*Optimiser blockEnd ts*/
                    for (Bus destination : destinations) {
                        destination.setState(state);
                    }
                    /*Optimiser line ts*/
                }
            }
            /*Optimiser blockEnd recurse*/
            processing = false;
        }
        /*Optimiser blockEnd allRecurse*/
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser line ts block noTs*/
        if (!triState) {
            throw new RuntimeException("setImpedance on non tri-state OutBus");
            /*Optimiser block ts*/
        }
        /*Optimiser blockEnd noTs*/
        assert !hiImpedance : "Already in hiImpedance:" + this;
        hiImpedance = true;
        /*Optimiser block allRecurse*/
        if (processing) {
            /*Optimiser line recurse*/
            if (hasQueue) {
                if (recurseError()) {
                    return;
                }
                /*Optimiser block recurse*/
            }
            hasQueue = true;
            /*Optimiser blockEnd recurse*/
        } else {
            processing = true;
            /*Optimiser blockEnd allRecurse*/
            for (Bus destination : destinations) {
                destination.setHiImpedance();
            }
            /*Optimiser block recurse block allRecurse*/
            while (hasQueue) {
                hasQueue = false;
                if (hiImpedance) {
                    for (Bus destination : destinations) {
                        destination.setHiImpedance();
                    }
                } else {
                    for (Bus destination : destinations) {
                        destination.setState(state);
                    }
                }
            }
            /*Optimiser blockEnd recurse*/
            processing = false;
        }
        /*Optimiser blockEnd allRecurse blockEnd ts*/
    }

    @Override
    public Bus copyState(IModelItem<Bus> oldBus) {
        super.copyState(oldBus);
        hiImpedance = oldBus.isHiImpedance();
        return this;
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        }
    }

    @Override
    public Bus getOptimised(ModelItem<?> source) {
        if (destinations.length == 0) {
            return new NCBus(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised(null).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(this);
            }
            ClassOptimiser<OutBus> optimiser = new ClassOptimiser<>(this, OutBus.class).unroll(destinations.length);
            if (getRecursionMode() == none) {
                optimiser.cut("allRecurse");
            } else if (getRecursionMode() == warn) {
                optimiser.cut("recurse");
            }
            if (triState) {
                optimiser.cut("noTs");
            } else {
                optimiser.cut("ts");
            }
            OutBus build = optimiser.build();
            build.source = source;
            for (Bus destination : destinations) {
                destination.source = build;
            }
            return build;
        }
    }

    private void sort() {
        destinations = Stream.concat(Arrays.stream(destinations)
                        .filter(d -> !(d instanceof MaskGroupBus)),
                Arrays.stream(destinations)
                        .filter(d -> d instanceof MaskGroupBus)
                        .map(d -> (MaskGroupBus) d).sorted(Comparator.comparingLong(d -> d.mask))).toArray(Bus[]::new);
    }
}
