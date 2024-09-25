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
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.bus.*;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

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
    }

    public void addDestination(Bus bus, long mask, byte offset) {
        if (offset != 0) {
            if (corrected.containsKey(mask) && corrected.get(mask).containsKey(offset)) {
                corrected.get(mask).get(offset).addDestination(bus);
                return;
            } else {
                if (offset > 0) {
                    bus = new OffsetBus(this, bus, offset);
                } else {
                    bus = new NegativeOffsetBus(this, bus, offset);
                }
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
        Arrays.stream(destinations)
                .filter(d -> d instanceof MaskGroupBus)
                .map(d -> ((MaskGroupBus) d))
                .filter(d -> d.mask == mask).findFirst().orElseGet(() -> {
                  MaskGroupBus groupBus = new MaskGroupBus(this, mask, "Mask" + mask);
                  destinations = Utils.addToArray(destinations, groupBus);
                  return groupBus;
              }).addDestination(new SimpleBusToWireAdapter(this, pin));
        sort();
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

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        }
    }

    @Override
    public Bus getOptimised() {
        if (destinations.length == 0) {
            return new NCBus(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised().copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return new ClassOptimiser<>(this).unroll(destinations.length).build();
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
