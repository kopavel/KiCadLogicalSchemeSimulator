/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class TriStateOutPin extends OutPin {
    public TriStateOutPin(String id, SchemaPart parent) {
        super(id, parent);
        hiImpedance = true;
        triStateOut = true;
    }
}
