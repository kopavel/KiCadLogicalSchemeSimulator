/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import java.util.Set;

public abstract class Pin extends ModelItem<Pin> {
    public boolean state;
    @Getter
    public boolean strong = true;
    public boolean strengthSensitive;

    protected Pin(Pin oldPin, String variantId) {
        this(oldPin.id, oldPin.parent);
        this.variantId = variantId + (oldPin.variantId == null ? "" : ":" + oldPin.variantId);
        state = oldPin.state;
        strong = oldPin.strong;
        used = oldPin.used;
        priority = oldPin.priority;
        triStateOut = oldPin.isTriState(oldPin.source);
        hiImpedance = oldPin.hiImpedance && isTriState(source);
        source = (oldPin.source == oldPin) ? this : oldPin.source;
        withState = oldPin.withState;
        strengthSensitive = oldPin.strengthSensitive;
    }

    protected Pin(String id, SchemaPart parent) {
        super(id, parent);
    }

    @Override
    public Pin copyState(IModelItem<? extends Pin> oldPin, ModelItem<?> source) {
        used = true;
        strong = oldPin.isStrong();
        hiImpedance = oldPin.isTriState(source) && oldPin.hasTriStateIn() && oldPin.isHiImpedance();
        state = oldPin.getState() != 0;
        priority += oldPin.getPriority();
        strengthSensitive = oldPin.getThis().strengthSensitive;
        if (parent == null) {
            parent = oldPin.getParent();
        }
        return this;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public int getState() {
        return (withState || source == null || source == this) ? (state ? 1 : 0) : source.getState();
    }

    public void setState(int state) {
        this.state = state == 1;
        resend();
    }

    public void setHi(boolean strong) {
        this.strong = strong;
        setHi();
    }

    public void setLo(boolean strong) {
        this.strong = strong;
        setLo();
    }

    abstract public void setHi();
    abstract public void setLo();

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        return (Pin) super.getOptimised(source);
    }

    @Override
    public Byte getAliasOffset(String pinName) {
        return 0;
    }

    @Override
    public Set<String> getAliases() {
        return Set.of(id);
    }

    @Override
    public String toString() {
        return state + ":" + strong + ":" + super.toString();
    }

    public void resend() {
        if (hiImpedance) {
            setHiImpedance();
        } else if (state) {
            setHi();
        } else {
            setLo();
        }
    }
}
