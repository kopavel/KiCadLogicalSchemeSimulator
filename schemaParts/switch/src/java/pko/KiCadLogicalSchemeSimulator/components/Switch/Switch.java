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
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Switch extends SchemaPart implements InteractiveSchemaPart {
    private final PassivePin pin1;
    private final PassivePin pin2;
    public boolean toggled;
    private Pin in1;
    private Pin in2;
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        addPassivePin(new PassivePin("IN1", this) {
            @Override
            public void onChange() {
                recalculate(in2, this);
            }
        });
        addPassivePin(new PassivePin("IN2", this) {
            @Override
            public void onChange() {
                recalculate(in1, this);
            }
        });
        toggled = reverse;
        pin1 = (PassivePin) getOutPin("IN1");
        pin2 = (PassivePin) getOutPin("IN2");
    }

    @Override
    public void initOuts() {
        in1 = getOutPin("IN1");
        in2 = getOutPin("IN2");
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
        recalculate(in1, pin2);
        recalculate(in2, pin1);
    }

    private void recalculate(Pin pin, PassivePin otherPin) {
        if (!toggled) {
            if (!pin.hiImpedance) {
                pin.setHiImpedance();
            }
        } else if (pin instanceof PassivePin pp) {
            if (otherPin.otherImpedance || (otherPin.otherState == pp.otherState && otherPin.otherStrong == pp.otherStrong)) {
                if (!pin.hiImpedance) {
                    pin.setHiImpedance();
                }
            } else if (pin.hiImpedance || pin.strong != otherPin.otherStrong || pin.state!=otherPin.otherState){
                pin.strong = otherPin.otherStrong;
                if (otherPin.otherState) {
                    pin.setHi();
                } else {
                    pin.setLo();
                }
            }
        } else {
            if (otherPin.otherImpedance || (otherPin.otherState == pin.state && otherPin.otherStrong == pin.strong)) {
                if (!pin.hiImpedance) {
                    pin.setHiImpedance();
                }
            } else {
                pin.strong = otherPin.otherStrong;
                if (otherPin.otherState) {
                    pin.setHi();
                } else {
                    pin.setLo();
                }
            }
        }
    }
}
