/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class TriStateOutBus extends OutBus {
    public TriStateOutBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        hiImpedance = true;
        triStateOut = true;
    }
}
