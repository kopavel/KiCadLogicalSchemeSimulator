/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.tester;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class TestInBus extends SchemaPart {
    public final InBus bus;

    protected TestInBus(String id, String sParam) {
        super(id, sParam);
        int size;
        if (params.containsKey("size")) {
            size = Integer.parseInt(params.get("size"));
        } else {
            size = 8;
        }
        InBus inBus = new InBus("In", this, size) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
            }

            @Override
            public void setState(int newState) {
                hiImpedance = false;
                state = newState;
            }
        };
        inBus.hiImpedance = true;
        bus = addInBus(inBus);
    }

    @Override
    public void initOuts() {
        if (bus.isTriState(bus.source)) {
            bus.hiImpedance = true;
        }
    }
}
