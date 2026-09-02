/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.bus;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public abstract class TriStateInBus extends Bus {
    protected TriStateInBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        hiImpedance = true;
    }

    protected TriStateInBus(Bus source, String variantId) {
        super(source, variantId);
    }

    @Override
    public boolean hasTriStateIn() {
        return true;
    }

    @Override
    public TriStateInBus getOptimised(ModelItem<?> source) {
        return (TriStateInBus) super.getOptimised(source);
    }

    @Override
    abstract public void setHiImpedance();
}
