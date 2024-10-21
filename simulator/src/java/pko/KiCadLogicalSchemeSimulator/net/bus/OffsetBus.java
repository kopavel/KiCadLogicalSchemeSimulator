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
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class OffsetBus extends OutBus {
    protected final byte offset;
    public long maskState;

    public OffsetBus(OutBus outBus, Bus destination, byte offset) {
        super(outBus, "offset" + offset);
        triState = outBus.triState;
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
        triState = oldBus.triState;
        destinations = oldBus.destinations;
    }

    @Override
    public void setState(long newState) {
        /*Optimiser block setters line iSetter*/
        hiImpedance = false;
        state = newState;
        /*Optimiser blockEnd setters block mask bind m:mask*/
        final long newMaskState = newState & mask;
        if (
            /*Optimiser line iSetter bind d:destinations[0]*/
                destinations[0].hiImpedance ||//
                        maskState != newMaskState) {
            maskState = newMaskState;
            /*Optimiser blockEnd mask block setters block allRecurse*/
            if (processing) {
                /*Optimiser line recurse*/
                if (hasQueue) {
                    if (recurseError()) {
                        return;
                    }
                    /*Optimiser block recurse*/
                }
                hasQueue = true;
                /*Optimiser blockEnd recurse*/
            } else {
                processing = true;
                /*Optimiser blockEnd setters blockEnd allRecurse*/
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
                /*Optimiser block setters block recurse block allRecurse*/
                while (hasQueue) {
                    hasQueue = false;
                    /*Optimiser block iSetter*/
                    if (hiImpedance) {
                        for (Bus destination : destinations) {
                            destination.setHiImpedance();
                        }
                    } else {
                        /*Optimiser blockEnd iSetter*/
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
                        /*Optimiser line iSetter*/
                    }
                }
                /*Optimiser blockEnd recurse*/
                processing = false;
            }
            /*Optimiser line mask blockEnd setters blockEnd allRecurse*/
        }
    }

    /*Optimiser block iSetter*/
    @Override
    public void setHiImpedance() {
        /*Optimiser block setters*/
        hiImpedance = true;
        if (processing) {
            /*Optimiser line recurse*/
            if (hasQueue) {
                if (recurseError()) {
                    return;
                }
                /*Optimiser block recurse*/
            }
            hasQueue = true;
            /*Optimiser blockEnd recurse*/
        } else {
            processing = true;
            /*Optimiser blockEnd setters*/
            for (Bus destination : destinations) {
                destination.setHiImpedance();
            }
            /*Optimiser block setters block recurse*/
            while (hasQueue) {
                hasQueue = false;
                if (hiImpedance) {
                    for (Bus destination : destinations) {
                        destination.setHiImpedance();
                    }
                } else {
                    for (Bus destination : destinations) {
                        /*Optimiser block positive line negative*/
                        if (offset > 0) {
                            /*Optimiser bind o:offset*/
                            destination.setState(state << offset);
                            /*Optimiser block negative*/
                        } else {
                            /*Optimiser blockEnd positive bind o:-offset*/
                            destination.setState(state >> -offset);
                            /*Optimiser line positive*/
                        }
                        /*Optimiser blockEnd negative*/
                    }
                }
            }
            /*Optimiser blockEnd recurse*/
            processing = false;
        }
        /*Optimiser blockEnd setters*/
    }
    /*Optimiser blockEnd iSetter*/

    public void addDestination(Bus item) {
        item.used = true;
        destinations = Utils.addToArray(destinations, item);
    }

    @Override
    public Bus getOptimised(ModelItem<?> source) {
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
        if (source != null) {
            optimiser.cut("setters");
        }
        if (!triState) {
            optimiser.cut("iSetter");
        } else if (groupByMask != 0) {
            optimiser.bind("d", "destination0");
        }
        if (destinations.length < 2 || Simulator.noRecursive) {
            optimiser.cut("allRecurse");
        } else if (!Simulator.recursive && Utils.notContain(Simulator.recursiveOuts, getName())) {
            optimiser.cut("recurse");
        }
        if (groupByMask == 0) {
            optimiser.cut("mask").bind("v", "newState");
        } else {
            optimiser.bind("m", groupByMask);
        }
        OffsetBus build = optimiser.build();
        build.source = source;
        for (Bus destination : destinations) {
            destination.source = build;
        }
        return build;
    }

    @Override
    public boolean useFullOptimiser() {
        return true;
    }
}
