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
    private final Map<Integer, Map<Byte, OffsetBus>> corrected = new HashMap<>();
    public Bus[] destinations = new Bus[0];
    public int mask;

    public OutBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        mask = Utils.getMaskForSize(size);
    }

    /*Optimiser constructor unroll destination:destinations*/
    public OutBus(OutBus oldBus, String variantId) {
        super(oldBus, variantId);
        mask = oldBus.mask;
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    public void addDestination(Bus bus, int destMask, byte offset) {
        used = true;
        bus.used = true;
        bus.source = this;
        bus.state = state;
        bus.hiImpedance = hiImpedance;
        triStateIn = triStateIn || bus.triStateIn;
        if (offset != 0) {
            if (corrected.containsKey(destMask) && corrected.get(destMask).containsKey(offset)) {
                corrected.get(destMask).get(offset).addDestination(bus);
                return;
            } else {
                bus = new OffsetBus(this, bus, offset);
                corrected.computeIfAbsent(destMask, m -> new HashMap<>()).put(offset, (OffsetBus) bus);
            }
            int loweredMask;
            loweredMask = destMask | (offset > 0 ? 0 : Utils.getMaskForSize(-offset));
            if (!Integer.toBinaryString(loweredMask).contains("0")) {
                destMask = loweredMask;
            }
        }
        if (destMask == mask) {
            destinations = Utils.addToArray(destinations, bus);
        } else {
            int finalMask = destMask;
            Arrays.stream(destinations)
                    .filter(dest -> dest instanceof MaskGroupBus)
                    .map(dest -> ((MaskGroupBus) dest))
                    .filter(dest -> dest.mask == finalMask).findFirst().orElseGet(() -> {
                      MaskGroupBus groupBus = new MaskGroupBus(this, finalMask, "Mask" + finalMask);
                      destinations = Utils.addToArray(destinations, groupBus);
                      return groupBus;
                  }).addDestination(bus);
        }
        sort();
    }

    public void addDestination(Pin pin, int destMask) {
        used = true;
        pin.used = true;
        pin.state = (state & destMask) > 0;
        pin.hiImpedance = hiImpedance;
        triStateIn = triStateIn || pin.triStateIn;
        MaskGroupBus maskGroup = Arrays.stream(destinations)
                .filter(dest -> dest instanceof MaskGroupBus)
                .map(dest -> ((MaskGroupBus) dest))
                .filter(dest -> dest.mask == destMask).findFirst().orElseGet(() -> {
                    MaskGroupBus groupBus = new MaskGroupBus(this, destMask, "Mask" + destMask);
                    destinations = Utils.addToArray(destinations, groupBus);
                    return groupBus;
                });
        Arrays.stream(maskGroup.destinations)
                .filter(dest -> dest instanceof BusToWiresAdapter)
                .map(dest -> (BusToWiresAdapter) dest).findFirst().orElseGet(() -> {
                  BusToWiresAdapter busToWiresAdapter = new BusToWiresAdapter(this, destMask);
                  maskGroup.destinations = Utils.addToArray(maskGroup.destinations, busToWiresAdapter);
                  return busToWiresAdapter;
              }).addDestination(pin);
        sort();
    }

    @Override
    public void setState(int newState) {
        /*Optimiser line ts*/
        hiImpedance = false;
        state = newState;
        /*Optimiser block ar*/
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar*/
                for (Bus destination : destinations) {
                    destination.setState(newState);
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
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
                /*Optimiser line nr blockEnd r*/
                processing = 0;
                return;
            }
            case 2: {
                recurseError();
            }
        }
        /*Optimiser blockEnd ar*/
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        assert !hiImpedance : "Already in hiImpedance:" + this;
        hiImpedance = true;
        /*Optimiser block ar*/
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar*/
                for (Bus destination : destinations) {
                    destination.setHiImpedance();
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
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
                /*Optimiser line nr blockEnd r*/
                processing = 0;
                return;
            }
            case 2: {
                recurseError();
            }
        }
        /*Optimiser blockEnd ar blockEnd ts*/
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        }
    }

    @Override
    public Bus getOptimised(ModelItem<?> inSource) {
        if (destinations.length == 0) {
            return new NCBus(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised(inSource).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(this);
            }
            ClassOptimiser<OutBus> optimiser = new ClassOptimiser<>(this, OutBus.class).unroll(destinations.length);
            if (getRecursionMode() == none) {
                optimiser.cut("ar");
            } else if (getRecursionMode() == warn) {
                optimiser.cut("r");
            } else {
                optimiser.cut("nr");
            }
            if (!isTriState(inSource)) {
                optimiser.cut("ts");
            }
            OutBus build = optimiser.build();
            build.source = inSource;
            for (Bus destination : destinations) {
                destination.source = build;
            }
            return build;
        }
    }

    private void sort() {
        destinations = Stream.concat(Arrays.stream(destinations)
                        .filter(dest -> !(dest instanceof MaskGroupBus)),
                Arrays.stream(destinations)
                        .filter(dest -> dest instanceof MaskGroupBus)
                        .map(dest -> (MaskGroupBus) dest).sorted(Comparator.comparingLong(dest -> dest.mask))).toArray(Bus[]::new);
    }
}
