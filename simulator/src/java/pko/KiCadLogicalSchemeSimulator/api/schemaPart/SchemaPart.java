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
package pko.KiCadLogicalSchemeSimulator.api.schemaPart;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.TriStateOutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.*;
import pko.KiCadLogicalSchemeSimulator.net.Net;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart.PinType.bidirectional;
import static pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart.PinType.input;
import static pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart.PinType.output;
import static pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart.PinType.passive;

@SuppressWarnings("unused")
public abstract class SchemaPart {
    public final String id;
    public final Map<String, ModelItem<?>> inPins = new HashMap<>();
    public final Map<String, PinType> pinType = new HashMap<>();
    public final Map<IModelItem<?>, String> ids = new HashMap<>();
    public final Map<String, ModelItem<?>> outPins = new HashMap<>();
    public final boolean reverse;
    public final Map<String, String> params = new HashMap<>();
    public final boolean nReverse;
    public Net net;
    public Map<Integer, String> pinNumberMap;

    protected SchemaPart(String id, String sParam) {
        this.id = id;
        if (sParam != null) {
            for (String param : sParam.split(";")) {
                if (!param.isBlank()) {
                    String[] pair = param.split("=");
                    params.put(pair[0], pair.length == 1 ? null : pair[1]);
                }
            }
        }
        reverse = params.containsKey("reverse");
        nReverse = !reverse;
    }

    public InPin addInPin(String pinId) {
        //Fixme - In pin cant be tri-state, it's only for mergers (internal usage)
        return addInPin(new InPin(pinId, this) {
            @Override
            public void setHi() {
                state = true;
            }

            @Override
            public void setLo() {
                state = false;
            }
        });
    }

    public final <T extends InPin> T addInPin(T pin) {
        if (pinType.containsKey(pin.id)) {
            throw new RuntimeException("Double pin name in " + id + ":" + pin.id);
        } else {
            pinType.put(pin.id, input);
        }
        inPins.put(pin.id, pin);
        ids.put(pin, pin.id);
        return pin;
    }

    public void addPassivePin(PassivePin pin) {
        if (pinType.containsKey(pin.id)) {
            throw new RuntimeException("Double pin name in " + id + ":" + pin.id);
        } else {
            pinType.put(pin.id, passive);
        }
        outPins.put(pin.id, pin);
        ids.put(pin, pin.id);
    }

    public void addPullPin(String pinId, boolean state) {
        if (pinType.containsKey(pinId)) {
            throw new RuntimeException("Double pin name in " + id + ":" + pinId);
        }
        pinType.put(pinId, output);
        outPins.put(pinId, new PullPin(pinId, this, state));
        ids.put(outPins.get(pinId), pinId);
    }

    public void addOutPin(String pinId) {
        if (pinType.containsKey(pinId)) {
            if (pinType.get(pinId) == input) {
                pinType.put(pinId, bidirectional);
            } else {
                throw new RuntimeException("Double pin name in " + id + ":" + pinId);
            }
        } else {
            pinType.put(pinId, output);
        }
        outPins.put(pinId, new OutPin(pinId, this));
        ids.put(outPins.get(pinId), pinId);
    }

    public final void addOutPin(String pinId, boolean state) {
        addOutPin(pinId);
        OutPin pin = (OutPin) outPins.get(pinId);
        pin.state = state;
    }

    public void addTriStateOutPin(String pinId) {
        if (pinType.containsKey(pinId)) {
            throw new RuntimeException("Double pin name in " + id + ":" + pinId);
        } else {
            pinType.put(pinId, output);
        }
        outPins.put(pinId, new TriStateOutPin(pinId, this));
        ids.put(outPins.get(pinId), pinId);
    }

    public final void addTriStateOutPin(String pinId, boolean state) {
        addTriStateOutPin(pinId);
        OutPin pin = (OutPin) outPins.get(pinId);
        pin.state = state;
        pin.hiImpedance = false;
    }

    public InBus addInBus(String pinId, int size, String... names) {
        return addInBus(new InBus(pinId, this, size, names) {
            @Override
            public void setState(int newState) {
                state = newState;
            }
        });
    }

