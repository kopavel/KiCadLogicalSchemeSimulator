/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.wire;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.TriStateInPin;

public class PassiveIn extends TriStateInPin {
    public final PassivePin destination;

    public PassiveIn(PassivePin destination) {
        super(destination, null);
        id += "_in";
        this.destination = destination;
    }

    @Override
    public void setHi() {
        state = true;
        hiImpedance = false;
        destination.otherState = true;
        destination.otherImpedance = false;
        if (source != null) {
            destination.otherStrong = source.isStrong();
        } else {
            destination.otherStrong = strong;
        }
        destination.onChange();
    }

    @Override
    public void setLo() {
        state = false;
        hiImpedance = false;
        destination.otherState = false;
        destination.otherImpedance = false;
        if (source != null) {
            destination.otherStrong = source.isStrong();
        } else {
            destination.otherStrong = strong;
        }
        destination.onChange();
    }

    @Override
    public void setHiImpedance() {
        hiImpedance = true;
        destination.otherImpedance = true;
        destination.onChange();
    }

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        this.source = source;
        destination.getOptimised(this);
        return this;
    }
}
