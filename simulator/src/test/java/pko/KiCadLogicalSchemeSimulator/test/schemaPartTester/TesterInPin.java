/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.schemaPartTester;
import pko.KiCadLogicalSchemeSimulator.api.wire.TriStateInPin;

public class TesterInPin extends TriStateInPin {
    public TesterInPin(String id) {
        super(id, null);
    }

    @Override
    public void setHiImpedance() {
        hiImpedance = true;
    }

    @Override
    public void setHi() {
        hiImpedance = false;
        state = true;
    }

    @Override
    public void setLo() {
        hiImpedance = false;
        state = false;
    }

    @Override
    public String toString() {
        return "TesterInPin{" + "id='" + id + '\'' + '}';
    }
}
