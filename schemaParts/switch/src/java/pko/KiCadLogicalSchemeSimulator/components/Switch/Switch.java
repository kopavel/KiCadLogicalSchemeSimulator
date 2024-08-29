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
package pko.KiCadLogicalSchemeSimulator.components.Switch;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import static pko.KiCadLogicalSchemeSimulator.components.Switch.Switch.MergerState.hiImpedance;
import static pko.KiCadLogicalSchemeSimulator.components.Switch.Switch.MergerState.strong;

public class Switch extends SchemaPart implements InteractiveSchemaPart {
    public boolean toggled;
    private Pin pin1;
    private Pin pin2;
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        addPassivePin("IN1");
        addPassivePin("IN2");
        toggled = reverse;
    }

    @Override
    public void initOuts() {
        pin1 = getOutPin("IN1");
        pin2 = getOutPin("IN2");
    }

    @Override
    public AbstractUiComponent getComponent() {
        if (switchUiComponent == null) {
            switchUiComponent = new SwitchUiComponent(this, id, toggled);
        }
        return switchUiComponent;
    }

    public void toggle(boolean toggled) {
        this.toggled = toggled;
        recalculate();
    }

    @Override
    public void onPassivePinChange(SchemaPart source) {
        if (source != this) {
            recalculate();
        }
    }

    public void recalculate() {
        if (!toggled) {
            if (!pin1.hiImpedance) {
                pin1.setHiImpedance();
                pin1.hiImpedance = true;
            }
            if (!pin2.hiImpedance) {
                pin2.setHiImpedance();
                pin2.hiImpedance = true;
            }
        } else {
            MergerState pin1State = MergerState.calculate(pin1);
            MergerState pin2State = MergerState.calculate(pin2);
            if (pin1State == strong && pin2State == strong) {
                throw new ShortcutException(pin1, pin2);
            } else {
                if (pin2State == hiImpedance || pin1State.compareTo(pin2State) >= 0) {
                    if (!pin1.hiImpedance) {
                        pin1.setHiImpedance();
                        pin1.hiImpedance = true;
                    }
                } else {
                    pin1.strong = pin2State == strong;
                    pin1.setState(pin2.merger.state);
                    pin1.hiImpedance = false;
                }
                if (pin1State == hiImpedance || pin2State.compareTo(pin1State) >= 0) {
                    if (!pin2.hiImpedance) {
                        pin2.setHiImpedance();
                        pin2.hiImpedance = true;
                    }
                } else {
                    pin2.strong = pin1State == strong;
                    pin2.setState(pin1.merger.state);
                    pin2.hiImpedance = false;
                }
            }
        }
    }

    enum MergerState implements Comparable<MergerState> {
        hiImpedance,
        weak,
        strong;

        public static MergerState calculate(Pin target) {
            if (target.merger.hiImpedance) {
                return hiImpedance;
            }
            if (target.merger.strong && !target.strong) {
                return strong;
            }
            if ((target.strong || target.hiImpedance) ? target.merger.weakState != 0 : Math.abs(target.merger.weakState) > 1) {
                return weak;
            }
            return hiImpedance;
        }
    }
}
