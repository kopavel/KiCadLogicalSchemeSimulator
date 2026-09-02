/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.counter.multipart;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class MultiPartRIn extends InPin {
    public final boolean reverse;
    public final MultiPartCounter parent;
    public final int mask;
    public final int nMask;
    public final int no;
    public final MultiPartCIn[] cIns;

    public MultiPartRIn(String id, MultiPartCounter parent, boolean reverse, int no) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        cIns = parent.cIns;
        mask = 1 << no;
        nMask = ~mask;
        this.no = no;
    }

    @SuppressWarnings("unused")
    /*Optimiser constructor unroll cIn:cIns*///
    public MultiPartRIn(MultiPartRIn oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        parent = oldPin.parent;
        cIns = oldPin.cIns;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        no = oldPin.no;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o block nr*/
        if (!reverse) {
            /*Optimiser bind mask block and*/
            if (parent.resetState == mask) {
                parent.resetState = 0;
                /*Optimiser blockEnd and*/
                for (MultiPartCIn cIn : cIns) {
                    cIn.reset();
                }
                /*Optimiser block and*/
            } else {
                /*Optimiser bind nMask*/
                parent.resetState &= nMask;
            }
            /*Optimiser blockEnd nr line o block r*/
        } else//
        {
            /*Optimiser bind mask*/
            parent.resetState |= mask;
        }
        /*Optimiser blockEnd and blockEnd r*/
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o block r*/
        if (reverse) {
            /*Optimiser bind mask block and*/
            if (parent.resetState == mask) {
                parent.resetState = 0;
                /*Optimiser blockEnd and*/
                for (MultiPartCIn cIn : cIns) {
                    cIn.reset();
                }
                /*Optimiser block and*/
            } else {
                /*Optimiser bind nMask*/
                parent.resetState &= nMask;
            }
            /*Optimiser blockEnd r block nr line o*/
        } else//
        {
            /*Optimiser bind mask*/
            parent.resetState |= mask;
        }

        /*Optimiser blockEnd and blockEnd nr*/
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiPartRIn> optimiser = new ClassOptimiser<>(this).cut("o").unroll(parent.cIns.length);
        if (parent.rIns.size() == 1) {
            optimiser.cut("and");
        } else {
            optimiser.bind("mask", mask).bind("nMask", nMask);
        }
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        MultiPartRIn build = optimiser.build();
        build.withState = source == null;
        parent.rIns.put(id, build);
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
