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

//Todo with one Bus input and only weak others - use simpler "Weak bus" implementation
public class BusMergerBusIn extends InBus implements MergerInput<Bus> {
    @Getter
    public final long mask;
    public final long nMask;
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
        mask = oldPin.merger.mask;
        nMask = ~oldPin.merger.mask;
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
            /*Optimiser block iSetter blockend otherMask*/
            if (hiImpedance) {
                hiImpedance = false;
                if (merger.strongPins != 0) {
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(merger.sources);
                    }
                }
                merger.strongPins = mask;
            }
            /*Optimiser blockend iSetter*/
            if (newState != merger.state) {
                merger.state = newState;
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
                    /*Optimiser blockend recurse*/
                } else {
                    processing = true;
                    /*Optimiser blockend setters*/
                    for (Bus destination : destinations) {
                        destination.setState(newState);
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
                                destination.setState(state);
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
            /*Optimiser block otherMask*/
        } else {
            /*Optimiser blockend sameMask*/
            long mergerState = merger.state;
            /*Optimiser block iSetter*/
            if (hiImpedance) {
                hiImpedance = false;
                if ((merger.strongPins & mask) != 0) {
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(merger.sources);
                    }
                }
                merger.strongPins |= mask;
            }
            /*Optimiser blockend iSetter*/
            mergerState &= nMask;
            mergerState |= newState & mask;
            if (mergerState != merger.state) {
                merger.state = mergerState;
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
                    /*Optimiser blockend recurse*/
                } else {
                    processing = true;
                    /*Optimiser blockend setters*/
                    for (Bus destination : destinations) {
                        destination.setState(mergerState);
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
                                destination.setState(merger.state);
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

    /*Optimiser block iSetter*/
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
                    /*Optimiser blockend recurse*/
                } else {
                    processing = true;
                    /*Optimiser blockend setters*/
                    for (Bus destination : destinations) {
                        destination.setState(merger.state);
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
                                destination.setState(merger.state);
                            }
                        }
                    }
                    /*Optimiser blockend recurse*/
                    processing = false;
                }
                /*Optimiser blockend setters*/
            }
            /*Optimiser block otherMask*/
        } else {
            /*Optimiser blockend sameMask*/
            long mergerState = merger.state;
            merger.strongPins &= nMask;
            mergerState &= nMask;
            mergerState |= merger.weakState & mask;
            if (mergerState != merger.state) {
                merger.state = mergerState;
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
                    /*Optimiser blockend recurse*/
                } else {
                    processing = true;
                    /*Optimiser blockend setters*/
                    for (Bus destination : destinations) {
                        destination.setState(mergerState);
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
                                destination.setState(merger.state);
                            }
                        }
                    }
                    /*Optimiser blockend recurse*/
                    processing = false;
                }
                /*Optimiser blockend setters*/
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
    /*Optimiser blockend iSetter*/

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
        ClassOptimiser<BusMergerBusIn> optimiser = new ClassOptimiser<>(this).unroll(merger.destinations.length);
        if (mask == merger.mask) {
            optimiser.cut("otherMask");
        } else {
            optimiser.cut("sameMask");
        }
        if (!keepSetters) {
            optimiser.cut("setters");
        }
        if (!triState) {
            optimiser.cut("iSetter");
        }
        BusMergerBusIn optimised = optimiser.build();
        merger.sources.add(optimised);
        return optimised;
    }
}
