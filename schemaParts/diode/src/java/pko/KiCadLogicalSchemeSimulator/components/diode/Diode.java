/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.diode;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;

public class Diode extends SchemaPart {
    protected PassivePin anode;
    protected PassivePin cathode;

    //FixMe - dual diode doesn't work!!!
    protected Diode(String id, String sParams) {
        super(id, sParams);
        anode = addPassivePin(new PassivePin("A", this) {
            @Override
            public void onChange() {
                if (otherImpedance || !otherState || (cathode.strengthSensitive && !otherStrong && cathode.strong)) {
                    if (!cathode.hiImpedance) {
                        cathode.setHiImpedance();
                    }
                } else if (cathode.hiImpedance || !cathode.state || cathode.strong != otherStrong) {
                    cathode.setHi(otherStrong);
                }
                if (cathode.otherImpedance || cathode.otherState || (strengthSensitive && !cathode.otherStrong && strong)) {
                    if (!hiImpedance) {
                        setHiImpedance();
                    }
                } else if (hiImpedance || state || strong != cathode.otherStrong) {
                    setLo(cathode.otherStrong);
                }
            }
        });
        cathode = addPassivePin(new PassivePin("K", this) {
            @Override
            public void onChange() {
                if (otherImpedance || otherState || (anode.strengthSensitive && !otherStrong && anode.strong)) {
                    if (!anode.hiImpedance) {
                        anode.setHiImpedance();
                    }
                } else if (anode.hiImpedance || anode.state || anode.strong != otherStrong) {
                    anode.setLo(otherStrong);
                }
                if (anode.otherImpedance || !anode.otherState || (strengthSensitive && !anode.otherStrong && strong)) {
                    if (!hiImpedance) {
                        setHiImpedance();
                    }
                } else if (hiImpedance || !state || strong != anode.otherStrong) {
                    setHi(anode.otherStrong);
                }
            }
        });
    }

    @Override
    public void initOuts() {
    }
}
