/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.counter.multipart;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.RaisingEdgePin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiPartCRaisingIn extends RaisingEdgePin implements MultiPartCIn {
    public final int countMask;
    public final int partNo;
    public final MultiPartCounter parent;
    public final int size;
    public final int skipMask;
    public Bus outBus;
    public Pin outPin;

    public MultiPartCRaisingIn(String id, MultiPartCounter parent, int size, int partNo, int skipMask) {
        super(id, parent);
        this.parent = parent;
        this.size = size;
        this.skipMask = skipMask;
        if (size == 1) {
            outPin = parent.getOutPin("Q" + (char) ('a' + partNo));
        } else {
            outBus = parent.getOutBus("Q" + (char) ('a' + partNo));
        }
        countMask = Utils.getMaskForSize(size);
        this.partNo = partNo;
    }

    @SuppressWarnings("unused")
    /*Optimiser constructor*///
    public MultiPartCRaisingIn(MultiPartCRaisingIn oldPin, String variantId) {
        super(oldPin, variantId);
        countMask = oldPin.countMask;
        outBus = oldPin.outBus;
        partNo = oldPin.partNo;
        size = oldPin.size;
        skipMask = oldPin.skipMask;
        parent = oldPin.parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line hasR*/
        if (parent.resetState != 0) {
            /*Optimiser line o*/
            if (size == 1) {
                /*Optimiser line pin*/
                if (outPin.state) {
                    outPin.setLo();
                } else {
                    outPin.setHi();
                }
                /*Optimiser line o*/
            } else if (skipMask != 0) {
                /*Optimiser line skip bind skip:skipMask*/
                outBus.setState(outBus.state + (((outBus.state & skipMask) == skipMask) ? 2 : 1));
                /*Optimiser line o*/
            } else {
                /*Optimiser line bus bind countMask*/
                outBus.setState((outBus.state + 1) & countMask);
                /*Optimiser line o*/
            }
            /*Optimiser line nasR*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
    }

    @Override
    public void reset() {
        if (size == 1) {
            outPin.setLo();
        } else {
            outBus.setState(0);
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiPartCRaisingIn> optimiser = new ClassOptimiser<>(this).cut("o");
        if (size == 1) {
            optimiser.cut("bus").cut("skip");
        } else if (skipMask != 0) {
            optimiser.cut("bus").cut("pin").bind("skip", skipMask);
        } else {
            optimiser.bind("countMask", countMask).cut("pin").cut("skip");
        }
        if (parent.rIns.values()
                .stream()
                .noneMatch(p -> p.used)) {
            optimiser.cut("hasR");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        MultiPartCRaisingIn build = optimiser.build();
        build.withState = source == null;
        parent.cIns[partNo] = build;
        parent.inPins.put(id, build);
        for (MultiPartRIn rPin : parent.rIns.values()) {
            rPin.cIns[partNo] = build;
        }
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }

    @Override
    public void setOut(Pin outPin) {
        this.outPin = outPin;
    }

    @Override
    public void setOut(Bus outBus) {
        this.outBus = outBus;
    }
}
