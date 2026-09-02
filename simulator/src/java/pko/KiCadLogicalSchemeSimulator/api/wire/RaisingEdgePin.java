/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public abstract class RaisingEdgePin extends InPin {
    protected RaisingEdgePin(String id, SchemaPart parent) {
        super(id, parent);
    }

    protected RaisingEdgePin(Pin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    @Override
    public void setLo() {
        state = false;
    }
}
