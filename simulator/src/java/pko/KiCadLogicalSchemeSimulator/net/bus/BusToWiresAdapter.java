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
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.FallingEdgePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.RaisingEdgePin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

public class BusToWiresAdapter extends OutBus {
    public Pin[] destinations = new Pin[0];
    public Pin[] toImp = new Pin[0];
    public Pin[] toLow = new Pin[0];
    public Pin[] toHi = new Pin[0];
    public long maskState;
    public boolean queueState;

    /* constructor unroll destination:destinations*/
    /*Optimiser constructor unroll low:toLow:l unroll hi:toHi:h unroll imp:toImp:i*/
    public BusToWiresAdapter(BusToWiresAdapter oldBus, String variantId) {
        super(oldBus, variantId);
        triState = oldBus.triState;
    }

    public BusToWiresAdapter(OutBus outBus, long mask) {
        super(outBus, "BusToWire");
        triState = outBus.triState;
        this.mask = mask;
    }

    /*Optimiser block iSetter*/
    @Override
    public void setHiImpedance() {
        /*Optimiser block setters*/
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
            /*Optimiser blockEnd setters blockEnd allRecurse*/
            for (Pin imp : toImp) {
                imp.setHiImpedance();
            }
            /*Optimiser block setters block recurse block allRecurse*/
            while (hasQueue) {
                hasQueue = false;
                if (hiImpedance) {
                    for (Pin imp : toImp) {
                        imp.setHiImpedance();
                    }
                } else {
                    if (queueState) {
                        for (Pin low : toLow) {
                            low.setLo();
                        }
                    } else {
                        for (Pin hi : toHi) {
                            hi.setHi();
                        }
                    }
                }
            }
            /*Optimiser blockEnd recurse*/
            processing = false;
            /*Optimiser blockEnd allRecurse*/
        }
        /*Optimiser blockEnd setters*/
    }

    public void addDestination(Pin pin) {
        pin.used = true;
        destinations = Utils.addToArray(destinations, pin);
        split();
    }
    /*Optimiser blockEnd iSetter*/

    @Override
    public long getState() {
        return (source.getState() & mask) > 0 ? 1 : 0;
    }

    @Override
    public void setState(long newState) {
        /*Optimiser block setters line iSetter*/
        hiImpedance = false;
        state = newState;
        /*Optimiser blockEnd setters block mask*/
        final long newMaskState;
        /*Optimiser bind m:mask*/
        if (maskState != (newMaskState = newState & mask)
                /*Optimiser line iSetter bind d:toImp[0] *///
                || toImp[0].hiImpedance //
        ) {
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
                /*Optimiser bind nState:maskState\s=\snewMaskState*/
                queueState = (maskState = newMaskState) == 0;
                /*Optimiser blockEnd recurse*/
            } else {
                processing = true;
                /*Optimiser blockEnd setters blockEnd allRecurse bind nState:maskState\s=\snewMaskState*/
                if ((maskState = newMaskState) == 0) {
                    for (Pin low : toLow) {
                        low.setLo();
                    }
                    /*Optimiser line toHi*/
                } else {
                    for (Pin hi : toHi) {
                        hi.setHi();
                    }
                }

                /*Optimiser block setters block recurse block allRecurse*/
                while (hasQueue) {
                    hasQueue = false;
                    /*Optimiser block iSetter*/
                    if (hiImpedance) {
                        for (Pin imp : toImp) {
                            imp.setHiImpedance();
                        }
                    } else {
                        /*Optimiser blockEnd iSetter*/
                        if (queueState) {
                            for (Pin low : toLow) {
                                low.setLo();
                            }
                        } else {
                            for (Pin hi : toHi) {
                                hi.setHi();
                            }
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

    @Override
    public BusToWiresAdapter getOptimised(ModelItem<?> source) {
        if (destinations.length == 0) {
            throw new RuntimeException("unconnected BusToWiresAdapter " + getName());
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(this);
            }
            split();
/*
            if (parent.id.equals("U1_A")) {
                toLow[0] = new AsyncInPin(toLow[0]);
            }
*/
            ClassOptimiser<BusToWiresAdapter> optimiser =
                    new ClassOptimiser<>(this).unroll("i", toImp.length).unroll("l", toLow.length).unroll("h", toHi.length).bind("m", mask);
            if (source != null) {
                optimiser.cut("setters");
            }
            if (!triState) {
                optimiser.cut("iSetter");
            } else {
                optimiser.bind("d", "imp0");
            }
            if (destinations.length < 2 || parent.net.parameterResolver.recursionMode == none) {
                optimiser.cut("allRecurse");
            } else if (parent.net.parameterResolver.recursionMode == warn && Utils.notContain(Simulator.recursiveOuts, getName()) &&
                    !parent.recursive.contains(getId())) {
                optimiser.cut("recurse");
            }
            if (applyMask == 0) {
                optimiser.cut("mask").bind("nState", "newState");
            }
            if (toHi.length == 0) {
                optimiser.cut("toHi");
            }
            BusToWiresAdapter build = optimiser.build();
            build.source = source;
            for (Pin destination : destinations) {
                destination.source = build;
            }
            return build;
        }
    }

    @Override
    public boolean useFullOptimiser() {
        return true;
    }

    protected void split() {
        toHi = new Pin[0];
        toLow = new Pin[0];
        toImp = new Pin[0];
        for (Pin destination : destinations) {
            if (!(destination instanceof FallingEdgePin)) {
                toHi = Utils.addToArray(toHi, destination);
                if (!(destination instanceof RaisingEdgePin)) {
                    toImp = Utils.addToArray(toImp, destination);
                }
            }
            if (!(destination instanceof RaisingEdgePin)) {
                toLow = Utils.addToArray(toLow, destination);
            }
        }
    }
}
