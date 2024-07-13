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
package lv.pko.KiCadLogicalSchemeSimulator.v2.api.schemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.IModelItem;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.ModelInItem;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.Bus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.OutBus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.in.InBus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.in.NoFloatingInBus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.in.InPin;

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
        return inPin;
    }

    public InPin addInPin(String pinId) {
        return addInPin(new InPin(pinId, this) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setState(boolean newState, boolean strong) {
                state = newState;
            }
        });
    }

    public <T extends InPin> T addInPin(T pin) {
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
    }

    public InBus addInBus(String pinId, int size, String... names) {
        return addInBus(new InBus(pinId, this, size, names) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setState(long newState) {
                state = newState;
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

    public Bus getInBus(String name) {
        ModelInItem item = getInItem(name);
        if (item instanceof Bus bus) {
            return bus;
        }
        throw new RuntimeException("Input item " + name + " in not a bus");
    }

    public Pin getInPin(String name) {
        ModelInItem item = getInItem(name);
        if (item instanceof Pin pin) {
            return pin;
        }
        throw new RuntimeException("Input item " + name + " in not a pin");
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
            default -> throw new RuntimeException("usuppoerted ModelOutItem: " + outPin.getClass().getName());
        }
        outMap.put(outPin.getId(), newOutPin);
    }

    public void reset() {
    }
}
