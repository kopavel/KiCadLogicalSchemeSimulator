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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.TriStateOutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.*;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class SchemaPart {
    public final String id;
    public final Map<String, IModelItem<?>> inPins = new HashMap<>();
    public final Map<String, IModelItem<?>> outPins = new HashMap<>();
    protected final Map<String, String> params = new HashMap<>();
    public final boolean reverse;
    protected final boolean nReverse;
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
        return addInPin(new InPin(pinId, this) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setHi() {
                hiImpedance = false;
                state = true;
            }

            @Override
            public void setLo() {
                hiImpedance = false;
                state = false;
            }
        });
    }

    public InPin addNoFloatingInPin(String pinId) {
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

    public <T extends InPin> T addInPin(T pin) {
        inPins.put(pin.id, pin);
        return pin;
    }

    public void addPassivePin(String pinId) {
        outPins.put(pinId, new PassivePin(pinId, this));
    }

    public void addPullPin(String pinId, boolean state) {
        outPins.put(pinId, new PullPin(pinId, this, state));
    }

    public void addOutPin(String pinId) {
        outPins.put(pinId, new OutPin(pinId, this));
    }

    public void addOutPin(String pinId, boolean state) {
        addOutPin(pinId);
        OutPin pin = (OutPin) outPins.get(pinId);
        pin.state = state;
    }

    public void addTriStateOutPin(String pinId) {
        outPins.put(pinId, new TriStateOutPin(pinId, this));
    }

    public void addTriStateOutPin(String pinId, boolean state) {
        addTriStateOutPin(pinId);
        OutPin pin = (OutPin) outPins.get(pinId);
        pin.state = state;
    }

    public InBus addInBus(String pinId, int size, String... names) {
        return addInBus(new InBus(pinId, this, size, names) {
            @Override
            public void setState(long newState) {
                state = newState;
            }
        });
    }

    public <T extends InBus> T addInBus(T bus) {
        inPins.put(bus.id, bus);
        for (String alias : bus.aliasOffsets.keySet()) {
            inPins.put(alias, bus);
        }
        return bus;
    }

    public void addOutBus(String pinId, int size, String... names) {
        OutBus pin = new OutBus(pinId, this, size, names);
        outPins.put(pinId, pin);
        for (String alias : pin.aliasOffsets.keySet()) {
            outPins.put(alias, pin);
        }
    }

    public void addTriStateOutBus(String pinId, int size, long state, String... names) {
        addTriStateOutBus(pinId, size, names);
        OutBus pin = (OutBus) outPins.get(pinId);
        pin.state = state;
        pin.hiImpedance = false;
    }

    public void addTriStateOutBus(String pinId, int size, String... names) {
        TriStateOutBus pin = new TriStateOutBus(pinId, this, size, names);
        outPins.put(pinId, pin);
        for (String alias : pin.aliasOffsets.keySet()) {
            outPins.put(alias, pin);
        }
    }

    public void addOutBus(String pinId, int size, long state, String... names) {
        addOutBus(pinId, size, names);
        OutBus pin = (OutBus) outPins.get(pinId);
        pin.state = state;
        pin.hiImpedance = false;
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

    public void onPassivePinChange(Pin merger) {
    }

    public abstract void initOuts();

    public <T> void replaceOut(IModelItem<T> outPin) {
        IModelItem<T> newOutPin = outPin.getOptimised(null);
        if (outPin != newOutPin) {
            newOutPin.copyState(outPin);
            outPins.put(outPin.getId(), newOutPin);
            if (outPin instanceof Bus) {
                for (String alias : outPin.getAliases()) {
                    outPins.put(alias, newOutPin);
                }
            } else if (!(outPin instanceof Pin)) {
                throw new RuntimeException("Unsupported item: " + outPin.getClass().getName());
            }
        }
    }

    public void reset() {
    }
}
