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
package lv.pko.KiCadLogicalSchemeSimulator.components.Switch;
import lv.pko.KiCadLogicalSchemeSimulator.api.AbstractUiComponent;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.schemaPart.InteractiveSchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.schemaPart.SchemaPart;

public class Switch extends SchemaPart implements InteractiveSchemaPart {
    //FixMe using passive pins?
    private final InPin pin1In;
    private final InPin pin2In;
    private boolean toggled;
    private Pin pin1Out;
    private Pin pin2Out;
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        pin1In = addInPin(new InPin("IN1", this) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
                if (!pin2Out.hiImpedance) {
                    pin2Out.setHiImpedance();
                }
            }

            @Override
            public void setState(boolean newState, boolean strong) {
                hiImpedance = false;
                this.strong = strong;
                state = newState;
                if (toggled) {
                    if (!pin2In.hiImpedance && pin2In.strong) {
                        throw new ShortcutException(pin1In, pin2In);
                    } else if (pin2Out.state != newState || pin2Out.hiImpedance) {
                        pin2Out.setState(newState, strong);
                    }
                }
            }
        });
        pin2In = addInPin(new InPin("IN2", this) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
                if (!pin1Out.hiImpedance) {
                    pin1Out.setHiImpedance();
                }
            }

            @Override
            public void setState(boolean newState, boolean strong) {
                hiImpedance = false;
                this.strong = strong;
                state = newState;
                if (toggled) {
                    if (!pin1In.hiImpedance && pin1In.strong) {
                        throw new ShortcutException(pin1In, pin2In);
                    } else if (pin1Out.state != newState || pin1Out.hiImpedance) {
                        pin1Out.setState(newState, strong);
                    }
                }
            }
        });
        addOutPin("IN1");
        addOutPin("IN2");
        toggled = reverse;
    }

    @Override
    public void initOuts() {
        pin1Out = getOutPin("IN1");
        pin2Out = getOutPin("IN2");
        pin1Out.setHiImpedance();
        pin1Out.hiImpedance = true;
        pin2Out.setHiImpedance();
        pin2Out.hiImpedance = true;
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
        if (toggled) {
            if (pin1In.hiImpedance) {
                if (!pin2Out.hiImpedance) {
                    pin2Out.setHiImpedance();
                }
            } else if (pin1In.strong != pin2Out.strong || pin1In.state != pin2Out.state) {
                pin2Out.setState(pin1In.state, pin1In.strong);
            }
            if (pin2In.hiImpedance) {
                if (!pin1Out.hiImpedance) {
                    pin1Out.setHiImpedance();
                }
            } else if (pin1In.strong != pin2Out.strong || pin1In.state != pin2Out.state) {
                pin2Out.setState(pin1In.state, pin1In.strong);
            }
        } else {
            if (!pin1Out.hiImpedance) {
                pin1Out.setHiImpedance();
            }
            if (!pin1Out.hiImpedance) {
                pin2Out.setHiImpedance();
            }
        }
    }
}
