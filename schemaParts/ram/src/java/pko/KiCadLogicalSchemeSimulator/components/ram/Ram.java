/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.ram;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.tools.MemoryDumpPanel;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import javax.swing.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class Ram extends SchemaPart {
    private final int[] words;
    private final InBus dIn;
    private final int size;
    private final InPin csPin;
    private final InPin oePin;
    private final Bus aBus;
    private Bus dOut;

    protected Ram(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("size")) {
            throw new RuntimeException("Ram component need \"size\" parameter");
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("Ram component need \"aSize\" parameter");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException ignore) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        int aSize;
        try {
            aSize = Integer.parseInt(params.get("aSize"));
        } catch (NumberFormatException ignore) {
            throw new RuntimeException("Component " + id + " aSize must be positive number");
        }
        if (size < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (size > 32) {
            throw new RuntimeException("Component " + id + " max size is 32");
        }
        if (aSize < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (aSize > 31) {
            throw new RuntimeException("Component " + id + " max size is 31");
        }
        int ramSize = (int) Math.pow(2, aSize);
        words = new int[ramSize];
        int maskForSize = Utils.getMaskForSize(size);
        for (int i = 0; i < ramSize; i++) {
            words[i] = ThreadLocalRandom.current().nextInt() & maskForSize;
        }
        addTriStateOutBus("D", size);
        dIn = addInBus(params.containsKey("separateOut") ? "Din" : "D", size);
        if (reverse) {
            aBus = addInBus(new InBus("A", this, aSize) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    if (!csPin.state) {
                        rOut();
                    }
                }
            });
            csPin = addInPin(new InPin("~{CS}", this) {
                @Override
                public void setHi() {
                    state = true;
                    rOut();
                }

                @Override
                public void setLo() {
                    state = false;
                    rOut();
                }
            });
            oePin = addInPin(new InPin("~{OE}", this) {
                @Override
                public void setHi() {
                    state = true;
                    rOut();
                }

                @Override
                public void setLo() {
                    state = false;
                    rOut();
                }
            });
            addInPin(new InPin("~{WE}", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (!csPin.state) {
                        words[aBus.state] = dIn.state;
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
        } else {
            aBus = addInBus(new InBus("A", this, aSize) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    if (!csPin.state) {
                        out();
                    }
                }
            });
            csPin = addInPin(new InPin("CS", this) {
                @Override
                public void setHi() {
                    state = true;
                    out();
                }

                @Override
                public void setLo() {
                    state = false;
                    out();
                }
            });
            oePin = addInPin(new InPin("OE", this) {
                @Override
                public void setHi() {
                    state = true;
                    out();
                }

                @Override
                public void setLo() {
                    state = false;
                    out();
                }
            });
            addInPin(new InPin("WE", this) {
                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    if (csPin.state) {
                        words[aBus.state] = dIn.state;
                    }
                }
            });
        }
    }

    @Override
    public void initOuts() {
        dOut = getOutBus("D");
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(size / 4.0d) + "X", aBus.state) + "\nD:" +
                String.format("%0" + (int) Math.ceil(size / 4.0d) + "X", words[aBus.state]);
    }

    @Override
    public Supplier<JPanel> extraPanel() {
        return () -> new MemoryDumpPanel(words);
    }

    private void out() {
        if (oePin.state && csPin.state) {
            if (dOut.hiImpedance || dOut.state != words[aBus.state]) {
                dOut.setState(words[aBus.state]);
            }
        } else if (!dOut.hiImpedance) {
            dOut.setHiImpedance();
        }
    }

    private void rOut() {
        if (oePin.state || csPin.state) {
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
            }
        } else if (dOut.hiImpedance || dOut.state != words[aBus.state]) {
            dOut.setState(words[aBus.state]);
        }
    }
}
