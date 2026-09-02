/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.merger.bus;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PullPin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.bus.BusInInterconnect;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.*;

public class BusMerger extends OutBus {
    public final Set<MergerInput<?>> sources = new TreeSet<>(Comparator.comparing(mergerInput -> mergerInput.getMask() + ":" + mergerInput.getName()));
    public int strongPins;
    public int weakPins;
    public int weakState;

    public BusMerger(Bus destination) {
        super(destination.id, destination.parent, destination.size);
        variantId = destination.variantId == null ? "" : destination.variantId + ":";
        variantId += "merger";
        destination.used = true;
        destination.source = this;
        Bus currentBus = destination;
        while (currentBus instanceof BusInInterconnect interconnect) {
            mask &= ~interconnect.interconnectMask;
            mask |= interconnect.senseMask;
            currentBus = interconnect.destination;
        }
        destinations = new Bus[]{destination};
    }

    public void addDestination(Bus destination) {
        destination.used = true;
        destination.source = this;
        switch (destination) {
            case BusInInterconnect interconnect -> {
                mask &= ~interconnect.interconnectMask;
                mask |= interconnect.senseMask;
                destinations = Utils.addToArray(destinations, interconnect);
            }
            case InBus bus -> destinations = Utils.addToArray(destinations, bus);
            default -> throw new RuntimeException("Unsupported destination " + destination.getClass().getName());
        }
        id += "/" + destination.getName();
    }

    public void addSource(OutBus bus, int srcMask, byte offset) {
        bus.used = true;
        int destinationMask = offset == 0 ? srcMask : (offset > 0 ? srcMask << offset : srcMask >> -offset);
        BusMergerBusIn input = new BusMergerBusIn(bus, destinationMask, this);
        bus.addDestination(input, srcMask, offset);
        sources.add(input);
        if (!bus.hiImpedance) {
            if ((strongPins & destinationMask) != 0) {
                throw new ShortcutException(this, bus.state, sources);
            }
            state |= bus.state;
            strongPins |= destinationMask;
        }
    }

    public void addSource(OutPin pin, byte offset) {
        int destinationMask = 1 << offset;
        pin.used = true;
        if (pin instanceof PullPin pullPin) {
            if ((weakPins & destinationMask) != 0 && ((weakState & destinationMask) > 0) != pin.state) {
                throw new ShortcutException(this, weakState, sources);
            }
            weakPins |= destinationMask;
            if (pin.state) {
                weakState |= destinationMask;
            }
            sources.add(pullPin);
        } else {
            BusMergerWireIn input = new BusMergerWireIn(destinationMask, this);
            pin.addDestination(input);
            processPin(pin, input, destinationMask);
        }
    }

    public void addSource(Net net, Set<? extends OutPin> pins, List<? extends PassivePin> passivePins, Byte offset) {
        int destinationMask = 1 << offset;
        BusMergerWireIn input = new BusMergerWireIn(destinationMask, this);
        Pin pin = net.processWire(input, pins, passivePins, Collections.emptyMap());
        processPin(pin, input, destinationMask);
    }

    @Override
    public Bus getOptimised(ModelItem<?> inSource) {
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(this);
        }
        return this;
    }

    private void processPin(Pin pin, BusMergerWireIn input, int destinationMask) {
        input.copyState(pin, null);
        input.source = pin;
        input.triStateOut = pin.isTriState(pin.source);
        input.id = pin.id;
        input.parent = pin.parent;
        input.oldStrong = input.strong;
        sources.add(input);
        if (!pin.hiImpedance) {
            if (pin.strong) {
                if ((strongPins & destinationMask) != 0) {
                    throw new ShortcutException(this, destinationMask, sources);
                }
                strongPins |= destinationMask;
                if (pin.state) {
                    state |= destinationMask;
                }
            } else {
                if ((weakPins & destinationMask) != 0 && ((weakState & destinationMask) == 0) == pin.state) {
                    throw new ShortcutException(this, destinationMask, sources);
                }
                weakPins |= destinationMask;
                weakState |= pin.state ? destinationMask : 0;
                if (pin.state && (strongPins & destinationMask) == 0) {
                    state |= destinationMask;
                }
            }
        }
    }
}
