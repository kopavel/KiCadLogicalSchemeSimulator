/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.counter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class Counter extends SchemaPart {
    public CInRaisingPin in;
    public CInFallingPin nIn;
    Bus outBus;
    public boolean enabled = true;
    CRInPin rPin;

    protected Counter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int pinAmount;
        try {
            pinAmount = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (pinAmount < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        int countMask = Utils.getMaskForSize(pinAmount);
        addOutBus("Q", pinAmount);
        if (reverse) {
            nIn = addInPin(new CInFallingPin("C", this, countMask));
        } else {
            in = addInPin(new CInRaisingPin("C", this, countMask));
        }
        rPin = addInPin(new CRInPin(this));
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        outBus.useBitPresentation = true;
        rPin.out = outBus;
        if (reverse) {
            nIn.out = outBus;
        } else {
            in.out = outBus;
        }
    }

    @Override
    public void reset() {
        if (outBus.getState() > 0 || outBus.hiImpedance) {
            outBus.setState(0);
        }
        if (reverse) {
            nIn.oState=0;
        } else {
            in.oState=0;
        }
    }
}
