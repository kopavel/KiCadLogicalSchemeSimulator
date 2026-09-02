/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.tester;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class TestOutBus extends SchemaPart {
    public Bus bus;

    protected TestOutBus(String id, String sParam) {
        super(id, sParam);
        int size;
        if (params.containsKey("size")) {
            size = Integer.parseInt(params.get("size"));
        } else {
            size = 8;
        }
        addTriStateOutBus("Out", size);
    }

    @Override
    public void initOuts() {
        bus = getOutBus("Out");
    }
}
