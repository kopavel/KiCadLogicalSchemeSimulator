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
package pko.KiCadLogicalSchemeSimulator.net.merger.bus;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

//ToDo  there no need for merger with "non tri-sate” inputs.
//Todo with one Bus input and only weak thers - use simpler "Weak bus" implementation
public class BusMergerBusIn extends InBus implements MergerInput<Bus> {
    @Getter
    public long mask;
    public long nMask;
    public Bus[] destinations;
    public BusMerger merger;

    public BusMergerBusIn(Bus source, long mask, BusMerger merger) {
        super(source, "BMergeBIn");
        this.mask = mask;
        this.merger = merger;
        nMask = ~mask;
        destinations = merger.destinations;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public BusMergerBusIn(BusMergerBusIn oldPin, String variantId) {
        super(oldPin, variantId);
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        merger = oldPin.merger;
        destinations = merger.destinations;
        triState = oldPin.triState;
    }

    @Override
    public void setState(long newState) {
        assert Log.debug(this.getClass(),
                "Bus merger change. before: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        /*Optimiser block setters*/
        state = newState;
        /*Optimiser blockend setters*/
        /*Optimiser block sameMask*/
        /*Optimiser block otherMask*/
        if (mask == merger.mask) {
            /*Optimiser blockend otherMask*/
            if (hiImpedance) {
                if (merger.strongPins != 0) {
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        hiImpedance = false;
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser bind mask*/
                merger.strongPins = mask;
            }
            hiImpedance = false;
            if (newState != merger.state) {
                merger.state = newState;
                for (Bus destination : destinations) {
                    destination.setState(newState);
                }
            }
            /*Optimiser block otherMask*/
        } else {
            /*Optimiser blockend sameMask*/
            long oldState = merger.state;
            if (hiImpedance) {
                /*Optimiser bind mask*/
                if ((merger.strongPins & mask) != 0) {
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        hiImpedance = false;
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser bind mask*/
                merger.strongPins |= mask;
            }
            hiImpedance = false;
            /*Optimiser bind nMask*/
            merger.state &= nMask;
            merger.state |= newState;
            if (oldState != merger.state) {
                for (Bus destination : destinations) {
                    destination.setState(merger.state);
                }
            }
            /*Optimiser block sameMask*/
        }
        /*Optimiser blockend sameMask*/
        /*Optimiser blockend otherMask*/
        assert Log.debug(this.getClass(),
                "Bus merger change. after: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
    }

    @Override
    public void setHiImpedance() {
        assert Log.debug(this.getClass(),
                "Bus merger setImpedance. before: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        if (hiImpedance) {
            return;
        }
        /*Optimiser block sameMask*/
        /*Optimiser block otherMask*/
        if (mask == merger.mask) {
            /*Optimiser blockend otherMask*/
            merger.strongPins = 0;
            if (merger.weakState != merger.state) {
                merger.state = merger.weakState;
                for (Bus destination : destinations) {
                    destination.setState(merger.state);
                }
            }
            /*Optimiser block otherMask*/
        } else {
            /*Optimiser blockend sameMask*/
            long oldState = merger.state;
            /*Optimiser bind nMask*/
            merger.strongPins &= nMask;
            /*Optimiser bind nMask*/
            merger.state &= nMask;
            /*Optimiser bind mask*/
            merger.state |= merger.weakState & mask;
            if (oldState != merger.state) {
                for (Bus destination : destinations) {
                    destination.setState(merger.state);
                }
            }
            /*Optimiser block sameMask*/
        }
        /*Optimiser blockend sameMask*/
        /*Optimiser blockend otherMask*/
        hiImpedance = true;
        assert Log.debug(this.getClass(),
                "Bus merger setImpedance. after: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else {
            setHiImpedance();
        }
    }

    @Override
    public BusMergerBusIn getOptimised(boolean keepSetters) {
        //ToDo in case of "no passive pin" weakPins/weakState are known after build phase (incomplete)
        merger.sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(false);
            if (triState) {
                destinations[i].triState = true;
            }
        }
        ClassOptimiser<BusMergerBusIn> optimiser = new ClassOptimiser<>(this).unroll(merger.destinations.length).bind("mask", mask);
        if (mask == merger.mask) {
            optimiser.cut("otherMask");
        } else {
            optimiser.cut("sameMask").bind("nMask", nMask);
        }
        if (!keepSetters) {
            optimiser.cut("setters");
        }
        BusMergerBusIn optimised = optimiser.build();
        merger.sources.add(optimised);
        return optimised;
    }
}
