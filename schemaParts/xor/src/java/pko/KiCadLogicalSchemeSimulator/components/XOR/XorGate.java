/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.XOR;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class XorGate extends SchemaPart {
    private final InPin in1;
    private final InPin in2;
    private Pin out;

    protected XorGate(String id, String sParam) {
        super(id, sParam);
        if (reverse) {
            in1 = addInPin(new InPin("IN0", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (out.state != (in2.state)) {
                        if (in2.state) {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        } else {
                            out.setLo();
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (out.state == in2.state) {
                        if (in2.state) {
                            out.setLo();
                        } else {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        }
                    }
                }
            });
            in2 = addInPin(new InPin("IN1", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (out.state != in1.state) {
                        if (in1.state) {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        } else {
                            out.setLo();
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (out.state == in1.state) {
                        if (!in1.state) {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        } else {
                            out.setLo();
                        }
                    }
                }
            });
        } else {
            in1 = addInPin(new InPin("IN0", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (out.state == (in2.state)) {
                        if (in2.state) {
                            out.setLo();
                        } else {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (out.state != in2.state) {
                        if (in2.state) {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        } else {
                            out.setLo();
                        }
                    }
                }
            });
            in2 = addInPin(new InPin("IN1", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (out.state == in1.state) {
                        if (in1.state) {
                            out.setLo();
                        } else {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        }
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    if (out.state != in1.state) {
                        if (in1.state) {
                            if (parent.params.containsKey("openCollector")){
                                out.setHiImpedance();
                                /*Optimiser line o blockEnd oc block rc*/
                            } else {
                                out.setHi();
                                /*Optimiser line o blockEnd rc*/
                            }
                        } else {
                            out.setLo();
                        }
                    }
                }
            });
        }
        if (params.containsKey("openCollector")){
            addTriStateOutPin("OUT", false);
        } else {
            addOutPin("OUT", false);
        }
    }

    @Override
    public void initOuts() {
        out = getOutPin("OUT");
        if (in1.state == in2.state) {
            out.setLo();
        } else {
            if (params.containsKey("openCollector")){
                out.setHiImpedance();
                /*Optimiser line o blockEnd oc block rc*/
            } else {
                out.setHi();
                /*Optimiser line o blockEnd rc*/
            }
        }
    }

    @Override
    public String extraState() {
        return reverse ? "reverse" : null;
    }
}
