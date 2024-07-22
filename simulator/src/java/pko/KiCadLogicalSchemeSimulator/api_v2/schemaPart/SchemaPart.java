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
package pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.ModelInItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.NoFloatingInBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class SchemaPart {
    public final String id;
    public final Map<String, IModelItem> outMap = new LinkedHashMap<>();
    public final Map<String, ModelInItem> inMap = new LinkedHashMap<>();
    protected final Map<String, String> params = new HashMap<>();
    protected final boolean reverse;
    private final Map<String, ModelInItem> inAliasMap = new HashMap<>();
    private final Map<String, IModelItem> outAliasMap = new HashMap<>();
    public Map<Integer, String> pinNumberMap;
    protected long hiState;
    protected long loState;

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
        if (params.containsKey("reverse")) {
            hiState = 0;
            loState = -1;
            reverse = true;
        } else {
            hiState = -1;
            loState = 0;
            reverse = false;
        }
    }

    public InPin addInPin(String pinId, boolean value, boolean strong) {
        InPin inPin = addInPin(pinId);
        inPin.state = value;
        inPin.strong = strong;
        inPin.hiImpedance = false;
        return inPin;
    }

    public InPin addInPin(String pinId) {
        return addInPin(new InPin(pinId, this) {
            @Override
            public void setHiImpedance() {
                assert !hiImpedance : "Already in hiImpedance:" + this;
                hiImpedance = true;
            }

            @Override
            public void setState(boolean newState, boolean strong) {
                hiImpedance = false;
                state = newState;
            }
        });
    }

    public <T extends InPin> T addInPin(T pin) {
        inMap.put(pin.id, pin);
        inAliasMap.put(pin.id, pin);
        return pin;
    }

    public PassivePin addPassivePin(PassivePin pin) {
        outMap.put(pin.id, pin);
        outAliasMap.put(pin.id, pin);
        inMap.put(pin.id, pin);
        inAliasMap.put(pin.id, pin);
        return pin;
    }

    public void addOutPin(String pinId) {
        OutPin pin = new OutPin(pinId, this);
        outAliasMap.put(pinId, pin);
        outMap.put(pinId, pin);
    }

    public void addOutPin(String pinId, boolean state, boolean strong) {
        addOutPin(pinId);
        OutPin pin = (OutPin) outMap.get(pinId);
        pin.state = state;
        pin.strong = strong;
        pin.hiImpedance = false;
    }

    public InBus addInBus(String pinId, int size, String... names) {
        return addInBus(new InBus(pinId, this, size, names) {
            @Override
            public void setHiImpedance() {
                assert !hiImpedance : "Already in hiImpedance:" + this;
                hiImpedance = true;
            }

            @Override
            public void setState(long newState) {
                state = newState;
                hiImpedance = false;
            }
        });
    }

    public InBus addNoFloatInBus(String pinId, int size, String... names) {
        return addInBus(new NoFloatingInBus(pinId, this, size, names) {
            @Override
            public void setState(long newState) {
                state = newState;
            }
        });
    }

    public <T extends InBus> T addInBus(T bus) {
        inMap.put(bus.id, bus);
        for (String alias : bus.aliasOffsets.keySet()) {
            inAliasMap.put(alias, bus);
        }
        return bus;
    }

    public void addOutBus(String pinId, int size, String... names) {
        OutBus pin = new OutBus(pinId, this, size, names);
        for (String alias : pin.aliasOffsets.keySet()) {
            outAliasMap.put(alias, pin);
        }
        outMap.put(pinId, pin);
    }

    public void addOutBus(String pinId, int size, long state, String... names) {
        addOutBus(pinId, size, names);
        OutBus pin = (OutBus) outMap.get(pinId);
        pin.state = state;
        pin.hiImpedance = false;
    }

    public ModelInItem getInItem(String name) {
        ModelInItem item = inMap.get(name);
        if (item == null) {
            item = inAliasMap.get(name);
        }
        if (item == null) {
            throw new RuntimeException("Unknown input item " + name + " in SchemaPart " + id);
        }
        return item;
    }

    public IModelItem getOutItem(String name) {
        IModelItem pin = outMap.get(name);
        if (pin == null) {
            pin = outAliasMap.get(name);
        }
        if (pin == null) {
            throw new RuntimeException("Unknown output item " + name + " in SchemaPart " + id);
        }
        return pin;
    }

    public Bus getOutBus(String name) {
        IModelItem item = getOutItem(name);
        if (item instanceof Bus bus) {
            return bus;
        }
        throw new RuntimeException("Output item " + name + " in not a bus");
    }

    public Pin getOutPin(String name) {
        IModelItem item = getOutItem(name);
        if (item instanceof Pin pin) {
            return pin;
        }
        throw new RuntimeException("Output item " + name + " in not a pin");
    }

    public String extraState() {
        return null;
    }

    public abstract void initOuts();

    public <T extends IModelItem> void replaceOut(T outPin, T newOutPin) {
        switch (outPin) {
            case OutPin pin -> outAliasMap.put(outPin.getId(), newOutPin);
            case OutBus bus -> {
                for (String alias : outPin.getAliases()) {
                    outAliasMap.put(alias, newOutPin);
                }
            }
            default -> throw new RuntimeException("Unsupported ModelOutItem: " + outPin.getClass().getName());
        }
        outMap.put(outPin.getId(), newOutPin);
    }

    public void reset() {
    }
}
