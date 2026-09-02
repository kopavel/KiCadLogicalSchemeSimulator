/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.bus;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.SupportMask;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

public class MaskGroupBus extends OutBus {
    public int queueState;
    protected int maskState;

    public MaskGroupBus(OutBus source, int mask, String variantId) {
        super(source, variantId + ":mask" + mask);
        this.mask = mask;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public MaskGroupBus(OutBus source, String variantId) {
        super(source, variantId);
    }

    public void addDestination(Bus bus) {
        bus.used = true;
        bus.state = state & mask;
        bus.hiImpedance = hiImpedance;
        used = true;
        destinations = Utils.addToArray(destinations, bus);
    }

    @Override
    public void setState(int newState) {
        /*Optimiser line setter*/
        state = newState;
        /*Optimiser bind m:mask*/
        int newMaskState = newState & mask;
        if (maskState != newMaskState
                /*Optimiser line ts*///
                || hiImpedance //
        ) {
            /*Optimiser line ts*/
            hiImpedance = false;
            maskState = newMaskState;
            /*Optimiser blockEnd mask block ar*/
            switch (processing++) {
                case 0: {
                    /*Optimiser blockEnd ar */
                    for (Bus destination : destinations) {
                        destination.setState(newMaskState);
                    }
                    /*Optimiser block r block ar*/
                    while (--processing > 0) {
                        /*Optimiser block ts*/
                        if (hiImpedance) {
                            for (Bus destination : destinations) {
                                destination.setHiImpedance();
                            }
                        } else {
                            /*Optimiser blockEnd ts*/
                            newMaskState = queueState;
                            for (Bus destination : destinations) {
                                destination.setState(newMaskState);
                            }
                            /*Optimiser line ts*/
                        }
                    }
                    /*Optimiser line nr blockEnd r*/
                    processing = 0;
                    return;
                }
                case 1: {
                    queueState = newMaskState;
                    return;
                }
                case 2: {
                    recurseError();
                }
            }
            /*Optimiser blockEnd ar*/
        }
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        hiImpedance = true;
        /*Optimiser block ar*/
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar */
                for (Bus destination : destinations) {
                    destination.setHiImpedance();
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
                    if (hiImpedance) {
                        for (Bus destination : destinations) {
                            destination.setHiImpedance();
                        }
                    } else {
                        for (Bus destination : destinations) {
                            destination.setState(queueState);
                        }
                    }
                }
                /*Optimiser line nr blockEnd r*/
                processing = 0;
                return;
            }
            case 2: {
                recurseError();
            }
        }
        /*Optimiser blockEnd ar blockEnd ts*/
    }

    @Override
    public Bus getOptimised(ModelItem<?> inSource) {
        if (destinations.length == 0) {
            throw new RuntimeException("unconnected MaskGroupBus " + getName());
        } else if (destinations.length == 1 && destinations[0] instanceof SupportMask) {
            destinations[0].applyMask = mask;
            return destinations[0].copyState(this, source).getOptimised(inSource);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(this);
            }
            ClassOptimiser<MaskGroupBus> optimiser = new ClassOptimiser<>(this).unroll(destinations.length).bind("m", mask);
            if (inSource != null) {
                optimiser.cut("setter");
            }
            if (destinations.length < 2 || getRecursionMode() == none) {
                optimiser.cut("ar");
            } else if (getRecursionMode() == warn) {
                optimiser.cut("r");
            } else {
                optimiser.cut("nr");
            }
            if (!isTriState(inSource) || !hasTriStateIn()) {
                optimiser.cut("ts");
                hiImpedance = false;
            }
            MaskGroupBus build = optimiser.build();
            build.withState = inSource == null;
            build.source = inSource;
            for (Bus destination : destinations) {
                destination.source = build;
            }
            return build;
        }
    }
}
