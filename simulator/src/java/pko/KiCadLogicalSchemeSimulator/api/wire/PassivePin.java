/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

//FixMe add target and use it, uze optimiser on top of it.
public abstract class PassivePin extends TriStateOutPin {
    public boolean otherImpedance = true;
    public boolean otherState;
    public boolean otherStrong = true;

    protected PassivePin(String id, SchemaPart parent) {
        super(id, parent);
        strong = false;
        strengthSensitive = true;
    }

    public abstract void onChange();

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(this);
        }
        split();
        return this;
    }

    public void recalculateOtherState(boolean mergerImpedance, boolean mergerState, int mergerWeakState, boolean mergerStrong) {
        if (mergerImpedance) {
            otherImpedance = true;
        } else if (hiImpedance) {
            //we are in impedance - clone merger
            otherState = mergerState;
            otherStrong = mergerStrong;
            otherImpedance = false;
        } else if (strong) {
            //we are strong
            if (mergerWeakState == 0) {
                //no other weak
                otherImpedance = true;
            } else {
                //other weak
                otherImpedance = false;
                otherStrong = false;
                otherState = mergerWeakState > 0;
            }
            //we are weak
        } else if (mergerStrong) {
            //has a strong pin, clone merger state.
            otherState = mergerState;
            otherStrong = true;
            otherImpedance = false;
        } else if (mergerWeakState == 1 || mergerWeakState == -1) {
            //we only weak on merger - hiImpedance
            otherImpedance = true;
        } else {
            //merger has many weak - state is the same as we are.
            otherImpedance = false;
            otherStrong = false;
            otherState = mergerState;
        }
        onChange();
    }
}
