/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.multiplexer;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;

import java.util.ArrayList;
import java.util.List;

public class Multiplexer extends SchemaPart {
    private final InBus[] inBuses;
    private int nState;
    private Bus outBus;

    protected Multiplexer(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("nSize")) {
            throw new RuntimeException("Component " + id + " has no parameter \"nSize\"");
        }
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int partsAmount = Integer.parseInt(params.get("size"));
        int nSize = Integer.parseInt(params.get("nSize"));
        if (nSize > 30) {
            throw new RuntimeException("Component " + id + " max nSize is 30");
        }
        int partSize = (int) Math.pow(2, nSize);
        inBuses = new InBus[partSize];
        for (int inNo = 0; inNo < partSize; inNo++) {
            List<String> aliases = new ArrayList<>();
            for (int part = 0; part < partsAmount; part++) {
                aliases.add((char) ('A' + part) + "" + inNo);
            }
            int finalInNo = inNo;
            inBuses[inNo] = addInBus(new InBus(String.valueOf(finalInNo), this, partsAmount, aliases.toArray(new String[0])) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    if (finalInNo == nState /*&& outBus.state != newState*/) {
                        outBus.setState(newState);
                    }
                }
            });
        }
        for (int i = 0; i < nSize; i++) {
            int finalI = i;
            addInPin(new InPin("N" + finalI, this) {
                final int mask = 1 << finalI;
                final int nMask = ~mask;
                @Override
                public void setHi() {
                    state = true;
                    int lState = (nState |= mask);
                    int state;
                    Bus out;
                    if ((out = outBus).state != (state = inBuses[lState].state)) {
                        out.setState(state);
                    }
                }

                @Override
                public void setLo() {
                    state = false;
                    int s = (nState &= nMask);
                    int state;
                    Bus out;
                    if ((out = outBus).state != (state = inBuses[s].state)) {
                        out.setState(state);
                    }
                }
            });
        }
        String[] aliases = new String[partsAmount];
        for (byte i = 0; i < partsAmount; i++) {
            aliases[i] = "Q" + (char) ('A' + i);
        }
        addOutBus("Q", partsAmount, aliases);
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
    }
}
