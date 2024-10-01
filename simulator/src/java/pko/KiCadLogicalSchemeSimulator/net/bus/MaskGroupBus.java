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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MaskGroupBus extends OutBus {
    public long queueState;
    protected long maskState;

    public MaskGroupBus(OutBus source, long mask, String variantId) {
        super(source, variantId + ":mask" + mask);
        this.mask = mask;
        triState = source.triState;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public MaskGroupBus(OutBus source, String variantId) {
        super(source, variantId);
        triState = source.triState;
    }

    public void addDestination(Bus bus) {
        bus.triState = triState;
        destinations = Utils.addToArray(destinations, bus);
    }

    @Override
    public void setState(long newState) {
        /*Optimiser block setters block iSetter*/
        hiImpedance = false;
        /*Optimiser blockend iSetter*/
        state = newState;
        /*Optimiser blockend setters*/
        /*Optimiser bind m:mask*/
        final long newMaskState = newState & mask;
        if (
            /*Optimiser block iSetter bind d:destinations[0]*/
                destinations[0].hiImpedance ||
                        /*Optimiser blockend iSetter*///
                        maskState != newMaskState) {
            /*Optimiser block setters*/
            if (processing) {
                /*Optimiser block recurse*/
                if (hasQueue) {
                    /*Optimiser blockend recurse*/
                    if (recurseError()) {
                        return;
                    }
                    /*Optimiser block recurse*/
                }
                hasQueue = true;
                queueState = newMaskState;
                /*Optimiser blockend recurse*/
            } else {
                processing = true;
                /*Optimiser blockend setters*/
                maskState = newMaskState;
                for (Bus destination : destinations) {
                    destination.setState(maskState);
                }
                /*Optimiser block setters block recurse*/
                while (hasQueue) {
                    hasQueue = false;
                    /*Optimiser block iSetter*/
                    if (hiImpedance) {
                        for (Bus destination : destinations) {
                            destination.setHiImpedance();
                        }
                    } else {
                        /*Optimiser blockend iSetter*/
                        for (Bus destination : destinations) {
                            destination.setState(queueState);
                        }
                        /*Optimiser block iSetter*/
                    }
                    /*Optimiser blockend iSetter*/
                }
                /*Optimiser blockend recurse*/
                processing = false;
            }
            /*Optimiser blockend setters*/
        }
    }

    /*Optimiser block iSetter*/
    @Override
    public void setHiImpedance() {
        /*Optimiser block setters*/
        hiImpedance = true;
        if (processing) {
            /*Optimiser block recurse*/
            if (hasQueue) {
                /*Optimiser blockend recurse*/
                if (recurseError()) {
                    return;
                }
                /*Optimiser block recurse*/
            }
            hasQueue = true;
            /*Optimiser blockend recurse*/
        } else {
            processing = true;
            /*Optimiser blockend setters*/
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
                        destination.setState(queueState);
                    }
                }
            }
            /*Optimiser blockend recurse*/
            processing = false;
        }
        /*Optimiser blockend setters*/
    }
    /*Optimiser blockend iSetter*/

    @Override
    public Bus getOptimised(boolean keepSetters) {
        if (destinations.length == 0) {
            throw new RuntimeException("unconnected MaskGroupBus " + getName());
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(false);
            }
            ClassOptimiser<MaskGroupBus> optimiser = new ClassOptimiser<>(this).unroll(destinations.length).bind("m", mask).bind("d", "destination0");
            if (!keepSetters) {
                optimiser.cut("setters");
            }
            if (!triState) {
                optimiser.cut("iSetter");
            }
            return optimiser.build();
        }
    }
}
