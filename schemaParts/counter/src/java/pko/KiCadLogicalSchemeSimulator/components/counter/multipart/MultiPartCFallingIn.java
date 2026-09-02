/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.counter.multipart;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.FallingEdgePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiPartCFallingIn extends FallingEdgePin implements MultiPartCIn {
    public final int countMask;
    public final int partNo;
    public final MultiPartCounter parent;
    public final int size;
    public final int skipMask;
    public Bus outBus;
    public Pin outPin;

    public MultiPartCFallingIn(String id, MultiPartCounter parent, int size, int partNo, int skipMask) {
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
    public MultiPartCFallingIn(MultiPartCFallingIn oldPin, String variantId) {
        super(oldPin, variantId);
        countMask = oldPin.countMask;
        outBus = oldPin.outBus;
        outPin = oldPin.outPin;
        partNo = oldPin.partNo;
        size = oldPin.size;
        skipMask = oldPin.skipMask;
        parent = oldPin.parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
    }

    @Override
    public void setLo() {
        int lState;
        Pin lPin;
        Bus bus;
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line hasR*/
        if (parent.resetState != 0) {
            /*Optimiser line o*/
            if (size == 1) {
                /*Optimiser block pin*/
                if ((lPin = outPin).state) {
                    lPin.setLo();
                } else {
                    lPin.setHi();
                }
                /*Optimiser line o blockEnd pin*/
            } else if (skipMask != 0) {
                /*Optimiser line skip bind skip:skipMask bind countMask*///
                (bus = outBus).setState(((lState = bus.state) + (((lState & skipMask) == skipMask) ? 2 : 1)) & countMask);
                /*Optimiser line o*/
            } else {
                /*Optimiser line bus bind countMask*///
                (bus = outBus).setState((lState = bus.state) == countMask ? 0 : lState + 1);
                /*Optimiser line o*/
            }
            /*Optimiser line hasR*/
        }
    }

    @Override
    public void reset() {
        if (size == 1) {
            if (outPin.state) {
                outPin.setLo();
            }
        } else {
            if (outBus.state > 0) {
                outBus.setState(0);
            }
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiPartCFallingIn> optimiser = new ClassOptimiser<>(this).cut("o");
        if (size == 1) {
            optimiser.cut("bus").cut("skip");
        } else if (skipMask != 0) {
            optimiser.cut("bus").cut("pin").bind("skip", skipMask).bind("countMask", countMask);
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
        MultiPartCFallingIn build = optimiser.build();
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
