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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Arrays;

public class BusToWiresAdapter extends OutBus {
    protected final long fMask;
    public Pin[] destinations = new Pin[0];
    public long maskState;
    public boolean queueState;

    /*Optimiser constructor unroll destination:destinations*/
    public BusToWiresAdapter(BusToWiresAdapter oldBus, String variantId) {
        super(oldBus, variantId);
        triState = oldBus.triState;
        fMask = oldBus.mask;
    }

    public BusToWiresAdapter(OutBus outBus, Bus[] wires, long mask) {
        super(outBus, "BusToWire");
        triState = outBus.triState;
        this.mask = mask;
        fMask = mask;
        destinations = Arrays.stream(wires)
                .map(w -> ((SimpleBusToWireAdapter) w).destination).toArray(Pin[]::new);
    }

    @Override
    public void setState(long newState) {
        /*Optimiser block setters block iSetter*/
        hiImpedance = false;
        /*Optimiser blockend iSetter*/
        state = newState;
        /*Optimiser blockend setters*/
        final long newMaskState = newState & fMask;
        if (
            /*Optimiser block iSetter*/
            /*Optimiser bind d:destinations[0]*/
                destinations[0].hiImpedance ||
                        /*Optimiser blockend iSetter*///
                        maskState != newMaskState) {
            maskState = newMaskState;
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
                queueState = newMaskState != 0;
                /*Optimiser blockend recurse*/
            } else {
                processing = true;
                /*Optimiser blockend setters*/
                /*Optimiser block dest*/
                final boolean dState = newMaskState != 0;
                /*Optimiser blockend dest*/
                for (Pin destination : destinations) {
                    /*Optimiser bind v:dState*/
                    destination.setState(dState);
                }
                /*Optimiser block setters block recurse*/
                while (hasQueue) {
                    hasQueue = false;
                    /*Optimiser block iSetter*/
                    if (hiImpedance) {
                        for (Pin destination : destinations) {
                            destination.setHiImpedance();
                        }
                    } else {
                        /*Optimiser blockend iSetter*/
                        for (Pin destination : destinations) {
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
            for (Pin destination : destinations) {
                destination.setHiImpedance();
            }
            /*Optimiser block setters block recurse*/
            while (hasQueue) {
                hasQueue = false;
                if (hiImpedance) {
                    for (Pin destination : destinations) {
                        destination.setHiImpedance();
                    }
                } else {
                    for (Pin destination : destinations) {
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

    public void addDestination(Pin pin) {
        destinations = Utils.addToArray(destinations, pin);
    }

    @Override
    public BusToWiresAdapter getOptimised(boolean keepSetters) {
        if (destinations.length == 0) {
            throw new RuntimeException("unconnected BusToWiresAdapter " + getName());
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(false);
            }
            ClassOptimiser<BusToWiresAdapter> optimiser = new ClassOptimiser<>(this).unroll(destinations.length).bind("mask", "fMask").bind("d", "destination0");
            if (destinations.length == 1) {
                optimiser.bind("v", "newMaskState != 0").cut("dest");
            }
            if (!keepSetters) {
                optimiser.cut("setters");
            }
            if (!triState) {
                optimiser.cut("iSetter");
            }
            if (!Simulator.recursive && Utils.notContain(Simulator.recursiveOuts, getName())) {
                optimiser.cut("recurse");
            }
            return optimiser.build();
        }
    }
}
