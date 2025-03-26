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
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

//Todo with one Bus input and only weak others - use simpler "Weak bus" implementation
public class BusMergerBusIn extends InBus implements MergerInput<Bus> {
    @Getter
    public final long mask;
    public final long nMask;
    public final BusMerger merger;
    public Bus[] destinations;
    public long maskState;

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
        /*Optimiser line setters*/
        state = newState;
        /*Optimiser line o*/
        if (applyMask != 0) {
            /*Optimiser block byMask bind gm:applyMask*/
            newState = newState & applyMask;
            if (maskState != newState
                    /*Optimiser line iSetter*///
                    || hiImpedance//
            ) {
                maskState = newState;
            } else {
                return;
            }
            /*Optimiser line o blockEnd byMask*/
        }
        /*Optimiser line o*/
        if (applyOffset > 0) {
            /*Optimiser line byOffset line positive bind o:applyOffset*/
            newState = newState << applyOffset;
            /*Optimiser line o*/
        } else if (applyOffset < 0) {
            /*Optimiser line byOffset line negative bind o:-applyOffset*/
            newState = newState >> -applyOffset;
            /*Optimiser line o*/
        }
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
        BusMerger merger = this.merger;
        /*Optimiser block sameMask line otherMask*/
        if (merger.mask == mask) {
            /*Optimiser block iSetter*/
            if (hiImpedance) {
                hiImpedance = false;
                if (merger.strongPins != 0) {
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser bind m:mask*/
                merger.strongPins = mask;
            }
            /*Optimiser blockEnd iSetter*/
            if (newState != merger.state) {
                merger.state = newState;
                /*Optimiser block setters*/
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
                            /*Optimiser blockEnd iSetter*/
                            for (Bus destination : destinations) {
                                destination.setState(state);
                            }
                            /*Optimiser line iSetter*/
                        }
                    }
                    /*Optimiser blockEnd recurse*/
                    processing = false;
                }
                /*Optimiser blockEnd setters*/
            }
            /*Optimiser block otherMask*/
        } else {
            /*Optimiser blockEnd sameMask*/
            long mergerState = merger.state;
            /*Optimiser block iSetter*/
            if (hiImpedance) {
                hiImpedance = false;
                /*Optimiser bind m:mask*/
                if ((merger.strongPins & mask) != 0) {
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser bind m:mask*/
                merger.strongPins |= mask;
            }
            /*Optimiser blockEnd iSetter bind nm:nMask*/
            mergerState &= nMask;
            mergerState |= newState;
            if (mergerState != merger.state) {
                merger.state = mergerState;
                /*Optimiser block setters*/
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
                            /*Optimiser blockEnd iSetter*/
                            for (Bus destination : destinations) {
                                destination.setState(merger.state);
                            }
                            /*Optimiser line iSetter*/
                        }
                    }
                    /*Optimiser blockEnd recurse*/
                    processing = false;
                }
                /*Optimiser blockEnd setters*/
            }
            /*Optimiser line sameMask*/
        }
        /*Optimiser blockEnd otherMask*/
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
        BusMerger merger = this.merger;
        /*Optimiser block sameMask line otherMask*/
        if (mask == merger.mask) {
            merger.strongPins = 0;
            if (merger.weakState != merger.state) {
                merger.state = merger.weakState;
                /*Optimiser block setters*/
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
                    /*Optimiser blockEnd recurse*/
                    processing = false;
                }
                /*Optimiser blockEnd setters*/
            }
            /*Optimiser block otherMask*/
        } else {
            /*Optimiser blockEnd sameMask*/
            long mergerState = merger.state;
            /*Optimiser bind nm:nMask*/
            merger.strongPins &= nMask;
            /*Optimiser bind nm:nMask*/
            mergerState &= nMask;
            /*Optimiser bind m:mask*/
            mergerState |= merger.weakState & mask;
            if (mergerState != merger.state) {
                merger.state = mergerState;
                /*Optimiser block setters*/
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
                    /*Optimiser blockEnd recurse*/
                    processing = false;
                }
                /*Optimiser blockEnd setters*/
            }
            /*Optimiser line sameMask*/
        }
        /*Optimiser blockEnd otherMask*/
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
    /*Optimiser blockEnd iSetter*/

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else {
            setHiImpedance();
        }
    }

    @Override
    public BusMergerBusIn getOptimised(ModelItem<?> source) {
        //ToDo in case of "no passive pin" weakPins/weakState are known after build phase (incomplete)
        merger.sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(merger);
            if (triState) {
                destinations[i].triState = true;
            }
        }
        ClassOptimiser<BusMergerBusIn> optimiser = new ClassOptimiser<>(this).cut("o").unroll(merger.destinations.length).bind("m", mask);
        if (mask == merger.mask) {
            optimiser.cut("otherMask");
        } else {
            optimiser.cut("sameMask").bind("nm", nMask);
        }
        if (source != null) {
            optimiser.cut("setters");
        }
        if (!triState) {
            optimiser.cut("iSetter");
        }
        if (applyMask == 0) {
            optimiser.cut("byMask");
        } else {
            optimiser.bind("gm", applyMask);
        }
        if (applyOffset == 0) {
            optimiser.cut("byOffset");
        } else if (applyOffset > 0) {
            optimiser.cut("negative").bind("o", applyOffset);
        } else {
            optimiser.cut("positive").bind("o", -applyOffset);
        }
        BusMergerBusIn build = optimiser.build();
        merger.sources.add(build);
        build.source = source;
        return build;
    }

    @Override
    public boolean useFullOptimiser() {
        return true;
    }
}
