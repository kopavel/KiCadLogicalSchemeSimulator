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
package pko.KiCadLogicalSchemeSimulator.net.merger.bus;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PullPin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.bus.BusInInterconnect;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//FixMe use one destination with splitter
//ToDo implement pure bus implementation (no weak state)
public class BusMerger extends OutBus {
    public MergerInput<?>[] sources = new MergerInput[0];
    public long strongPins;
    public long weakPins;
    public long weakState;

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

    //fixme what about optimisation?
    public void addDestination(Bus item) {
        switch (item) {
            case BusInInterconnect interconnect -> {
                this.mask &= interconnect.inverseInterconnectMask;
                this.mask |= interconnect.senseMask;
                destinations = Utils.addToArray(destinations, interconnect);
            }
            case InBus bus -> destinations = Utils.addToArray(destinations, bus);
            default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
        }
        id += "/" + item.getName();
    }

    public void addSource(OutBus bus, long srcMask, byte offset) {
        long destinationMask = offset == 0 ? srcMask : (offset > 0 ? srcMask << offset : srcMask >> -offset);
        BusMergerBusIn input = new BusMergerBusIn(bus, destinationMask, this);
        bus.addDestination(input, srcMask, offset);
        sources = Utils.addToArray(sources, input);
        if (!bus.hiImpedance) {
            if ((strongPins & destinationMask) != 0) {
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    throw new ShortcutException(sources);
                }
            }
            state |= bus.state;
            strongPins |= destinationMask;
        }
        Arrays.sort(sources, Comparator.comparing(MergerInput::getName));
        hiImpedance = (strongPins | weakPins) != mask;
    }

    public void addSource(OutPin pin, byte offset) {
        long destinationMask = 1L << offset;
        if (pin instanceof PullPin pullPin) {
            weakPins |= destinationMask;
            if (pin.state) {
                weakState |= destinationMask;
            }
            sources = Utils.addToArray(sources, pullPin);
            Arrays.sort(sources, Comparator.comparing(MergerInput::getName));
            hiImpedance = (strongPins | weakPins) != mask;
        } else {
            BusMergerWireIn input = new BusMergerWireIn(destinationMask, this);
            input.id = pin.id;
            input.parent = pin.parent;
            pin.addDestination(input);
            processPin(pin, input, destinationMask);
        }
    }

    public void addSource(Net net, List<OutPin> pins, List<PassivePin> passivePins, Byte offset) {
        long destinationMask = 1L << offset;
        BusMergerWireIn input = new BusMergerWireIn(destinationMask, this);
        Pin pin = net.processWire(input, pins, passivePins, Collections.emptyMap());
        processPin(pin, input, destinationMask);
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

    private void processPin(Pin pin, BusMergerWireIn input, long destinationMask) {
        input.oldStrong = pin.strong;
        input.oldImpedance = pin.hiImpedance;
        input.id = pin.id;
        input.parent = pin.parent;
        input.copyState(pin);
        sources = Utils.addToArray(sources, input);
        if (!pin.hiImpedance) {
            if (pin.strong) {
                if ((strongPins & destinationMask) != 0) {
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(sources);
                    }
                }
                strongPins |= destinationMask;
                if (pin.state) {
                    state |= destinationMask;
                }
            } else {
                if ((weakPins & destinationMask) != 0 && ((weakState & destinationMask) == 0) == pin.state) {
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(sources);
                    }
                }
                weakPins |= destinationMask;
                weakState |= pin.state ? destinationMask : 0;
                if (pin.state && (strongPins & destinationMask) == 0) {
                    state |= destinationMask;
                }
            }
        }
        Arrays.sort(sources, Comparator.comparing(MergerInput::getName));
        hiImpedance = (strongPins | weakPins) != mask;
    }
}
