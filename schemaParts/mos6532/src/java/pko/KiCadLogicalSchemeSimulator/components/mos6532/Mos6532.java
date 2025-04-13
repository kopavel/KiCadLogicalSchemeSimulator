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
package pko.KiCadLogicalSchemeSimulator.components.mos6532;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.tools.MemoryDumpPanel;

import javax.swing.*;
import java.util.function.Supplier;

public class Mos6532 extends SchemaPart {
    Bus dOut;
    InBus dIn;
    InBus aBus;
    InPin RWPin;
    InPin cs1Pin;
    InPin cs2Pin;
    InPin RSPin;
    InPin RESPin;
    Pin IRQPin;
    Pins aPart = new Pins("A", this);
    Pins bPart = new Pins("B", this);
    boolean timerFlag;
    boolean pa7Flag;
    boolean pa7Interrupt;
    boolean timerInterrupt;
    boolean pa7PositiveEdge;
    long timerDivider = 1;
    long timerCount;
    long[] ram = new long[128];
    boolean selected;

    protected Mos6532(String id, String sParam) {
        super(id, sParam);
        dIn = addInBus("D", 8);
        aBus = addInBus("A", 7);
        RWPin = addInPin("R/~{W}");
        RSPin = addInPin("~{RS}");
        RESPin = addInPin(new InPin("~{RES}", this) {
            @Override
            public void setHi() {
                state = true;
                selected = cs1Pin.state & !cs2Pin.state;
            }

            @Override
            public void setLo() {
                state = false;
                selected = false;
                aPart.reset();
                bPart.reset();
                pa7Interrupt = false;
                pa7PositiveEdge = false;
                timerInterrupt = false;
                timerFlag = false;
                pa7Flag = false;
                if (!IRQPin.hiImpedance) {
                    IRQPin.setHiImpedance();
                }
                if (!dOut.hiImpedance) {
                    dOut.setHiImpedance();
                }
            }
        });
        addTriStateOutPin("~{IRQ}");
        cs1Pin = addInPin(new InPin("CS1", this) {
            @Override
            public void setHi() {
                state = true;
                selected = (!cs2Pin.state) & RESPin.state;
                if (!selected && !dOut.hiImpedance) {
                    dOut.setHiImpedance();
                }
            }

            @Override
            public void setLo() {
                state = false;
                selected = false;
                if (!dOut.hiImpedance) {
                    dOut.setHiImpedance();
                }
            }
        });
        cs2Pin = addInPin(new InPin("~{CS2}", this) {
            @Override
            public void setHi() {
                state = true;
                selected = false;
                if (!dOut.hiImpedance) {
                    dOut.setHiImpedance();
                }
            }

            @Override
            public void setLo() {
                state = false;
                selected = cs1Pin.state & RESPin.state;
                if (!selected && !dOut.hiImpedance) {
                    dOut.setHiImpedance();
                }
            }
        });
        cs1Pin.priority = -1;
        cs2Pin.priority = 1;
        addTriStateOutBus("D", 8);
        addInPin(new InPin("F2", this) {
            @Override
            public void setHi() {
                state = true;
                if (timerCount == 0) {
                    timerCount = 255;
                    timerDivider = 1;
                    timerFlag = true;
                    if (timerInterrupt && IRQPin.hiImpedance) {
                        IRQPin.setLo();
                    }
                }
                if (selected) {
                    if (RWPin.state) {
                        if (RSPin.state) {
                            int addr = (int) aBus.state;
                            if ((addr & 0b100) == 0) {
                                int mask;
                                if ((mask = (addr & 3)) == 0) {
                                    dOut.setState(aPart.getState());
                                } else if (mask == 1) {
                                    dOut.setState(aPart.direction);
                                } else if (mask == 2) {
                                    dOut.setState(bPart.getState());
                                } else {
                                    dOut.setState(bPart.direction);
                                }
                                return;
                            }
                            if ((addr & 1) == 0) {
                                timerInterrupt = (aBus.state & 0b1000) > 0;
                                dOut.setState(timerCount / timerDivider);
                                timerFlag = false;
                                if (!pa7Interrupt || !pa7Flag) {
                                    if (!IRQPin.hiImpedance) {
                                        IRQPin.setHiImpedance();
                                    }
                                }
                                return;
                            }
                            dOut.setState((timerFlag ? 128 : 0) | (pa7Flag ? 65 : 0));
                            pa7Flag = false;
                            if (!timerInterrupt || !timerFlag) {
                                if (!IRQPin.hiImpedance) {
                                    IRQPin.setHiImpedance();
                                }
                            }
                            return;
                        } else {
                            dOut.setState(ram[(int) aBus.state]);
                        }
                    }
                }
            }

            @Override
            public void setLo() {
                state = false;
                timerCount--;
                if (selected) {
                    if (!RWPin.state) {
                        if (RSPin.state) {
                            int addr = (int) aBus.state;
                            if ((addr & 0b100) == 0) {
                                int mask;
                                if ((mask = (addr & 3)) == 0) {
                                    aPart.setState();
                                    return;
                                } else if (mask == 1) {
                                    aPart.direction = dIn.state;
                                    return;
                                } else if (mask == 2) {
                                    bPart.setState();
                                    return;
                                } else {
                                    bPart.direction = dIn.state;
                                    return;
                                }
                            }
                            if ((addr & 0b10000) > 0) {
                                timerInterrupt = (aBus.state & 0b1000) > 0;
                                timerCount = (timerDivider = switch ((int) (aBus.state & 3)) {
                                    case 0 -> 1;
                                    case 1 -> 8;
                                    case 2 -> 64;
                                    case 3 -> 1024;
                                    default -> throw new IllegalStateException("unreachable");
                                }) * dIn.state;
                                timerFlag = false;
                                if (!pa7Interrupt || !pa7Flag) {
                                    if (!IRQPin.hiImpedance) {
                                        IRQPin.setHiImpedance();
                                    }
                                }
                                return;
                            } else {
                                pa7Interrupt = (aBus.state & 2) > 0;
                                pa7PositiveEdge = (aBus.state & 1) > 0;
                                return;
                            }
                        } else {
                            ram[(int) aBus.state] = dIn.state;
                        }
                    }
                }
                if (!dOut.hiImpedance) {
                    dOut.setHiImpedance();
                }
            }
        });
    }

