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
import pko.KiCadLogicalSchemeSimulator.api.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api_v2.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;

public class Switch extends SchemaPart implements InteractiveSchemaPart {
    private final PassivePin pin1;
    private final PassivePin pin2;
    private boolean toggled;
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        pin1 = addPassivePin(new PassivePin("IN1", this) {
            @Override
            public void setHiImpedance() {
                assert !hiImpedance : "Already in hiImpedance:" + this;
                hiImpedance = true;
                if (toggled && !pin2.hiImpedance) {
                    for (Pin destination : destinations) {
                        if (destination.state != pin2.state || destination.strong != pin2.strong) {
                            destination.state = pin2.state;
                            destination.strong = pin2.strong;
                            destination.setState(pin2.state, pin2.strong);
                        }
                    }
                } else {
                    for (Pin destination : destinations) {
                        if (!destination.hiImpedance) {
                            destination.hiImpedance = true;
                            destination.setHiImpedance();
                        }
                    }
                }
            }

            @Override
            public void resend() {
                if (!hiImpedance) {
                    setState(state, strong);
                }
            }

            @Override
            public void setState(boolean newState, boolean newStrong) {
                hiImpedance = false;
                state = newState;
                strong = newStrong;
                if (toggled && !pin2.hiImpedance) {
                    if (pin2.strong) {
                        if (newStrong) {
                            throw new ShortcutException(pin1, pin2);
                        } else {
                            for (Pin destination : destinations) {
                                if (destination.state != pin2.state || destination.strong != pin2.strong) {
                                    destination.state = pin2.state;
                                    destination.strong = pin2.strong;
                                    destination.setState(pin2.state, pin2.strong);
                                }
                            }
                        }
                    } else if (newStrong) {
                        for (Pin destination : destinations) {
                            if (destination.state != newState || !destination.strong) {
                                destination.state = newState;
                                destination.strong = true;
                                destination.setState(newState, true);
                            }
                        }
                    }
                } else {
                    for (Pin destination : destinations) {
                        if (destination.state != newState || destination.strong != newStrong) {
                            destination.state = newState;
                            destination.strong = newStrong;
                            destination.setState(newState, newStrong);
                        }
                    }
                }
            }
        });
        pin2 = addPassivePin(new PassivePin("IN2", this) {
            @Override
            public void setHiImpedance() {
                assert !hiImpedance : "Already in hiImpedance:" + this;
                hiImpedance = true;
                if (toggled && !pin1.hiImpedance) {
                    for (Pin destination : destinations) {
                        if (destination.state != pin1.state || destination.strong != pin1.strong) {
                            destination.state = pin1.state;
                            destination.strong = pin1.strong;
                            destination.setState(pin1.state, pin1.strong);
                        }
                    }
                } else {
                    for (Pin destination : destinations) {
                        if (!destination.hiImpedance) {
                            destination.hiImpedance = true;
                            destination.setHiImpedance();
                        }
                    }
                }
            }

            @Override
            public void resend() {
                if (!hiImpedance) {
                    setState(state, strong);
                }
            }

            @Override
            public void setState(boolean newState, boolean newStrong) {
                hiImpedance = false;
                state = newState;
                strong = newStrong;
                if (toggled && !pin1.hiImpedance) {
                    if (pin1.strong) {
                        if (newStrong) {
                            throw new ShortcutException(pin1, pin2);
                        } else {
                            for (Pin destination : destinations) {
                                if (destination.state != pin1.state || destination.strong != pin1.strong) {
                                    destination.state = pin1.state;
                                    destination.strong = pin1.strong;
                                    destination.setState(pin1.state, pin1.strong);
                                }
                            }
                        }
                    } else if (newStrong) {
                        for (Pin destination : destinations) {
                            if (destination.state != newState || !destination.strong) {
                                destination.state = newState;
                                destination.strong = true;
                                destination.setState(newState, true);
                            }
                        }
                    }
                } else {
                    for (Pin destination : destinations) {
                        if (destination.state != newState || destination.strong != newStrong) {
                            destination.state = newState;
                            destination.strong = newStrong;
                            destination.setState(newState, newStrong);
                        }
                    }
                }
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
}
