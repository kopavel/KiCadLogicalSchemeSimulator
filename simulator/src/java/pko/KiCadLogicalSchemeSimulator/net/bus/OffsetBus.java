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
import pko.KiCadLogicalSchemeSimulator.api.SupportOffset;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

//Fixme if we offset "Down" - we can use no mask for "lower bit"
public class OffsetBus extends OutBus implements SupportOffset, SupportMask {
    protected final byte offset;
    public int maskState;
    public boolean queueImpedance;

    public OffsetBus(OutBus outBus, Bus destination, byte offset) {
        super(outBus, "offset" + offset);
        triStateIn = destination.triStateIn;
        if (offset == 0) {
            throw new RuntimeException("Offset must not be 0");
        }
        destinations = new Bus[]{destination};
        this.offset = offset;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public OffsetBus(OffsetBus oldBus, String variantId) {
        super(oldBus, variantId);
        offset = oldBus.offset;
        destinations = oldBus.destinations;
    }

    public void addDestination(Bus item) {
        item.used = true;
        item.state = state & maskState;
        if (offset > 0) {
            item.state = item.state << offset;
        } else if (offset < 0) {
            item.state = item.state >> -offset;
        }
        triStateIn = triStateIn || item.triStateIn;
        destinations = Utils.addToArray(destinations, item);
    }

    @Override
    public void setState(int newState) {
        /*Optimiser line setter*/
        state = newState;
        /*Optimiser block mask bind m:applyMask*/
        int newMaskState = newState & applyMask;
        if (maskState != newMaskState
                /*Optimiser line ts*///
                || hiImpedance //
        ) {
            /*Optimiser line ts*/
            hiImpedance = false;
            maskState = newMaskState;
            /*Optimiser blockEnd mask block ar */
            switch (processing++) {
                case 0: {
                    /*Optimiser blockEnd ar*/
                    for (Bus destination : destinations) {
                        /*Optimiser block positive line negative*/
                        if (offset > 0) {
                            /*Optimiser bind o:offset bind v:newMaskState*/
                            destination.setState(newMaskState << offset);
                            /*Optimiser block negative*/
                        } else {
                            /*Optimiser blockEnd positive bind o:-offset bind v:newMaskState*/
                            destination.setState(newMaskState >> -offset);
                            /*Optimiser line positive*/
                        }
                        /*Optimiser blockEnd negative*/
                    }
                    /*Optimiser block r block ar*/
                    while (--processing > 0) {
                        /*Optimiser block ts*/
                        if (queueImpedance) {
                            for (Bus destination : destinations) {
                                destination.setHiImpedance();
                            }
                        } else {
                            /*Optimiser blockEnd ts*/
                            newMaskState=maskState;
                            for (Bus destination : destinations) {
                                /*Optimiser block positive line negative*/
                                if (offset > 0) {
                                    /*Optimiser bind o:offset*/
                                    destination.setState(newMaskState << offset);
                                    /*Optimiser block negative*/
                                } else {
                                    /*Optimiser blockEnd positive bind o:-offset*/
                                    destination.setState(newMaskState >> -offset);
                                    /*Optimiser line positive*/
                                }
                                /*Optimiser blockEnd negative*/
                            }
                            /*Optimiser line ts*/
                        }
                    }
                    /*Optimiser line nr blockEnd r*/
                    processing = 0;
                    return;
                }
                /*Optimiser block ts*/
                case 1: {
                    queueImpedance = false;
                    return;
                }
                /*Optimiser blockEnd ts*/
                case 2: {
                    recurseError();
                }
            }
            /*Optimiser blockEnd ar line mask*/
        }
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts line setter*/
        hiImpedance = true;
        /*Optimiser block ar */
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar*/
                for (Bus destination : destinations) {
                    destination.setHiImpedance();
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
                    if (queueImpedance) {
                        for (Bus destination : destinations) {
                            destination.setHiImpedance();
                        }
                    } else {
                        for (Bus destination : destinations) {
                            /*Optimiser block positive line negative*/
                            if (offset > 0) {
                                /*Optimiser bind o:offset*/
                                destination.setState(maskState << offset);
                                /*Optimiser block negative*/
                            } else {
                                /*Optimiser blockEnd positive bind o:-offset*/
                                destination.setState(maskState >> -offset);
                                /*Optimiser line positive*/
                            }
                            /*Optimiser blockEnd negative*/
                        }
                    }
                }
                /*Optimiser line nr blockEnd r*/
                processing = 0;
                return;
            }
            case 1: {
                queueImpedance = true;
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
        if (destinations.length == 1 && destinations[0] instanceof SupportOffset && (applyMask == 0 || destinations[0] instanceof SupportMask)) {
            destinations[0].applyMask = applyMask;
            destinations[0].applyOffset = offset;
            return destinations[0].getOptimised(inSource).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(this);
            }
            ClassOptimiser<OffsetBus> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
            if (offset > 0) {
                optimiser.bind("o", offset);
                optimiser.cut("negative");
            } else {
                optimiser.bind("o", -offset);
                optimiser.cut("positive");
            }
            if (inSource != null) {
                optimiser.cut("setter");
            }
            if (!isTriState(inSource)) {
                optimiser.cut("ts");
            } else if (applyMask != 0) {
                optimiser.bind("d", "destination0");
            }
            if (destinations.length < 2 || getRecursionMode() == none) {
                optimiser.cut("ar");
            } else if (getRecursionMode() == warn) {
                optimiser.cut("r");
            } else {
                optimiser.cut("nr");
            }
            if (applyMask == 0) {
                optimiser.cut("mask").bind("v", "newState");
            } else {
                optimiser.bind("m", applyMask);
            }
            OffsetBus build = optimiser.build();
            build.source = inSource;
            for (Bus destination : destinations) {
                destination.source = build;
            }
            return build;
        }
    }
}
