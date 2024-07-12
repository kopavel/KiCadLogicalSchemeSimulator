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
package lv.pko.KiCadLogicalSchemeSimulator.v2.model.merger;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.IModelItem;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.ModelOutItem;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.Bus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.OutBus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class BusMerger extends OutBus {
    private final Map<ModelOutItem, DestinationDescriptor> sources = new HashMap<>();
    public long hiImpedancePins;
    public long weakState;
    public String hash;
    public MergerInput[] inputs;
    public byte[] weakStates = new byte[256];

    public BusMerger(Bus dest) {
        super(dest.id, dest.parent, dest.size);
        destinations = new Bus[]{dest};
        hiImpedance = true;
    }

    @Override
    public void addDestination(IModelItem item, long mask, byte offset) {
        switch (item) {
            case Pin ignored -> throw new RuntimeException("Use PinMerger for pin destination");
            case Bus bus -> destinations = Utils.addToArray(destinations, bus);
            default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
        }
    }

    public String getHash() {
        StringBuilder result = new StringBuilder();
        for (MergerInput input : inputs) {
            result.append(";").append(input.getHash());
        }
        return result.toString();
    }

    public void addSource(ModelOutItem src, long mask, byte offset) {
        MergerInput input;
        switch (src) {
            case Pin pin -> {
                long destinationMask = 1L << offset;
                input = new BusMergerInPin(pin, destinationMask, offset, this);
                if (pin.hiImpedance) {
                    hiImpedancePins |= destinationMask;
                } else {
                    if (!pin.strong) {
                        if ((weakStates[offset] > 0 && !pin.state) || (weakStates[offset] < 0 && pin.state)) {
                            throw new ShortcutException(pin);
                        }
                        weakStates[offset] += (byte) (pin.state ? 1 : -1);
                        weakState |= pin.state ? destinationMask : 0;
                    }
                    if (pin.state) {
                        state |= 1L << offset;
                    }
                }
            }
            case Bus bus -> {
                long destinationMask = offset == 0 ? mask : (offset > 0 ? mask << offset : mask >> -offset);
                input = new BusMergerInBus(bus, destinationMask, this);
                if (bus.hiImpedance) {
                    hiImpedancePins |= destinationMask;
                } else {
                    state |= bus.state;
                }
            }
            default -> throw new RuntimeException("Unsupported pin " + src.getClass().getName());
        }
        sources.put(src, new DestinationDescriptor(input, mask, offset));
        hash = getHash();
        inputs = Utils.addToArray(inputs, input);
        Arrays.sort(inputs, Comparator.comparing(MergerInput::getName));
    }

    public void bindSources() {
        sources.forEach((source, descriptor) -> source.addDestination(descriptor.item, descriptor.mask, descriptor.offset));
    }

    @Override
    public void resend() {
        if (hiImpedancePins > 0) {
            setHiImpedance();
        } else {
            setState(state);
        }
    }

    private record DestinationDescriptor(IModelItem item, long mask, byte offset) {
    }
}