    @Override
    public void initOuts() {
        dOut = getOutBus("D");
        dOut.setHiImpedance();
        IRQPin = getOutPin("~{IRQ}");
        aPart.initOuts();
        bPart.initOuts();
    }

    @Override
    public String extraState() {
        return "DDRA:" + Long.toBinaryString(aPart.direction) + "\n" +//
                "DDRB:" + Long.toBinaryString(bPart.direction) + "\n" +//
                "pa7 Flag:" + pa7Flag + "\n" +//
                "pa7 PositiveEdge:" + pa7PositiveEdge + "\n" +//
                "pa7 Interrupt:" + pa7Interrupt + "\n" +//
                "Timer count:" + timerCount + "\n" +//
                "Timer divider:" + timerDivider + "\n" +//
                "timer Flag:" + timerFlag + "\n" +//
                "timer Interrupt:" + timerInterrupt;
    }

    public Supplier<JPanel> extraPanel() {
        return () -> new MemoryDumpPanel(ram);
    }

    private class Pins {
        final InPin[] ins = new InPin[8];
        final String suffix;
        final Pin[] outs = new Pin[8];
        long data;
        long direction;

        private Pins(String suffix, Mos6532 parent) {
            this.suffix = suffix;
            for (int i = 0; i < 8; i++) {
                if (suffix.equals("A") && i == 7) {
                    ins[i] = addInPin(new InPin("PA7", parent) {
                        @Override
                        public void setHi() {
                            state = true;
                            if (pa7PositiveEdge) {
                                pa7Flag = true;
                                if (pa7Interrupt && IRQPin.hiImpedance) {
                                    IRQPin.setLo();
                                }
                            }
                        }

                        @Override
                        public void setLo() {
                            state = false;
                            if (!pa7PositiveEdge) {
                                pa7Flag = true;
                                if (pa7Interrupt && IRQPin.hiImpedance) {
                                    IRQPin.setLo();
                                }
                            }
                        }
                    });
                } else {
                    ins[i] = addInPin("P" + suffix + i);
                }
                addOutPin("P" + suffix + i);
            }
        }

        public void setState() {
            long data = (this.data = dIn.state);
            long direction = this.direction;
            int mask = 1;
            for (Pin out : outs) {
                if ((direction & mask) > 0) {
                    if ((data & mask) > 0) {
                        out.setHi();
                    } else {
                        out.setLo();
                    }
                }
                mask <<= 1;
            }
        }

        public void reset() {
            for (Pin out : outs) {
                out.setLo();
            }
            data = 0;
            direction = 0;
        }

        private long getState() {
            long data = this.data;
            long direction = this.direction;
            long result = 0;
            int mask = 1;
            for (InPin in : ins) {
                if ((direction & mask) > 0) {
                    if (in.state) {
                        result |= mask;
                    }
                } else {
                    result |= (data & mask);
                }
                mask <<= 1;
            }
            return result;
        }

        void initOuts() {
            for (int i = 0; i < 8; i++) {
                outs[i] = getOutPin("P" + suffix + i);
            }
        }
    }
}
