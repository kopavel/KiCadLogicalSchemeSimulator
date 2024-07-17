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
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.in.InBus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.in.InPin;

import java.util.*;
import java.util.stream.Collectors;

//FixMe use one destination with splitter
public class PinMerger extends OutPin implements IMerger {
    private final Map<ModelOutItem, DestinationDescriptor> sources = new HashMap<>();
    public MergerInput[] mergerInputs = new MergerInput[0];
    public byte weakState;
    public String hash;

    public PinMerger(Pin dest) {
        super(dest.id, dest.parent);
        destinations = new Pin[]{dest};
    }

    @Override
    public void addDestination(IModelItem item, long mask, byte offset) {
        switch (item) {
            case InPin pin -> destinations = Utils.addToArray(destinations, pin);
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
        MergerInput input;
        switch (src) {
            case Pin pin -> {
                input = new PinMergerPinIn(pin, this);
                if (!pin.hiImpedance) {
                    hiImpedance = false;
                    if (pin.strong) {
                        if (strong) {
                            Set<ModelOutItem> items = sources.keySet();
                            items.add(src);
                            throw new ShortcutException(items.toArray(new ModelOutItem[0]));
                        }
                        strong = true;
                        state = pin.state;
                    } else {
                        if ((weakState > 0 && !pin.state) || (weakState < 0 && pin.state)) {
                            Set<ModelOutItem> items = sources.keySet();
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
            case Bus bus -> {
                input = new PinMergerBusIn(bus, mask, this);
                if (!bus.hiImpedance) {
                    if (strong) {
                        Set<ModelOutItem> items = sources.keySet();
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

    public void bindSources() {
        sources.forEach((source, descriptor) -> source.addDestination(descriptor.item, descriptor.mask, descriptor.offset));
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state, strong);
        }
    }

    private record DestinationDescriptor(IModelItem item, long mask, byte offset) {
    }
}
