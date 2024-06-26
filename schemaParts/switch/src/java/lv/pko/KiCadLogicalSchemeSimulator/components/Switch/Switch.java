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
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

//FixME rework it completely for new model
public class Switch extends SchemaPart implements InteractiveSchemaPart {
    private final InPin pin1In;
    private final InPin pin2In;
    private boolean toggled;
    private boolean pin1HiImpedance;
    private boolean pin2HiImpedance;
    private TriStateOutPin pin1Out;
    private TriStateOutPin pin2Out;
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        pin1In = addInPin(new InPin("IN1", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                pin1HiImpedance = hiImpedance;
                if (toggled) {
                    if (!hiImpedance && !pin2HiImpedance) {
                        throw new ShortcutException(pin1In, pin2In);
                    }
                    if (hiImpedance) {
                        pin2Out.setHiImpedance();
/*
                    } else if (!pin1Weak) {
                        synchronized (pin2Out) {
                            pin2Out.setState(newState);
                        }
*/
                    }
                }
            }
        });
        pin2In = addInPin(new InPin("IN2", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                if (!hiImpedance && !pin1HiImpedance) {
                    throw new ShortcutException(pin1In, pin2In);
                }
                pin2HiImpedance = hiImpedance;
                if (toggled) {
                    if (hiImpedance) {
                        pin1Out.setHiImpedance();
/*
                    } else if (!pin2Weak) {
                        synchronized (pin1Out) {
                            pin1Out.setState(state);
                        }
*/
                    }
                }
            }
        });
        addTriStateOutPin("IN1", 1);
        addTriStateOutPin("IN2", 1);
    }

    @Override
    public void initOuts() {
        pin1Out = (TriStateOutPin) getOutPin("IN1");
        pin2Out = (TriStateOutPin) getOutPin("IN2");
    }

    @Override
    public AbstractUiComponent getComponent() {
        if (switchUiComponent == null) {
            switchUiComponent = new SwitchUiComponent(this, id);
        }
        return switchUiComponent;
    }

    public void toggle(boolean toggled) {
        this.toggled = toggled;
        if (toggled) {
            if (!pin2HiImpedance && !pin1HiImpedance) {
                throw new ShortcutException(pin1In, pin2In);
            }
        } else {
            pin1Out.setHiImpedance();
            pin2Out.setHiImpedance();
        }
    }
}
