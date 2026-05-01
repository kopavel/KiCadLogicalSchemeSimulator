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
    private static final int TIMER = 0b100;
    private static final int TIMER_DEVIDER = 0b10000;
    private static final int DDR = 0b1;
    private static final int B_PART = 0b10;
    private static final int ENABLE_INTERRUPT = 0b1000;
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
    boolean timerInterruptOccured;
    boolean timerInterruptEnabled;
    boolean pa7InterruptOccured;
    boolean pa7InterruptEnabled;
    boolean pa7PositiveEdgeSensetive;
    int timerDivider = 1;
    int timerCount;
    final int[] ram = new int[128];
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
                selected = cs1Pin.state && !cs2Pin.state;
            }

            @Override
            public void setLo() {
                state = false;
                selected = false;
                aPart.reset();
                bPart.reset();
                pa7InterruptEnabled = false;
                pa7PositiveEdgeSensetive = false;
                timerInterruptEnabled = false;
                timerInterruptOccured = false;
                pa7InterruptOccured = false;
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
                selected = (!cs2Pin.state) && RESPin.state;
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
                selected = cs1Pin.state && RESPin.state;
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
                if (timerInterruptEnabled & timerCount == 0) {
                    timerCount = 255;
                    timerDivider = 1;
                    timerInterruptOccured = true;
                }
                if (!timerInterruptOccured && !pa7InterruptOccured) {
                    if (!IRQPin.hiImpedance) {
                        IRQPin.setHiImpedance();
                    }
                } else if (IRQPin.hiImpedance) {
                    IRQPin.setLo();
                }
                //Cpu read -> we write.
                if (selected && RWPin.state) {
                    if (RSPin.state) {
                        int addr = aBus.state;
                        if ((addr & TIMER) != 0) {
                            if ((addr & TIMER_DEVIDER) != 0) {
                                if ((addr & 1) == 0) {
                                    timerInterruptEnabled = (aBus.state & ENABLE_INTERRUPT) > 0;
                                    dOut.setState(timerCount / timerDivider);
                                    timerInterruptOccured = false;
                                } else {
                                    dOut.setState((timerInterruptOccured ? 0x80 : 0) | (pa7InterruptOccured ? 0x40 : 0));
                                    pa7InterruptOccured = false;
                                }
                            }
                        } else if ((addr & DDR) != 0) {
                            dOut.setState((addr & B_PART) == 0 ? aPart.direction : bPart.direction);
                        } else {
                            dOut.setState((addr & B_PART) == 0 ? aPart.getState() : bPart.getState());
                        }
                        //RAM
                    } else {
                        dOut.setState(ram[aBus.state]);
                    }
                }
            }

            @Override
            public void setLo() {
                state = false;
                if (!dOut.hiImpedance) {
                    dOut.setHiImpedance();
                }
                if (timerInterruptEnabled) {
                    timerCount--;
                }
                //Cpu write -> we read.
                if (selected && !RWPin.state) {
                    if (RSPin.state) {
                        int addr = aBus.state;
                        if ((addr & TIMER) != 0) {
                            if ((addr & TIMER_DEVIDER) != 0) {
                                timerInterruptEnabled = (aBus.state & ENABLE_INTERRUPT) > 0;
                                timerCount = (timerDivider = switch ((aBus.state & 3)) {
                                    case 0 -> 1;
                                    case 1 -> 8;
                                    case 2 -> 64;
                                    case 3 -> 1024;
                                    default -> throw new IllegalStateException("unreachable");
                                }) * dIn.state;
                                timerInterruptOccured = false;
                            } else {
                                pa7InterruptEnabled = (aBus.state & 0b10) > 0;
                                pa7PositiveEdgeSensetive = (aBus.state & 0b1) > 0;
                            }
                        } else if ((addr & DDR) != 0) {
                            if ((addr & B_PART) == 0) {
                                aPart.direction = dIn.state;
                            } else {
                                bPart.direction = dIn.state;
                            }
                        } else {
                            if ((addr & B_PART) == 0) {
                                aPart.setState();
                            } else {
                                bPart.setState();
                            }
                        }
                    } else {
                        ram[aBus.state] = dIn.state;
                    }
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
        return "DDRA:" + Integer.toBinaryString(aPart.direction) + "\n" +//
                "DDRB:" + Integer.toBinaryString(bPart.direction) + "\n" +//
                "PA7 Irq enabled:" + pa7InterruptEnabled + "\n" +//
                "PA7 Irq occured:" + pa7InterruptOccured + "\n" +//
                "PA7 PositiveEdgeSense:" + pa7PositiveEdgeSensetive + "\n" +//
                "Timer divider:" + timerDivider + "\n" +//
                "Timer count:" + timerCount + "\n" +//
                "Timer Irq enabled:" + timerInterruptEnabled + "\n" +//
                "Timer Irq occuder:" + timerInterruptOccured;
    }

    public Supplier<JPanel> extraPanel() {
        return () -> new MemoryDumpPanel(ram);
    }

    private final class Pins {
        final InPin[] ins = new InPin[8];
        final String suffix;
        final Pin[] outs = new Pin[8];
        int data;
        int direction;

        private Pins(String suffix, Mos6532 parent) {
            this.suffix = suffix;
            for (int i = 0; i < 8; i++) {
                if ("A".equals(suffix) && i == 7) {
                    ins[7] = addInPin(new InPin("PA7", parent) {
                        @Override
                        public void setHi() {
                            state = true;
                            if (pa7PositiveEdgeSensetive) {
                                pa7InterruptOccured = true;
                                if (pa7InterruptEnabled && IRQPin.hiImpedance) {
                                    IRQPin.setLo();
                                }
                            }
                        }

                        @Override
                        public void setLo() {
                            state = false;
                            if (!pa7PositiveEdgeSensetive) {
                                pa7InterruptOccured = true;
                                if (pa7InterruptEnabled && IRQPin.hiImpedance) {
                                    IRQPin.setLo();
                                }
                            }
                        }
                    });
                } else {
                    ins[i] = addInPin("P" + suffix + i);
                }
                addTriStateOutPin("P" + suffix + i);
            }
        }

        public void setState() {
            int data = (this.data = dIn.state);
            int direction = this.direction;
            int mask = 1;
            for (Pin out : outs) {
                if ((direction & mask) > 0) {
                    if ((data & mask) > 0) {
                        out.setHi();
                    } else {
                        out.setLo();
                    }
                } else {
                    out.setHiImpedance();
                }
                mask <<= 1;
            }
        }

        public void reset() {
            for (Pin out : outs) {
                out.setHiImpedance();
            }
            data = 0;
            direction = 0;
        }

        private int getState() {
            int data = this.data;
            int direction = this.direction;
            int result = 0;
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
