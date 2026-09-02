/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public abstract class FallingEdgePin extends InPin {
    protected FallingEdgePin(String id, SchemaPart parent) {
        super(id, parent);
    }

    protected FallingEdgePin(Pin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    @Override
    public void setHi() {
        state = true;
    }
}
