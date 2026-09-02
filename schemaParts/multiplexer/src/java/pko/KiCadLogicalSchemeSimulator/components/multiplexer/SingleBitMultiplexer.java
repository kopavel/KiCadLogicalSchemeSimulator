/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.multiplexer;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class SingleBitMultiplexer extends SchemaPart {
    private final InPin[] inPins;
    private int nState;
    private Pin outPin;

    protected SingleBitMultiplexer(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int size = Integer.parseInt(params.get("size"));
        if (size > 30) {
            throw new RuntimeException("Component " + id + " max nSize is 30");
        }
        int partSize = (int) Math.pow(2, size);
        inPins = new InPin[partSize];
        for (int inNo = 0; inNo < partSize; inNo++) {
            int finalInNo = inNo;
            inPins[inNo] = addInPin(new InPin(String.valueOf(finalInNo), this) {
                @Override
                public void setHi() {
                    hiImpedance = false;
                    state = true;
                    if (finalInNo == nState /*&& outBus.state != newState*/) {
                        outPin.setHi();
                    }
                }

                @Override
                public void setLo() {
                    hiImpedance = false;
                    state = false;
                    if (finalInNo == nState /*&& outBus.state != newState*/) {
                        outPin.setLo();
                    }
                }
            });
        }
        for (int i = 0; i < size; i++) {
            int mask = 1 << i;
            int nMask = ~mask;
            addInPin(new InPin("N" + i, this) {
                @Override
                public void setHi() {
                    hiImpedance = false;
                    state = true;
                    nState |= mask;
                    if (!inPins[nState].hiImpedance && outPin.state != inPins[nState].state) {
                        if (inPins[nState].state) {
                            outPin.setHi();
                        } else {
                            outPin.setLo();
                        }
                    }
                }

                @Override
                public void setLo() {
                    hiImpedance = false;
                    state = false;
                    nState &= nMask;
                    if (!inPins[nState].hiImpedance && outPin.state != inPins[nState].state) {
                        if (inPins[nState].state) {
                            outPin.setHi();
                        } else {
                            outPin.setLo();
                        }
                    }
                }
            });
        }
        addOutPin("Q");
    }

    @Override
    public void initOuts() {
        outPin = getOutPin("Q");
    }
}
