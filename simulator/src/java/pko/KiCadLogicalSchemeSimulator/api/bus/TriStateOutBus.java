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
package pko.KiCadLogicalSchemeSimulator.api.bus;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.bus.NCBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class TriStateOutBus extends OutBus {
    public TriStateOutBus(String id, SchemaPart parent, int size, String... names) {
        super(id, parent, size, names);
        hiImpedance = true;
        triState = true;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public TriStateOutBus(TriStateOutBus oldBus, String variantId) {
        super(oldBus, variantId);
        hiImpedance = oldBus.hiImpedance;
        triState = oldBus.triState;
    }

    @Override
    public void setState(long newState) {
        hiImpedance = false;
        state = newState;
        /*Optimiser block allRecurse*/
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
            /*Optimiser blockEnd allRecurse*/
            for (Bus destination : destinations) {
                destination.setState(newState);
            }
            /*Optimiser block recurse block allRecurse*/
            while (hasQueue) {
                hasQueue = false;
                if (hiImpedance) {
                    for (Bus destination : destinations) {
                        destination.setHiImpedance();
                    }
                } else {
                    for (Bus destination : destinations) {
                        destination.setState(state);
                    }
                }
            }
            /*Optimiser blockEnd recurse*/
            processing = false;
        }
        /*Optimiser blockEnd allRecurse*/
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this;
        hiImpedance = true;
        /*Optimiser block allRecurse*/
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
            /*Optimiser blockEnd allRecurse*/
            for (Bus destination : destinations) {
                destination.setHiImpedance();
            }
            /*Optimiser block recurse block allRecurse*/
            while (hasQueue) {
                hasQueue = false;
                if (hiImpedance) {
                    for (Bus destination : destinations) {
                        destination.setHiImpedance();
                    }
                } else {
                    for (Bus destination : destinations) {
                        destination.setState(state);
                    }
                }
            }
            /*Optimiser blockEnd recurse*/
            processing = false;
        }
        /*Optimiser blockEnd allRecurse*/
    }

    @Override
    public Bus copyState(IModelItem<Bus> oldBus) {
        super.copyState(oldBus);
        hiImpedance = oldBus.isHiImpedance();
        return this;
    }

    @Override
    public Bus getOptimised(boolean keepSetters) {
        if (destinations.length == 0) {
            return new NCBus(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised(true).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(false);
            }
            ClassOptimiser<TriStateOutBus> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
            if (Simulator.noRecursive) {
                optimiser.cut("allRecurse");
            } else if (!Simulator.recursive && Utils.notContain(Simulator.recursiveOuts, getName())) {
                optimiser.cut("recurse");
            }
            return optimiser.build();
        }
    }
}
