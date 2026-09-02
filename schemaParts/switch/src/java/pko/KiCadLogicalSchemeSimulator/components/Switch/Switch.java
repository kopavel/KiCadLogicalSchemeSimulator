/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        pin1 = addPassivePin(new PassivePin("IN1", this) {
            @Override
            public void onChange() {
                recalculate(pin2, this);
                recalculate(this, pin2);
            }
        });
        pin2 = addPassivePin(new PassivePin("IN2", this) {
            @Override
            public void onChange() {
                recalculate(pin1, this);
                recalculate(this, pin1);
            }
        });
        toggled = reverse;
    }

    @Override
    public void initOuts() {
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
        recalculate(pin1, pin2);
        recalculate(pin2, pin1);
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
                if (otherPin.otherState) {
                    pin.setHi(otherPin.otherStrong);
                } else {
                    pin.setLo(otherPin.otherStrong);
                }
            }
        } else {
            if (otherPin.otherImpedance || (otherPin.otherState == pin.state && otherPin.otherStrong == pin.strong)) {
                if (!pin.hiImpedance) {
                    pin.setHiImpedance();
                }
            } else {
                if (otherPin.otherState) {
                    pin.setHi(otherPin.otherStrong);
                } else {
                    pin.setLo(otherPin.otherStrong);
                }
            }
        }
    }
}
