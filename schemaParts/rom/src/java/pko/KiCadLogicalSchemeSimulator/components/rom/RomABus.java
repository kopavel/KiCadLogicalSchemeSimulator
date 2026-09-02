/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.rom;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class RomABus extends InBus {
    protected final int[] words;
    protected final Rom parent;
    public Bus dBus;
    public int iCsActive;
    public boolean bCsActive;

    public RomABus(String id, Rom parent, int size, String... names) {
        super(id, parent, size, names);
        words = parent.words;
        this.parent = parent;
        iCsActive = parent.csCount;
    }

    /*Optimiser constructor*/
    public RomABus(RomABus oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        words = oldPin.words;
        iCsActive = oldPin.iCsActive;
        dBus = oldPin.dBus;
    }

    @Override
    public void setState(int newState) {
        state = newState;
        int word;
        if (
            /*Optimiser line useI*/
                iCsActive == 0 &&
                        /*Optimiser line useB*/
                        bCsActive &&//
                        dBus.state != (word = words[newState])) {
            dBus.setState(word);
        }
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        ClassOptimiser<RomABus> optimiser = new ClassOptimiser<>(this);
        if (parent.csCount == 1) {
            optimiser.cut("useI");
        } else {
            optimiser.cut("useB");
        }
        RomABus build = optimiser.build();
        build.source = source;
        parent.replaceIn(this, build);
        parent.aBus = build;
        for (RomCsPin csPin : parent.csPins) {
            csPin.aBus = build;
        }
        return build;
    }
}
