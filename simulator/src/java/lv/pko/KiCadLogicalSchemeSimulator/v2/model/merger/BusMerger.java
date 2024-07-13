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
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

//FixMe use one destination with splitter
//FixMe create pure base (no pin/strength) implementation
public class BusMerger extends OutBus implements IMerger {
    private final Map<ModelOutItem, DestinationDescriptor> sources = new HashMap<>();
    public MergerInput[] mergerInputs = new MergerInput[0];
    public long weakPins;
    public long strongPins;
    public long weakState;
    public byte[] weakStates = new byte[256];
    public String hash;

    public BusMerger(Bus destination) {
        super(destination.id, destination.parent, destination.size);
        destinations = new Bus[]{destination};
        hiImpedance = true;
    }

    @Override
    public void addDestination(IModelItem item, long mask, byte offset) {
        switch (item) {
            case OutPin ignored -> throw new RuntimeException("Use PinMerger for pin destination");
            case OutBus bus -> destinations = Utils.addToArray(destinations, bus);
            default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
        }
    }

    public String getHash() {
        StringBuilder result = new StringBuilder();
        for (MergerInput mergerInput : mergerInputs) {
            result.append(";").append(mergerInput.getHash());
        }
        return result.toString();
    }

    public void addSource(ModelOutItem src, long mask, byte offset) {
        MergerInput input;
        switch (src) {
            case Pin pin -> {
                long destinationMask = 1L << offset;
                input = new BusMergerPinIn(pin, destinationMask, offset, this);
                if (!pin.hiImpedance) {
                    if (pin.strong) {
                        if ((strongPins & destinationMask) != 0) {
                            throw new ShortcutException(this);
                        }
                        strongPins |= destinationMask;
                        if (pin.state) {
                            state |= destinationMask;
                        }
                    } else {
                        if ((weakStates[offset] > 0 && !pin.state) || (weakStates[offset] < 0 && pin.state)) {
                            throw new ShortcutException(pin);
                        }
                        weakPins |= destinationMask;
                        weakStates[offset] += (byte) (pin.state ? 1 : -1);
                        weakState |= pin.state ? destinationMask : 0;
                        if (pin.state && (strongPins & destinationMask) == 0) {
                            state |= destinationMask;
                        }
                    }
                }
            }
            case Bus bus -> {
                long destinationMask = offset == 0 ? mask : (offset > 0 ? mask << offset : mask >> -offset);
                input = new BusMergerBusIn(bus, destinationMask, this);
                if (!bus.hiImpedance) {
                    state |= bus.state;
                    strongPins |= destinationMask;
                }
            }
            default -> throw new RuntimeException("Unsupported item " + src.getClass().getName());
        }
        sources.put(src, new DestinationDescriptor(input, mask, offset));
        hash = getHash();
        mergerInputs = Utils.addToArray(mergerInputs, input);
        Arrays.sort(mergerInputs, Comparator.comparing(MergerInput::getName));
        hiImpedance = (strongPins | weakPins) != mask;
    }

    public void bindSources() {
        sources.forEach((source, descriptor) -> source.addDestination(descriptor.item, descriptor.mask, descriptor.offset));
    }

    @Override
    public void resend() {
        if ((strongPins | weakPins) != mask) {
            setHiImpedance();
        } else {
            setState(state);
        }
    }

    private record DestinationDescriptor(IModelItem item, long mask, byte offset) {
    }
}
