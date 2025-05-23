/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
        triStateIn = false;
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
        triStateIn = triStateIn || bus.triStateIn;
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
                            newMaskState=queueState;
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
            return destinations[0].getOptimised(inSource).copyState(this);
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
            if (!isTriState(inSource)) {
                optimiser.cut("ts");
            }
            MaskGroupBus build = optimiser.build();
            build.source = inSource;
            for (Bus destination : destinations) {
                destination.source = build;
            }
            return build;
        }
    }
}
