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
package pko.KiCadLogicalSchemeSimulator.model.merger.bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.ModelOutItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.model.bus.BusInInterconnect;
import pko.KiCadLogicalSchemeSimulator.model.merger.DestinationDescriptor;
import pko.KiCadLogicalSchemeSimulator.model.merger.IMerger;
import pko.KiCadLogicalSchemeSimulator.model.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.*;
import java.util.stream.Collectors;

//FixMe use one destination with splitter
//FixMe create pure bus (no pin/strength) implementation
public class BusMerger extends OutBus implements IMerger {
    public final Map<ModelOutItem, DestinationDescriptor> forBind = new HashMap<>();
    public MergerInput[] inputs = new MergerInput[0];
    public long strongPins;
    public long weakPins;
    public long weakState;
    public Map<Byte, BusMergerWireIn> wires = new HashMap<>();
    public String hash;

    public BusMerger(Bus destination) {
        super(destination.id, destination.parent, destination.size);
        variantId = destination.variantId == null ? "" : destination.variantId + ":";
        variantId += "merger";
        if (destination instanceof BusInInterconnect interconnect) {
            this.mask &= interconnect.inverseInterconnectMask;
            this.mask |= interconnect.senseMask;
        }
        destinations = new Bus[]{destination};
    }

    @Override
    public void addDestination(IModelItem item, long mask, byte offset) {
        switch (item) {
            case InPin ignored -> throw new RuntimeException("Use WireMerger for pin destination");
            case BusInInterconnect interconnect -> {
                this.mask &= interconnect.inverseInterconnectMask;
                this.mask |= mask;
                destinations = Utils.addToArray(destinations, interconnect);
            }
            case InBus bus -> destinations = Utils.addToArray(destinations, bus);
            default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
        }
        id += "/" + item.getName();
        wires.values().forEach(i -> i.id = id + ":in");
    }

    public String getHash() {
        return "mask" + mask + ":" + Arrays.stream(inputs)
                .map(MergerInput::getHash)
                .collect(Collectors.joining(";"));
    }

    public void addSource(ModelOutItem src, long srcMask, byte offset) {
        MergerInput input = null;
        switch (src) {
            case OutPin pin -> {
                long destinationMask = 1L << offset;
                if (wires.containsKey(offset)) {
                    wires.get(offset).addSource(pin);
                } else {
                    input = new BusMergerWireIn(pin, destinationMask, offset, this);
                    wires.put(offset, (BusMergerWireIn) input);
                }
                if (!pin.hiImpedance) {
                    if (pin.strong) {
                        if ((strongPins & destinationMask) != 0) {
                            Set<ModelOutItem> items = new HashSet<>(forBind.keySet());
                            items.add(src);
                            throw new ShortcutException(items.toArray(new ModelOutItem[0]));
                        }
                        strongPins |= destinationMask;
                        if (pin.state) {
                            state |= destinationMask;
                        }
                    } else {
                        if ((weakPins & destinationMask) > 0 && ((weakState & destinationMask) > 0) != pin.state) {
                            Set<ModelOutItem> items = new HashSet<>(forBind.keySet());
                            items.add(src);
                            throw new ShortcutException(items.toArray(new ModelOutItem[0]));
                        }
                        weakPins |= destinationMask;
                        weakState |= pin.state ? destinationMask : 0;
                        if (pin.state && (strongPins & destinationMask) == 0) {
                            state |= destinationMask;
                        }
                    }
                }
            }
            case OutBus bus -> {
                long destinationMask = offset == 0 ? srcMask : (offset > 0 ? srcMask << offset : srcMask >> -offset);
                input = new BusMergerBusIn(bus, destinationMask, this);
                if (!bus.hiImpedance) {
                    if ((strongPins & destinationMask) != 0) {
                        Set<ModelOutItem> items = new HashSet<>(forBind.keySet());
                        items.add(src);
                        throw new ShortcutException(items.toArray(new ModelOutItem[0]));
                    }
                    state |= bus.state;
                    strongPins |= destinationMask;
                }
                forBind.put(src, new DestinationDescriptor(input, srcMask, offset));
            }
            default -> throw new RuntimeException("Unsupported item " + src.getClass().getName());
        }
        if (input != null) {
            inputs = Utils.addToArray(inputs, input);
            Arrays.sort(inputs, Comparator.comparing(MergerInput::getName));
        }
        hiImpedance = (strongPins | weakPins) != mask;
        hash = getHash();
    }

    public void bind() {
        forBind.forEach((source, descriptor) -> source.addDestination(descriptor.item, descriptor.mask, descriptor.offset));
    }

    @Override
    public void resend() {
        if ((strongPins | weakPins) == mask) {
            setState(state);
        }
    }

    @Override
    public Bus getOptimised() {
        throw new UnsupportedOperationException();
    }
}
