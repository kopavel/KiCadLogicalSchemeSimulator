
/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.resister;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;

public class Resister extends SchemaPart {
    private final PassivePin in1;
    private final PassivePin in2;

    protected Resister(String id, String sParams) {
        super(id, sParams);
        in1 = addPassivePin(new PassivePin("IN1", this) {
            @Override
            public void onChange() {
                if (in2 instanceof PassivePin pin2) {
                    if (otherImpedance || (!pin2.otherImpedance && otherState == pin2.otherState && pin2.otherStrong == otherStrong)) {
                        if (!in2.hiImpedance) {
                            in2.setHiImpedance();
                        }
                    } else if (otherState) {
                        if (!in2.state || in2.hiImpedance) {
                            in2.setHi();
                        }
                    } else if (in2.state || in2.hiImpedance) {
                        in2.setLo();
                    }
                } else {
                    if (otherImpedance) {
                        if (!in2.hiImpedance) {
                            in2.setHiImpedance();
                        }
                    } else if (!in2.hiImpedance && otherState == in2.state) {
                        in2.setHiImpedance();
                    } else if (otherState) {
                        in2.setHi();
                    } else {
                        in2.setLo();
                    }
                }
            }
        });
        in2 = addPassivePin(new PassivePin("IN2", this) {
            @Override
            public void onChange() {
                if (in1 instanceof PassivePin pin1) {
                    if (otherImpedance || (!pin1.otherImpedance && otherState == pin1.otherState && pin1.otherStrong == otherStrong)) {
                        if (!in1.hiImpedance) {
                            in1.setHiImpedance();
                        }
                    } else if (otherState) {
                        if (!in1.state || in1.hiImpedance) {
                            in1.setHi();
                        }
                    } else if (in1.state || in1.hiImpedance) {
                        in1.setLo();
                    }
                } else {
                    if (otherImpedance) {
                        if (!in1.hiImpedance) {
                            in1.setHiImpedance();
                        }
                    } else if (!in1.hiImpedance && otherState == in1.state) {
                        in1.setHiImpedance();
                    } else if (otherState) {
                        in1.setHi();
                    } else {
                        in1.setLo();
                    }
                }
            }
        });
    }

    @Override
    public void initOuts() {
    }
}