    public <T extends InBus> T addInBus(T bus) {
        PinType type;
        if (pinType.containsKey(bus.id)) {
            if (pinType.get(bus.id) == output) {
                type = bidirectional;
            } else {
                throw new RuntimeException("Double pin name in " + id + ":" + bus.id);
            }
        } else {
            type = input;
        }
        pinType.put(bus.id, type);
        inPins.put(bus.id, bus);
        ids.put(bus, bus.id);
        for (String alias : bus.aliasOffsets.keySet()) {
            pinType.put(alias, type);
            inPins.put(alias, bus);
        }
        return bus;
    }

    public void addOutBus(String pinId, int size, String... names) {
        PinType type;
        if (pinType.containsKey(pinId)) {
            if (pinType.get(pinId) == input) {
                type = bidirectional;
            } else {
                throw new RuntimeException("Double pin name in " + id + ":" + pinId);
            }
        } else {
            type = output;
        }
        OutBus pin = new OutBus(pinId, this, size, names);
        pinType.put(pinId, type);
        outPins.put(pinId, pin);
        ids.put(outPins.get(pinId), pinId);
        for (String alias : pin.aliasOffsets.keySet()) {
            pinType.put(alias, type);
            outPins.put(alias, pin);
        }
    }

    public void addTriStateOutBus(String pinId, int size, int state, String... names) {
        addTriStateOutBus(pinId, size, names);
        OutBus pin = (OutBus) outPins.get(pinId);
        pin.state = state;
        pin.hiImpedance = false;
    }

    public void addTriStateOutBus(String pinId, int size, String... names) {
        PinType type;
        if (pinType.containsKey(pinId)) {
            if (pinType.get(pinId) == input) {
                type = bidirectional;
            } else {
                throw new RuntimeException("Double pin name in " + id + ":" + pinId);
            }
        } else {
            type = output;
        }
        pinType.put(pinId, type);
        TriStateOutBus pin = new TriStateOutBus(pinId, this, size, names);
        outPins.put(pinId, pin);
        ids.put(outPins.get(pinId), pinId);
        for (String alias : pin.aliasOffsets.keySet()) {
            pinType.put(alias, type);
            outPins.put(alias, pin);
        }
    }

    public void addOutBus(String pinId, int size, int state, String... names) {
        addOutBus(pinId, size, names);
        OutBus pin = (OutBus) outPins.get(pinId);
        pin.state = state;
        pin.hiImpedance = false;
    }

    public PinType getPinType(String name) {
        return pinType.get(name);
    }

    public IModelItem<?> getInItem(String name) {
        IModelItem<?> item = inPins.get(name);
        if (item == null) {
            throw new RuntimeException("Unknown input item " + name + " in SchemaPart " + id);
        }
        return item;
    }

    public IModelItem<?> getOutItem(String name) {
        IModelItem<?> item = outPins.get(name);
        if (item != null) {
            return item;
        }
        throw new RuntimeException("Unknown output item " + name + " in SchemaPart " + id);
    }

    public Bus getOutBus(String name) {
        IModelItem<?> item = getOutItem(name);
        if (item instanceof Bus bus) {
            return bus;
        }
        throw new RuntimeException("Output item " + name + " in not a bus");
    }

    public Pin getOutPin(String name) {
        IModelItem<?> item = getOutItem(name);
        if (item instanceof Pin pin) {
            return pin;
        }
        throw new RuntimeException("Output item " + name + " in not a pin");
    }

    public String extraState() {
        return null;
    }

    public Supplier<JPanel> extraPanel() {
        return null;
    }

    public abstract void initOuts();

    public <T> void replaceOut(ModelItem<T> outPin) {
        ModelItem<T> newOutPin = outPin.getOptimised(null);
        if (outPin != newOutPin) {
            String oldId = ids.remove(outPin);
            outPins.put(outPin.getId(), newOutPin);
            ids.put(newOutPin, oldId);
            if (outPin instanceof Bus) {
                for (String alias : outPin.getAliases()) {
                    outPins.put(alias, newOutPin);
                }
            }
        }
    }

    public <T> void replaceIn(ModelItem<T> oldIn, ModelItem<T> newIn) {
        String oldId = ids.remove(oldIn);
        inPins.replaceAll((key, value) -> oldIn == value ? newIn : value);
        ids.put(newIn, oldId);
    }

    public void reset() {
    }

    public enum PinType {
        input,
        output,
        passive,
        bidirectional
    }
}
