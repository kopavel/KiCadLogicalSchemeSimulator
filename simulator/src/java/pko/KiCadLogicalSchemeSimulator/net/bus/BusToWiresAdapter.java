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
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.FallingEdgePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.RaisingEdgePin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

public class BusToWiresAdapter extends OutBus implements SupportMask, SupportOffset {
    public Pin[] pinDestinations = new Pin[0];
    public Pin[] toImp = new Pin[0];
    public Pin[] toLow = new Pin[0];
    public Pin[] toHi = new Pin[0];
    public int maskState;

    @SuppressWarnings("unused")
    /*Optimiser constructor unroll low:toLow:l unroll hi:toHi:h unroll imp:toImp:i*///
    public BusToWiresAdapter(BusToWiresAdapter oldBus, String variantId) {
        super(oldBus, variantId);
    }

    public BusToWiresAdapter(OutBus outBus, int mask) {
        super(outBus, "BusToWire");
        triStateIn = false;
        this.mask = mask;
    }

    public void addDestination(Pin pin) {
        pin.used = true;
        pin.state = (state & mask) > 0;
        used = true;
        triStateIn = triStateIn || pin.triStateIn;
        pinDestinations = Utils.addToArray(pinDestinations, pin);
        priority += pin.priority;
        split();
    }

    @Override
    public int getState() {
        return ((source == null ? state : source.getState()) & mask) > 0 ? 1 : 0;
    }

    @Override
    public void setState(int newState) {
        /*Optimiser line setter*/
        state = newState;
        /*Optimiser block mask*/
        int newMaskState;
        /*Optimiser bind m:mask*/
        if (maskState != (newMaskState = newState & mask)
                /*Optimiser line ts *///
                || hiImpedance //
        ) {
            /*Optimiser line ts*/
            hiImpedance = false;
            /*Optimiser blockEnd mask block ar*/
            switch (processing++) {
                case 0: {
                    /*Optimiser blockEnd ar bind nState:maskState\s=\snewMaskState bind eq:==*/
                    if ((maskState = newMaskState) == 0) {
                        /*Optimiser block lo*/
                        for (Pin low : toLow) {
                            low.setLo();
                        }
                        /*Optimiser block hi blockEnd lo line lo*/
                    } else {
                        for (Pin hi : toHi) {
                            hi.setHi();
                        }
                        /*Optimiser blockEnd hi*/
                    }
                    /*Optimiser block r block ar*/
                    while (--processing > 0) {
                        /*Optimiser block ts*/
                        if (hiImpedance) {
                            for (Pin imp : toImp) {
                                imp.setHiImpedance();
                            }
                        } else {
                            /*Optimiser blockEnd ts bind eq:==*/
                            if (maskState == 0) {
                                /*Optimiser block lo*/
                                for (Pin low : toLow) {
                                    low.setLo();
                                }
                                /*Optimiser block hi blockEnd lo line lo*/
                            } else {
                                for (Pin hi : toHi) {
                                    hi.setHi();
                                }
                                /*Optimiser blockEnd hi*/
                            }
                            /*Optimiser line ts*/
                        }
                    }
                    /*Optimiser line nr blockEnd r*/
                    processing = 0;
                    return;
                }
                case 1: {
                    /*Optimiser bind nState:newMaskState*/
                    maskState = newMaskState;
                    return;
                }
                case 2: {
                    recurseError();
                }
            }
            /*Optimiser blockEnd ar line mask*/
        }
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        hiImpedance = true;
        /*Optimiser block ar*/
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar*/
                for (Pin imp : toImp) {
                    imp.setHiImpedance();
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
                    if (hiImpedance) {
                        for (Pin imp : toImp) {
                            imp.setHiImpedance();
                        }
                    } else {
                        /*Optimiser bind eq:==*/
                        if (maskState == 0) {
                            /*Optimiser block lo*/
                            for (Pin low : toLow) {
                                low.setLo();
                            }
                            /*Optimiser block hi blockEnd lo line lo*/
                        } else {
                            for (Pin hi : toHi) {
                                hi.setHi();
                            }
                            /*Optimiser blockEnd hi*/
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
    public BusToWiresAdapter getOptimised(ModelItem<?> inSource) {
        if (pinDestinations.length == 0) {
            throw new RuntimeException("unconnected BusToWiresAdapter " + getName());
        } else {
            for (int i = 0; i < pinDestinations.length; i++) {
                pinDestinations[i] = pinDestinations[i].getOptimised(this);
            }
            split();
/*
            if (parent.id.equals("U1_A")) {
                toLow[0] = new AsyncInPin(toLow[0]);
            }
*/
            ClassOptimiser<BusToWiresAdapter> optimiser = new ClassOptimiser<>(this).unroll("l", toLow.length).unroll("h", toHi.length);
            if (inSource != null) {
                optimiser.cut("setter");
            }
            if (isTriState(inSource)) {
                optimiser.unroll("i", toImp.length);
            } else {
                optimiser.cut("ts");
            }
            if (pinDestinations.length < 2 || getRecursionMode() == none) {
                optimiser.cut("ar");
            } else if (getRecursionMode() == warn) {
                optimiser.cut("r");
            } else {
                optimiser.cut("nr");
            }
            if (applyMask == 0) {
                optimiser.cut("mask").bind("nState", "newState");
            } else {
                optimiser.bind("m", mask);
            }
            if (toHi.length == 0) {
                optimiser.cut("hi");
            } else if (toLow.length == 0) {
                optimiser.cut("lo").bind("eq", "!==");
            }
            BusToWiresAdapter build = optimiser.build();
            build.source = inSource;
            for (Pin destination : pinDestinations) {
                destination.source = build;
            }
            return build;
        }
    }

    protected void split() {
        toHi = new Pin[0];
        toLow = new Pin[0];
        toImp = new Pin[0];
        for (Pin destination : pinDestinations) {
            //noinspection ChainOfInstanceofChecks
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
