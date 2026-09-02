/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.rom;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class RomCsPin extends InPin {
    protected final int[] words;
    protected final Rom parent;
    public RomABus aBus;
    public Bus dBus;

    public RomCsPin(String id, Rom parent) {
        super(parent.reverse ? "~{" + id + "}" : id, parent);
        this.parent = parent;
        aBus = parent.aBus;
        words = parent.words;
        priority = 1;
    }

    /*Optimiser constructor*/
    public RomCsPin(RomCsPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        words = oldPin.words;
        dBus = oldPin.dBus;
        aBus = oldPin.aBus;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o block reverse*/
        if (parent.reverse) {
            /*Optimiser line useI*/
            aBus.iCsActive++;
            /*Optimiser line useB*/
            aBus.bCsActive = false;
            if (!dBus.hiImpedance) {
                dBus.setHiImpedance();
            }
            /*Optimiser line o bockEnd reverse block nReverse*/
        } else {
            /*Optimiser line useI*/
            aBus.iCsActive--;
            /*Optimiser line useB bind bHi:aBus.iCsActive\s==\s0*/
            aBus.bCsActive = aBus.iCsActive == 0;
            int word;
            Bus bus;
            if (
                /*Optimiser line useI*///
                    aBus.iCsActive == 0 &&//
                            ((bus = dBus).state != (word = words[aBus.state]) || bus.hiImpedance)) {
                bus.setState(word);
            }
            /*Optimiser line o blockEnd nReverse*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        Bus bus;
        int word;
        /*Optimiser line o block reverse*/
        if (parent.reverse) {
            /*Optimiser line useI*/
            aBus.iCsActive--;
            /*Optimiser line useB bind bHi:aBus.iCsActive\s==\s0*/
            aBus.bCsActive = aBus.iCsActive == 0;
            if (
                /*Optimiser line useI*///
                    aBus.iCsActive == 0 &&//
                            ((bus = dBus).state != (word = words[aBus.state]) || bus.hiImpedance)) {
                bus.setState(word);
            }
            /*Optimiser line o bockEnd reverse block nReverse*/
        } else {
            /*Optimiser line useI*/
            aBus.iCsActive++;
            /*Optimiser line useB*/
            aBus.bCsActive = false;
            if (!(bus = dBus).hiImpedance) {
                bus.setHiImpedance();
            }
            /*Optimiser line o blockEnd nReverse*/
        }
    }

    @Override
    public RomCsPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<RomCsPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (source != null) {
            optimiser.cut("setter");
        }
        if (parent.csCount == 1) {
            optimiser.cut("useI");
            optimiser.bind("bHi", true);
        } else {
            optimiser.cut("useB");
        }
        optimiser.cut(parent.reverse ? "nReverse" : "reverse");
        optimiser.bind("rev", parent.reverse ? "-1" : "1");
        RomCsPin build = optimiser.build();
        build.withState = source == null;
        build.source = source;
        parent.replaceIn(this, build);
        RomCsPin[] csPins = parent.csPins;
        for (int i = 0; i < csPins.length; i++) {
            if (csPins[i] == this) {
                csPins[i] = build;
            }
        }
        return build;
    }
}
