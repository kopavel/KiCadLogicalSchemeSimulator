/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.busDriver;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;

import java.util.Arrays;

public class BusDriver extends SchemaPart {
    private final Bus[] outs;
    private final InBus[] ins;
    private final boolean[] oe;
    private final int partAmount;

    public BusDriver(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        Integer[] sizes = Arrays.stream(params.get("size").split(","))
                .map(Integer::parseInt).toArray(Integer[]::new);
        partAmount = sizes.length;
        ins = new InBus[partAmount];
        oe = new boolean[partAmount];
        outs = new Bus[partAmount];
        for (int i = 0; i < partAmount; i++) {
            int finalI = i;
            ins[i] = addInBus(new InBus("I" + (char) ('a' + finalI), this, sizes[finalI]) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    if (oe[finalI]) {
                        outs[finalI].setState(newState);
                    }
                }
            });
            addTriStateOutBus("O" + (char) ('a' + i), sizes[i]);
            if (reverse) {
                InPin inPin = addInPin(new InPin("OE" + (char) ('a' + i), this) {
                    @Override
                    public void setHi() {
                        state = true;
                        oe[finalI] = false;
                        if (!outs[finalI].hiImpedance) {
                            outs[finalI].setHiImpedance();
                        }
                    }

                    @Override
                    public void setLo() {
                        state = false;
                        oe[finalI] = true;
                        outs[finalI].setState(ins[finalI].state);
                    }
                });
                inPin.state = true;
                inPin.priority = 1;
            } else {
                addInPin(new InPin("OE" + (char) ('a' + i), this) {
                    @Override
                    public void setHi() {
                        state = true;
                        oe[finalI] = true;
                        outs[finalI].setState(ins[finalI].state);
                    }

                    @Override
                    public void setLo() {
                        state = false;
                        oe[finalI] = false;
                        if (!outs[finalI].hiImpedance) {
                            outs[finalI].setHiImpedance();
                        }
                    }
                }).priority = -1;
            }
        }
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < partAmount; i++) {
            outs[i] = getOutBus("O" + (char) ('a' + i));
        }
    }
}
