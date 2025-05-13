
/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package pko.KiCadLogicalSchemeSimulator.components.resister;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class Resister extends SchemaPart {
    private Pin in1;
    private Pin in2;

    protected Resister(String id, String sParams) {
        super(id, sParams);
        addPassivePin(new PassivePin("IN1", this) {
            @Override
            public void onChange() {
                PassivePin pin1 = (PassivePin) in1;
                if (in2 instanceof PassivePin pin2) {
                    if (pin1.otherImpedance || (!pin2.otherImpedance && pin1.otherState == pin2.otherState && pin2.otherStrong == pin1.otherStrong)) {
                        if (!in2.hiImpedance) {
                            Log.debug(Resister.class, "IN2 -> imp");
                            in2.setHiImpedance();
                        }
                    } else if (pin1.otherState) {
                        if (!in2.state || in2.hiImpedance) {
                            Log.debug(Resister.class, "IN2 -> hi");
                            in2.setHi();
                        }
                    } else if (in2.state || in2.hiImpedance) {
                        Log.debug(Resister.class, "IN2 -> lo");
                        in2.setLo();
                    }
                } else {
                    if (pin1.otherImpedance) {
                        if (!in2.hiImpedance) {
                            Log.debug(Resister.class, "IN2 -> imp");
                            in2.setHiImpedance();
                        }
                    } else if (!in2.hiImpedance && pin1.otherState == in2.state) {
                        in2.setHiImpedance();
                    } else if (pin1.otherState) {
                        Log.debug(Resister.class, "IN2 -> hi");
                        in2.setHi();
                    } else {
                        Log.debug(Resister.class, "IN2 -> lo");
                        in2.setLo();
                    }
                }
            }
        });
        addPassivePin(new PassivePin("IN2", this) {
            @Override
            public void onChange() {
                PassivePin pin2 = (PassivePin) in2;
                if (in1 instanceof PassivePin pin1) {
                    if (pin2.otherImpedance || (!pin1.otherImpedance && pin2.otherState == pin1.otherState && pin1.otherStrong == pin2.otherStrong)) {
                        if (!in1.hiImpedance) {
                            Log.debug(Resister.class, "IN1 -> imp");
                            in1.setHiImpedance();
                        }
                    } else if (pin2.otherState) {
                        if (!in1.state || in1.hiImpedance) {
                            Log.debug(Resister.class, "IN1 -> hi");
                            in1.setHi();
                        }
                    } else if (in1.state || in1.hiImpedance) {
                        Log.debug(Resister.class, "IN1 -> lo");
                        in1.setLo();
                    }
                } else {
                    if (pin2.otherImpedance) {
                        if (!in1.hiImpedance) {
                            Log.debug(Resister.class, "IN1 -> imp");
                            in1.setHiImpedance();
                        }
                    } else if (!in1.hiImpedance && pin2.otherState == in1.state) {
                        Log.debug(Resister.class, "IN1 -> imp");
                        in1.setHiImpedance();
                    } else if (pin2.otherState) {
                        Log.debug(Resister.class, "IN1 -> hi");
                        in1.setHi();
                    } else {
                        Log.debug(Resister.class, "IN1 -> lo");
                        in1.setLo();
                    }
                }
            }
        });
    }

    @Override
    public void initOuts() {
        in1 = getOutPin("IN1");
        in2 = getOutPin("IN2");
    }
}
