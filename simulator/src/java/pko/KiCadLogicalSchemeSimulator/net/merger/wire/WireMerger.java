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
package pko.KiCadLogicalSchemeSimulator.net.merger.wire;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PullPin;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.net.merger.bus.BusMergerWireIn;
import pko.KiCadLogicalSchemeSimulator.net.wire.NCWire;

import java.util.*;

//Todo if there is no BusMerger in destinations - cut out hiImpedance processing
//Todo with one source only and only weak others - use simpler "Weak pin" implementation
public class WireMerger extends OutPin {
    public final Set<PassivePin> passivePins = new TreeSet<>();
    public final Set<BusMergerWireIn> mergers = new TreeSet<>();
    public Set<MergerInput<?>> sources = new TreeSet<>(Comparator.comparing(mergerInput -> mergerInput.getMask() + ":" + mergerInput.getName()));

    public WireMerger(Pin destination, List<OutPin> pins, List<PassivePin> passivePins, Map<OutBus, Long> buses) {
        super(destination.id, destination.parent);
        variantId = destination.variantId == null ? "" : destination.variantId + ":";
        variantId += "merger";
        destinations = new Pin[]{destination};
        strong = false;
        hiImpedance = true;
        if (destination instanceof PassivePin passivePin) {
            passivePin.merger = this;
        }
        pins.forEach(this::addSource);
        passivePins.forEach(this::addSource);
        if (buses != null) {
            buses.forEach(this::addSource);
        }
    }

    @Override
    public void addDestination(Pin pin) {
        super.addDestination(pin);
        if (!(pin instanceof NCWire)) {
            id += "/" + pin.getName();
        }
    }

    @Override
    public Pin getOptimised(boolean keepSetters) {
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(false);
        }
        return this;
    }

    private void addSource(OutBus bus, long mask) {
        WireMergerBusIn input = new WireMergerBusIn(bus, mask, this);
        bus.addDestination(input, mask, (byte) 0);
        sources.add(input);
        if (!bus.hiImpedance) {
            if (!hiImpedance) {
                throw new ShortcutException(sources);
            }
            hiImpedance = false;
            state = (bus.state & mask) != 0;
        }
    }

    private void addSource(OutPin pin) {
        if (pin instanceof PullPin pullPin) {
            sources.add(pullPin);
            if (weakState != 0 && pin.state != (weakState > 0)) {
                throw new ShortcutException(sources);
            }
            weakState += pin.state ? 1 : -1;
            if (hiImpedance) {
                state = weakState > 0;
                strong = false;
                hiImpedance = false;
            }
        } else if (pin instanceof PassivePin passivePin) {
            if (!passivePins.contains(passivePin)) {
                passivePins.add(passivePin);
                passivePin.merger = this;
                WireMergerWireIn input = new WireMergerWireIn(pin, this);
                pin.addDestination(input);
                sources.add(input);
                if (!passivePin.hiImpedance && !passivePin.strong) {
                    weakState += pin.state ? 1 : -1;
                }
            }
        } else {
            WireMergerWireIn input = new WireMergerWireIn(pin, this);
            pin.addDestination(input);
            sources.add(input);
            if (!pin.hiImpedance) {
                if (!hiImpedance && strong) {
                    throw new ShortcutException(sources);
                }
                strong = true;
                state = pin.state;
                hiImpedance = false;
            }
        }
    }
}
