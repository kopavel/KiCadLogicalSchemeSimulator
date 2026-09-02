/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
    public final int maxState;
    public int state;
    public boolean useBitPresentation;
    //Fixme why not just 'mask'?
    public int applyMask = Integer.MAX_VALUE;

    protected Bus(String id, SchemaPart parent, int size, String... aliases) {
        super(id, parent);
        this.size = size;
        maxState = Integer.MAX_VALUE & ((1 << size) - 1);
        if (aliases == null || aliases.length == 0) {
            if (size == 1) {
                throw new RuntimeException("Use Pin for Bus with size 1:" + getName());
            } else {
                aliasOffsets = new HashMap<>();
                for (byte b = 0; b < size; b++) {
                    aliasOffsets.put(id + b, b);
                }
            }
        } else if (aliases.length != size) {
            throw new RuntimeException("Pin definition Error, Names amount not equal size, pin" + getName());
        } else if (size == 1) {
            aliasOffsets = Collections.singletonMap(id, (byte) 0);
        } else {
            aliasOffsets = new HashMap<>();
            for (byte b = 0; b < aliases.length; b++) {
                aliasOffsets.put(aliases[b], b);
            }
        }
    }

    protected Bus(Bus oldBus, String variantId) {
        this(oldBus.id, oldBus.parent, oldBus.size);
        this.variantId = variantId + (oldBus.variantId == null ? "" : ":" + oldBus.variantId);
        aliasOffsets.clear();
        aliasOffsets.putAll(oldBus.aliasOffsets);
        useBitPresentation = oldBus.useBitPresentation;
        state = oldBus.state;
        used = oldBus.used;
        priority = oldBus.priority;
        source = (oldBus.source == oldBus) ? this : oldBus.source;
        triStateOut = oldBus.isTriState(oldBus.source);
        hiImpedance = oldBus.hiImpedance && triStateOut;
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
        return (withState || source == null || source == this) ? state : source.getState();
    }

    public void setHi() {
        setState(maxState);
    }

    @Override
    public Bus copyState(IModelItem<? extends Bus> oldBus, ModelItem<?> source) {
        state = oldBus.getState();
        hiImpedance = oldBus.isHiImpedance();
        if (parent == null) {
            parent = oldBus.getParent();
        }
        return this;
    }

    @Override
    public Bus getOptimised(ModelItem<?> source) {
        return (Bus) super.getOptimised(source);
    }

    @Override
    public boolean isStrong() {
        return true;
    }

    @Override
    public String toString() {
        return state + ":" + super.toString();
    }

    @Override
    public void resend() {
        if (hiImpedance) {
            setHiImpedance();
        } else {
            setState(state);
        }
    }
}
