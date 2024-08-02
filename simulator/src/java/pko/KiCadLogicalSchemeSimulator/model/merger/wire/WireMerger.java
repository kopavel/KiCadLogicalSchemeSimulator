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
package pko.KiCadLogicalSchemeSimulator.model.merger.wire;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.model.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.List;
import java.util.Map;

public class WireMerger extends OutPin {
    public MergerInput<?>[] sources = new MergerInput[0];
    public byte weakState;

    public WireMerger(Pin destination, List<OutPin> pins, Map<OutBus, Long> buses) {
        super(destination, "wireMerger");
        destinations = new Pin[]{destination};
        strong = false;
        pins.forEach(this::addSource);
        if (buses != null) {
            buses.forEach(this::addSource);
        }
    }

    //FixMe what about destination optimisation?
    @Override
    public void addDestination(Pin pin) {
        if (destinations.length == 1 && destinations[0] instanceof PassivePin passivePin) {
            passivePin.addDestination(pin);
        } else {
            destinations = Utils.addToArray(destinations, pin);
        }
        if (pin.getId().contains("->")) {
            id += "/" + pin.getId().substring(pin.getId().indexOf("->") + 2);
        } else {
            id += "/" + pin.getName();
        }
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state, strong);
        }
    }

    @Override
    public Pin getOptimised() {
        throw new UnsupportedOperationException();
    }

    private void addSource(OutBus bus, long mask) {
        WireMergerBusIn input = new WireMergerBusIn(bus, mask, this);
        bus.addDestination(input, mask, (byte) 0);
        sources = Utils.addToArray(sources, input);
        if (!bus.hiImpedance) {
            if (strong) {
                throw new ShortcutException(sources);
            }
            strong = true;
            state = (bus.state & mask) != 0;
        }
    }

    private void addSource(OutPin pin) {
        WireMergerWireIn input = new WireMergerWireIn(pin, this);
        pin.addDestination(input);
        sources = Utils.addToArray(sources, input);
        if (!pin.hiImpedance) {
            hiImpedance = false;
            if (pin.strong) {
                if (strong) {
                    throw new ShortcutException(sources);
                }
                strong = true;
                state = pin.state;
            } else {
                if (weakState != 0 && (weakState > 0 != pin.state)) {
                    throw new ShortcutException(sources);
                }
                weakState += (byte) (pin.state ? 1 : -1);
                if (!strong) {
                    state = pin.state;
                }
            }
        }
    }
}
