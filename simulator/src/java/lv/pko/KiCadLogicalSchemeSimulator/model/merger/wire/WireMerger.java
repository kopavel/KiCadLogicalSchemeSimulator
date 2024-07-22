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
package lv.pko.KiCadLogicalSchemeSimulator.model.merger.wire;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.ModelOutItem;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.bus.OutBus;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.InBus;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.PassivePin;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.DestinationDescriptor;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.IMerger;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.MergerInput;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class WireMerger extends OutPin implements IMerger {
    private final Map<ModelOutItem, DestinationDescriptor> sources = new HashMap<>();
    public MergerInput[] mergerInputs = new MergerInput[0];
    public byte weakState;
    public String hash;

    public WireMerger(Pin destination) {
        super(destination.id, destination.parent);
        destinations = new Pin[]{destination};
    }

    @Override
    public void addDestination(IModelItem item, long mask, byte offset) {
        switch (item) {
            case Pin pin -> {
                if (destinations.length == 1 && destinations[0] instanceof PassivePin passivePin) {
                    passivePin.addDestination(item, mask, offset);
                } else {
                    destinations = Utils.addToArray(destinations, pin);
                }
            }
            case InBus ignored -> throw new RuntimeException("Use BusMerger for bus destination");
            default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
        }
    }

    public String getHash() {
        return Arrays.stream(mergerInputs)
                .map(MergerInput::getHash)
                .collect(Collectors.joining(";"));
    }

    public void addSource(ModelOutItem src, long mask, byte offset) {
        if (src instanceof PassivePin passivePin) {
            passivePin.destinations = destinations;
            destinations = new Pin[]{passivePin};
        } else {
            MergerInput input;
            switch (src) {
                case OutPin pin -> {
                    input = new WireMergerWireIn(pin, this);
                    if (!pin.hiImpedance) {
                        hiImpedance = false;
                        if (pin.strong) {
                            if (strong) {
                                Set<ModelOutItem> items = new HashSet<>(sources.keySet());
                                items.add(src);
                                throw new ShortcutException(items.toArray(new ModelOutItem[0]));
                            }
                            strong = true;
                            state = pin.state;
                        } else {
                            if ((weakState > 0 && !pin.state) || (weakState < 0 && pin.state)) {
                                Set<ModelOutItem> items = new HashSet<>(sources.keySet());
                                items.add(src);
                                throw new ShortcutException(items.toArray(new ModelOutItem[0]));
                            }
                            weakState += (byte) (pin.state ? 1 : -1);
                            if (!strong) {
                                state = pin.state;
                            }
                        }
                    }
                }
                case OutBus bus -> {
                    input = new WireMergerBusIn(bus, mask, this);
                    if (!bus.hiImpedance) {
                        if (strong) {
                            Set<ModelOutItem> items = new HashSet<>(sources.keySet());
                            items.add(src);
                            throw new ShortcutException(items.toArray(new ModelOutItem[0]));
                        }
                        strong = true;
                        state = (bus.state & mask) > 0;
                    }
                }
                default -> throw new RuntimeException("Unsupported item " + src.getClass().getName());
            }
            sources.put(src, new DestinationDescriptor(input, mask, offset));
            mergerInputs = Utils.addToArray(mergerInputs, input);
            Arrays.sort(mergerInputs, Comparator.comparing(MergerInput::getName));
            hash = getHash();
        }
    }

    public void bindSources() {
        sources.forEach((source, descriptor) -> source.addDestination(descriptor.item, descriptor.mask, descriptor.offset));
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state, strong);
        }
    }

    @Override
    public Pin getOptimised() {
        return this;
    }
}
