/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.shifter.singleOut;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class SingleOutShifter extends SchemaPart {
    public final InBus dBus;
    public final InPin dsPins;
    public final int hiDsMask;
    public final int latchMask;
    public final int outMask;
    final SingleOutShifterRPin rPin;
    final SingleOutShifterCIPin ciPin;
    public int latch;
    public Pin out;
    //Todo Migrate to 'hot pin' locals
    public boolean parallelLoad;
    public boolean clockEnabled;
    public SingleOutShifterCPin cp;
    public SingleOutShifterCPin cn;

    protected SingleOutShifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        if (!params.containsKey("outPin")) {
            throw new RuntimeException("Component " + id + " has no parameter \"outPin\"");
        }
        outMask = 1 << Integer.parseInt(params.get("outPin"));
        dBus = addInBus("D", dSize);
        boolean plReverse = params.containsKey("plReverse");
        latchMask = Utils.getMaskForSize(dSize);
        hiDsMask = 1 << (dSize - 1);
        dsPins = addInPin("DS");
        addInPin(new SingleOutShifterPlPin("PL", this, plReverse));
        boolean clearReverse = params.containsKey("clearReverse");
        boolean inhibitReverse = params.containsKey("inhibitReverse");
        clockEnabled = !clearReverse && !inhibitReverse;
        rPin = addInPin(new SingleOutShifterRPin("R", this, inhibitReverse, clearReverse));
        ciPin = addInPin(new SingleOutShifterCIPin("CI", this, inhibitReverse, clearReverse));
        rPin.ciPin = ciPin;
        ciPin.rPin = rPin;
        cp = addInPin(new SingleOutShifterCPin("CP", this, reverse, false));
        cn = addInPin(new SingleOutShifterCPin("CN", this, reverse, true));
        addOutPin("Q", false);
    }

    @Override
    public void initOuts() {
        out = getOutPin("Q");
        cn.out = out;
        cp.out = out;
    }

    @Override
    public String extraState() {
        return "latch" + "\n" + Integer.toBinaryString(latch);
    }

    @Override
    public void reset() {
        latch = 0;
        out.setLo();
    }
}
