/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.multiplexer.masked;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import java.util.ArrayList;
import java.util.Collection;

public class MaskedMultiplexer extends SchemaPart {
    public final InBus[] inBuses;
    public final Collection<MaskedMultiplexerOEPin> oePins = new ArrayList<>();
    public final Collection<MaskedMultiplexerNPin> nPins = new ArrayList<>();
    public MaskedMultiplexerOEPin oePin;
    public int outMask;
    public int nState;
    public Bus outBus;

    protected MaskedMultiplexer(String id, String sParam) {
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
        String[] aliases = new String[partsAmount];
        for (byte i = 0; i < partsAmount; i++) {
            aliases[i] = "Q" + (char) ('A' + i);
        }
        addOutBus("Q", partsAmount, aliases);
        inBuses = new InBus[partSize];
        outMask = reverse ? -1 : 0;
        for (int inNo = 0; inNo < partSize; inNo++) {
            for (int part = 0; part < partsAmount; part++) {
                aliases[part] = ((char) ('A' + part) + "" + inNo);
            }
            int finalInNo = inNo;
            inBuses[inNo] = addInBus(new InBus(String.valueOf(finalInNo), this, partsAmount, aliases) {
                @Override
                public void setState(int newState) {
                    state = newState;
                    int state;
                    if (finalInNo == nState && outBus.state != (state = (newState & outMask))) {
                        outBus.setState(state);
                    }
                }
            });
        }
        oePin = addInPin(new MaskedMultiplexerOEPin("OE", this, -1));
        for (int i = 0; i < partSize; i++) {
            oePins.add(addInPin(new MaskedMultiplexerOEPin("OE" + (char) ('a' + i), this, 1 << i)));
        }
        for (int i = 0; i < nSize; i++) {
            nPins.add(addInPin(new MaskedMultiplexerNPin("N" + i, this, 1 << i)));
        }
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        oePin.outBus = outBus;
        for (MaskedMultiplexerOEPin oePin : oePins) {
            oePin.outBus = outBus;
        }
        for (MaskedMultiplexerNPin nPin : nPins) {
            nPin.outBus = outBus;
        }
    }
}
