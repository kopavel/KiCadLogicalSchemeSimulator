/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.bus;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public abstract class InBus extends Bus {
    protected InBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
    }

    protected InBus(Bus source, String variantId) {
        super(source, variantId);
    }

    @Override
    public boolean hasTriStateIn() {
        return false;
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        return (InBus) super.getOptimised(source);
    }
}
