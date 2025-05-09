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
