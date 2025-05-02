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
package pko.KiCadLogicalSchemeSimulator.api.bus;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Bus extends ModelItem<Bus> {
    @Getter
    public final int size;
    public final Map<String, Byte> aliasOffsets;
    public int state;
    public boolean useBitPresentation;

    public Bus(String id, SchemaPart parent, int size, String... aliases) {
        super(id, parent);
        this.size = size;
        if (aliases == null || aliases.length == 0) {
            if (size == 1) {
                throw new RuntimeException("Use Pin for Bus with size 1:" + getName());
            } else {
                aliasOffsets = new HashMap<>();
                for (byte i = 0; i < size; i++) {
                    aliasOffsets.put(id + i, i);
                }
            }
        } else if (aliases.length != size) {
            throw new RuntimeException("Pin definition Error, Names amount not equal size, pin" + getName());
        } else if (size == 1) {
            aliasOffsets = Collections.singletonMap(id, (byte) 0);
        } else {
            aliasOffsets = new HashMap<>();
            for (byte i = 0; i < aliases.length; i++) {
                aliasOffsets.put(aliases[i], i);
            }
        }
    }

    public Bus(Bus oldBus, String variantId) {
        this(oldBus.id, oldBus.parent, oldBus.size);
        this.variantId = variantId + (oldBus.variantId == null ? "" : ":" + oldBus.variantId);
        aliasOffsets.clear();
        aliasOffsets.putAll(oldBus.aliasOffsets);
        useBitPresentation = oldBus.useBitPresentation;
        state = oldBus.state;
        hiImpedance = oldBus.hiImpedance;
        used = oldBus.used;
        priority = oldBus.priority;
        triStateIn=oldBus.triStateIn;
        triStateOut=oldBus.triStateOut;
        source=oldBus;
    }

    @Override
    public Byte getAliasOffset(String pinName) {
        return aliasOffsets.get(pinName);
    }

    @Override
    public Set<String> getAliases() {
        return aliasOffsets.keySet();
    }

    @Override
    public int getState() {
        return (source == null || source == this) ? state : source.getState();
    }

    abstract public void setState(int newState);

    @Override
    public Bus copyState(IModelItem<Bus> oldBus) {
        this.state = oldBus.getThis().state;
        this.hiImpedance = oldBus.isHiImpedance();
        return this;
    }

    @Override
    public Bus getOptimised(ModelItem<?> source) {
        return (Bus) super.getOptimised(source);
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else {
            // noinspection ConstantValue,AssertWithSideEffects
            assert !(hiImpedance = false);
            setHiImpedance();
        }
    }

    @Override
    public boolean isStrong() {
        return true;
    }

    @Override
    public String toString() {
        return state + ":" + super.toString();
    }
}
