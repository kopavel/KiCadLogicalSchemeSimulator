/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.bus;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;

public class NCBus extends OutBus {
    public NCBus(OutBus outBus) {
        super(outBus, "NC");
    }

    @Override
    public void setState(int newState) {
    }

    @Override
    public void setHiImpedance() {
    }

    @Override
    public Bus getOptimised(ModelItem<?> inSource) {
        return this;
    }
}
