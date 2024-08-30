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
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Switch extends SchemaPart implements InteractiveSchemaPart {
    public boolean toggled;
    private Pin pin1;
    private Pin pin2;
    private pko.KiCadLogicalSchemeSimulator.components.Switch.SwitchUiComponent switchUiComponent;

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
            switchUiComponent = new pko.KiCadLogicalSchemeSimulator.components.Switch.SwitchUiComponent(this, id, toggled);
        }
        return switchUiComponent;
    }

    public void toggle(boolean toggled) {
        this.toggled = toggled;
        recalculate(pin1, pin2);
        recalculate(pin2, pin1);
    }

    @Override
    public void onPassivePinChange(Pin merger) {
        if (pin1.merger != merger) {
            recalculate(pin1, pin2);
        } else {
            recalculate(pin2, pin1);
        }
    }

    public void recalculate(Pin pin, Pin otherPin) {
        if (!toggled || otherPin.merger.hiImpedance) {
            if (!pin.hiImpedance) {
                pin.setHiImpedance();
            }
        } else if (otherPin.merger.strong && !otherPin.strong) {
            if (!pin.strong || pin.state != otherPin.merger.state) {
                pin.strong = true;
                pin.setState(otherPin.merger.state);
            }
        } else if ((otherPin.strong || otherPin.hiImpedance) ? otherPin.merger.weakState != 0 : Math.abs(otherPin.merger.weakState) > 1) {
            if (pin.hiImpedance || pin.strong || pin.state != otherPin.merger.weakState > 0) {
                pin.strong = false;
                pin.setState(otherPin.merger.weakState > 0);
            }
        } else if (!pin.hiImpedance) {
            pin.setHiImpedance();
        }
    }
}
