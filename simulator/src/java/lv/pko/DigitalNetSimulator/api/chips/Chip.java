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
package lv.pko.DigitalNetSimulator.api.chips;
import lv.pko.DigitalNetSimulator.api.pins.PassivePin;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;
import lv.pko.DigitalNetSimulator.api.pins.out.PullPin;
import lv.pko.DigitalNetSimulator.api.pins.out.TriStateOutPin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class Chip {
    public final String id;
    public final Map<String, OutPin> outMap = new LinkedHashMap<>();
    public final Map<String, InPin> inMap = new LinkedHashMap<>();
    public final Map<String, PassivePin> passiveMap = new HashMap<>();
    protected final Map<String, String> params = new HashMap<>();
    protected final long hiState;
    protected final long loState;
    protected final boolean reverse;
    private final Map<String, InPin> inAliasMap = new HashMap<>();
    private final Map<String, OutPin> outAliasMap = new HashMap<>();

    protected Chip(String id, String sParam) {
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
            loState = Long.MAX_VALUE;
            reverse = true;
        } else {
            hiState = Long.MAX_VALUE;
            loState = 0;
            reverse = false;
        }
    }

    public InPin addInPin(String pinId) {
        return addInPin(pinId, 1);
    }

    public InPin addInPin(String pinId, int size, String... names) {
        InPin pin = new InPin(pinId, this, size, names) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        inMap.put(pinId, pin);
        for (String alias : pin.aliases.keySet()) {
            inAliasMap.put(alias, pin);
        }
        return pin;
    }

    public <T extends InPin> T addInPin(T pin) {
        inMap.put(pin.id, pin);
        for (String alias : pin.aliases.keySet()) {
            inAliasMap.put(alias, pin);
        }
        return pin;
    }

    public <T extends PassivePin> T addPassivePin(T pin) {
        passiveMap.put(pin.id, pin);
        return pin;
    }

    public void addOutPin(String pinId, int size, String... names) {
        OutPin pin = new OutPin(pinId, this, size, names);
        for (String alias : pin.aliases.keySet()) {
            outAliasMap.put(alias, pin);
        }
        outMap.put(pinId, pin);
    }

    public void addOutPin(String pinId, int size, long state, String... names) {
        OutPin pin = new OutPin(pinId, this, size, names);
        pin.state = state;
        for (String alias : pin.aliases.keySet()) {
            outAliasMap.put(alias, pin);
        }
        outMap.put(pinId, pin);
    }

    public void addTriStateOutPin(String pinId, int size, String... names) {
        OutPin pin = new TriStateOutPin(pinId, this, size, names);
        for (String alias : pin.aliases.keySet()) {
            outAliasMap.put(alias, pin);
        }
        outMap.put(pinId, pin);
    }

    public void addTriStateOutPin(String pinId, int size, long state, String... names) {
        OutPin pin = new TriStateOutPin(pinId, this, size, names);
        pin.state = state;
        for (String alias : pin.aliases.keySet()) {
            outAliasMap.put(alias, pin);
        }
        outMap.put(pinId, pin);
    }

    public void addPullPin(String pinId, long state) {
        OutPin pin = new PullPin(pinId, this, state);
        for (String alias : pin.aliases.keySet()) {
            outAliasMap.put(alias, pin);
        }
        outMap.put(pinId, pin);
    }

    public InPin getInPin(String name) {
        InPin pin = inMap.get(name);
        if (pin == null) {
            pin = inAliasMap.get(name);
        }
        if (pin == null) {
            throw new RuntimeException("Unknown input pin " + name + " in SchemaPart " + id);
        }
        return pin;
    }

    public OutPin getOutPin(String name) {
        OutPin pin = outMap.get(name);
        if (pin == null) {
            pin = outAliasMap.get(name);
        }
        if (pin == null) {
            throw new RuntimeException("Unknown output pin " + name + " in SchemaPart " + id);
        }
        return pin;
    }

    public PassivePin getPassivePin(String name) {
        PassivePin pin = passiveMap.get(name);
        if (pin == null) {
            throw new RuntimeException("Unknown passive pin " + name + " in SchemaPart " + id);
        }
        return pin;
    }

    public String extraState() {
        return null;
    }

    public abstract void initOuts();

    public void replaceOut(OutPin outPin, OutPin newOutPin) {
        for (String alias : outPin.aliases.keySet()) {
            outAliasMap.put(alias, newOutPin);
        }
        outMap.put(outPin.id, newOutPin);
    }
}