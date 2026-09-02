/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.wire;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class TriStateNCWire extends OutPin {
    public TriStateNCWire(OutPin outPin) {
        super(outPin, "NC");
    }

    @Override
    public void addDestination(Pin pin) {
        throw new UnsupportedOperationException("Can't add destination to NC Out Pin");
    }

    @Override
    public void setHi() {
        state = true;
        hiImpedance = false;
    }

    @Override
    public void setLo() {
        state = false;
        hiImpedance = false;
    }

    @Override
    public void setHiImpedance() {
        hiImpedance = true;
    }

}
