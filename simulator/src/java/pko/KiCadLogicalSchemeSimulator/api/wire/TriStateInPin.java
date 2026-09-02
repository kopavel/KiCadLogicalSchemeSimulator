/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public abstract class TriStateInPin extends InPin {
    protected TriStateInPin(String id, SchemaPart parent) {
        super(id, parent);
    }

    protected TriStateInPin(Pin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    protected TriStateInPin(String id, SchemaPart parent, boolean state) {
        super(id, parent);
        this.state = state;
    }

    @Override
    public boolean hasTriStateIn() {
        return true;
    }

    @Override
    abstract public void setHiImpedance();
}
