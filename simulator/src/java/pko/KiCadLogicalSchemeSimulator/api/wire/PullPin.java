/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;

import java.util.Set;

public class PullPin extends OutPin implements MergerInput<Pin> {
    public PullPin(String id, SchemaPart parent, boolean state) {
        super(id, parent);
        strong = false;
        hiImpedance = false;
        this.state = state;
    }

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(this);
        }
        split();
        this.source = source;
        return this;
    }

    @Override
    public int getMask() {
        return 0;
    }

    @Override
    public Set<MergerInput<?>> getSources() {
        return Set.of();
    }

    @Override
    public void retry() {
    }
}
