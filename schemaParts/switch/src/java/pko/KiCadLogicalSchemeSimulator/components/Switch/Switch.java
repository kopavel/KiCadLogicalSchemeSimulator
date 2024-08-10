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
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class Switch extends SchemaPart implements InteractiveSchemaPart {
    private final PassivePin pin1;
    private final PassivePin pin2;
    private boolean toggled;
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        //FixMe create non abstract class as Switch Pin, so can use optimizer on it.
        pin1 = addPassivePin(new PassivePin("IN1", this) {
            @Override
            public void setHiImpedance() {
                Switch.this.setImpedance(this, pin2);
            }

            @Override
            public void setState(boolean newState) {
                Switch.this.setState(this, newState, pin2);
            }
        });
        pin2 = addPassivePin(new PassivePin("IN2", this) {
            @Override
            public void setHiImpedance() {
                Switch.this.setImpedance(this, pin1);
            }

            @Override
            public void setState(boolean newState) {
                Switch.this.setState(this, newState, pin1);
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
        pin1.resend();
        pin2.resend();
    }

    private void setImpedance(PassivePin pin1, PassivePin pin2) {
//        assert !pin1.inImpedance : "Already in hiImpedance:" + pin1;
        pin1.inImpedance = true;
        if (toggled && !pin2.inImpedance) {
            for (Pin destination : pin1.destinations) {
                pin1.hiImpedance = false;
                pin1.state = pin2.state;
                pin1.strong = pin2.strong;
                if (destination.state != pin2.state || destination.strong != pin2.strong) {
                    destination.state = pin2.state;
                    destination.strong = pin2.strong;
                    destination.setState(pin2.state);
                    destination.hiImpedance = false;
                }
            }
        } else {
            pin1.hiImpedance = true;
            for (Pin destination : pin1.destinations) {
                if (!destination.hiImpedance) {
                    destination.setHiImpedance();
                    destination.hiImpedance = true;
                }
            }
        }
    }

    private void setState(PassivePin pin1, boolean newState, PassivePin pin2) {
        pin1.inImpedance = false;
        pin1.inState = newState;
        pin1.inStrong = pin1.source == null || pin1.source.strong;
        pin1.hiImpedance = false; //in all scenarios
        if (toggled && !pin2.inImpedance) { // second pin meter...
            if (pin2.inStrong) { // second pin are strong
                if (pin1.inStrong) { // both in strong - shortcut
                    if (Net.stabilizing) {
                        Net.forResend.add(pin1);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", pin1, pin2);
                    } else {
                        throw new ShortcutException(pin1, pin2);
                    }
                } else { // we are weak - use second pin
                    pin1.state = pin2.inState;
                    pin1.strong = true; //we are strong too now
                    for (Pin destination : pin1.destinations) {
                        if (destination.state != pin2.inState || destination.strong != pin2.inStrong) {
                            destination.state = pin2.inState;
                            destination.strong = true;
                            destination.hiImpedance = false;
                            destination.setState(destination.state);
                        }
                    }
                }
            } else if (pin1.inStrong) {
                //second pin are weak but we are strong
                pin1.state = newState;
                pin1.strong = true;
                //FixMe move to passive pin itself and make unroll
                for (Pin destination : pin1.destinations) {
                    if (destination.state != newState || !destination.strong) {
                        destination.state = newState;
                        destination.strong = true;
                        destination.setState(newState);
                        destination.hiImpedance = false;
                    }
                }
            } else if (pin1.inState != pin2.inState) {
                //both are in opposite weak - shortcut
                if (Net.stabilizing) {
                    Net.forResend.add(pin1);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", pin1, pin2);
                } else {
                    throw new ShortcutException(pin1, pin2);
                }
            }
        } else {
            // second pin doesn't meter
            pin1.state = newState;
            pin1.strong = pin1.inStrong;
            for (Pin destination : pin1.destinations) {
                if (destination.state != newState || destination.strong != pin1.strong) {
                    destination.state = newState;
                    destination.strong = pin1.strong;
                    destination.setState(newState);
                    destination.hiImpedance = false;
                }
            }
        }
    }
}
