/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.bus;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class BusInInterconnect extends InBus {
    public final int interconnectMask;
    public final int senseMask;
    public InBus destination;

    public BusInInterconnect(InBus destination, int interconnectMask, Byte offset) {
        super(destination, "interconnect" + interconnectMask);
        this.destination = destination;
        used = true;
        this.interconnectMask = interconnectMask;
        senseMask = 1 << offset;
    }

    /*Optimiser constructor*/
    public BusInInterconnect(BusInInterconnect oldBus, String variantId) {
        super(oldBus, variantId);
        interconnectMask = oldBus.interconnectMask;
        senseMask = oldBus.senseMask;
        destination = oldBus.destination;
    }

    @Override
    public void setState(int newState) {
        /*Optimiser block setter line ts*/
        hiImpedance = false;
        state = newState;
        /*Optimiser blockEnd setter bind m:interconnectMask*/
        if ((newState & interconnectMask) != 0) {
            /*Optimiser bind m:interconnectMask*/
            destination.setState(newState | interconnectMask);
        } else {
            destination.setState(newState);
        }
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts line setter*/
        hiImpedance = true;
        destination.setHiImpedance();
        /*Optimiser blockEnd ts*/
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        destination = destination.getOptimised(this);
        ClassOptimiser<BusInInterconnect> optimiser = new ClassOptimiser<>(this).bind("m", interconnectMask);
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!isTriState(source) || !hasTriStateIn()) {
            optimiser.cut("ts");
            hiImpedance = false;
        }
        InBus build = optimiser.build();
        build.source = source;
        build.withState = source == null;
        destination.source = build;
        return build;
    }
}
