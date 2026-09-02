/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.schemaPartTester;
import pko.KiCadLogicalSchemeSimulator.net.bus.TriStateInBus;

public class TesterInBus extends TriStateInBus {
    public TesterInBus(String id, int size) {
        super(id, null, size);
    }

    @Override
    public void setHiImpedance() {
        hiImpedance = true;
    }

    @Override
    public void setState(int state) {
        hiImpedance = false;
        this.state = state;
    }

    @Override
    public String toString() {
        return "TesterInBus{" + "id='" + id + '\'' + '}';
    }
}
