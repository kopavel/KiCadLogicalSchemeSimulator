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
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class BusMergerWireIn extends InPin implements MergerInput<Pin> {
    public final long nMask;
    public final BusMerger merger;
    @Getter
    public long mask;
    public boolean oldStrong;
    public Bus[] destinations;

    public BusMergerWireIn(long mask, BusMerger merger) {
        super(merger.id + ":in", merger.parent);
        variantId = "BMergePIn";
        this.mask = mask;
        nMask = ~mask;
        this.merger = merger;
        destinations = merger.destinations;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public BusMergerWireIn(BusMergerWireIn oldPin, String variantId) {
        super(oldPin, variantId);
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        merger = oldPin.merger;
        destinations = merger.destinations;
        triState = oldPin.triState;
    }

    @Override
    public void setHi() {
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger change. before: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})", true,
                getName(),
                state, strong, hiImpedance, merger.getName(), merger.state, merger.strongPins, merger.weakState, merger.weakPins);
        long mergerState = merger.state;
        /*Optimiser line setters*/
        state = true;
        if (strong) { //to strong
            if (
                /*Optimiser line iSetter*/
                    hiImpedance ||//
                            !oldStrong) { //from hiImpedance or weak
                /*Optimiser bind m:mask*/
                if ((merger.strongPins & mask) != 0) { //strong pins shortcut
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        /*Optimiser line iSetter*/
                        hiImpedance = false;
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser line iSetter*/
                if (!hiImpedance) { // from weak
                    /*Optimiser bind nm:nMask*/
                    merger.weakState &= nMask;
                    /*Optimiser bind nm:nMask*/
                    merger.weakPins &= nMask;
                    /*Optimiser block iSetter*/
                } else {
                    hiImpedance = false;
                }
                /*Optimiser blockEnd iSetter bind m:mask*/
                merger.strongPins |= mask;
            }
            /*Optimiser bind m:mask*/
            mergerState |= mask;
        } else { //to weak
            /*Optimiser bind m:mask*/
            if ((merger.weakPins & mask) != 0 && ((merger.weakState & mask) == 0)) { //opposite weak state
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    /*Optimiser line iSetter*/
                    hiImpedance = false;
                    throw new ShortcutException(merger.sources);
                }
            }
            /*Optimiser block iSetter*/
            if (hiImpedance) { // from impedance
                /*Optimiser bind m:mask*/
                merger.weakPins |= mask;
                hiImpedance = false;
            } else
                /*Optimiser blockEnd iSetter*/
                if (oldStrong) { //from strong
                    /*Optimiser bind nm:nMask*/
                    merger.strongPins &= nMask;
                    /*Optimiser bind m:mask*/
                    merger.weakPins |= mask;
                }
            /*Optimiser bind m:mask*/
            if ((merger.strongPins & mask) == 0) {
                /*Optimiser bind m:mask*/
                mergerState |= mask;
            }
        }
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
        oldStrong = strong;
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger change. after: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})",
                true,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
    }

    @Override
    public void setLo() {
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger change. before: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})",
                false,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        long mergerState = merger.state;
        /*Optimiser line setters*/
        state = false;
        if (strong) { //to strong
            if (
                /*Optimiser line iSetter*/
                    hiImpedance ||//
                            !oldStrong) { //from hiImpedance or weak
                /*Optimiser bind m:mask*/
                if ((merger.strongPins & mask) != 0) { //strong pins shortcut
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        /*Optimiser line iSetter*/
                        hiImpedance = false;
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser line iSetter*/
                if (!hiImpedance) { // from weak
                    /*Optimiser bind nm:nMask*/
                    merger.weakState &= nMask;
                    /*Optimiser bind nm:nMask*/
                    merger.weakPins &= nMask;
                    /*Optimiser block iSetter*/
                } else {
                    hiImpedance = false;
                }
                /*Optimiser blockEnd iSetter bind m:mask*/
                merger.strongPins |= mask;
            }
            /*Optimiser bind nm:nMask*/
            mergerState &= nMask;
        } else { //to weak
            /*Optimiser bind m:mask*/
            if ((merger.weakPins & mask) != 0 && !((merger.weakState & mask) == 0)) { //opposite weak state
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    /*Optimiser line iSetter*/
                    hiImpedance = false;
                    throw new ShortcutException(merger.sources);
                }
            }
            /*Optimiser block iSetter*/
            if (hiImpedance) { // from impedance
                /*Optimiser bind m:mask*/
                merger.weakPins |= mask;
                hiImpedance = false;
            } else
                /*Optimiser blockEnd iSetter*/
                if (oldStrong) { //from strong
                    /*Optimiser bind nm:nMask*/
                    merger.strongPins &= nMask;
                    /*Optimiser bind m:mask*/
                    merger.weakPins |= mask;
                }
            /*Optimiser bind m:mask*/
            if ((merger.strongPins & mask) == 0) {
                /*Optimiser bind nm:nMask*/
                mergerState &= nMask;
            }
        }
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
        oldStrong = strong;
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger change. after: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})", false,
                getName(),
                state, strong, hiImpedance, merger.getName(), merger.state, merger.strongPins, merger.weakState, merger.weakPins);
    }

    /*Optimiser block iSetter*/
    @Override
    public void setHiImpedance() {
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger setImpedance. before: Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        if (hiImpedance) {
            return;
        }
        long mergerState = merger.state;
        if (oldStrong) {
            /*Optimiser bind nm:nMask*/
            merger.strongPins &= nMask;
            /*Optimiser bind nm:nMask*/
            mergerState &= nMask;
            /*Optimiser bind m:mask*/
            mergerState |= merger.weakState & mask;
        } else {
            /*Optimiser bind nm:nMask*/
            merger.weakPins &= nMask;
            /*Optimiser bind m:mask*/
            if ((merger.strongPins & mask) != 0) {
                /*Optimiser bind nm:nMask*/
                mergerState &= nMask;
            }
        }
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
        hiImpedance = true;
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger setImpedance. after: Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                getName(),
                state,
                strong,
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
            if (state) {
                setHi();
            } else {
                setLo();
            }
        } else {
            setHiImpedance();
        }
    }

    @Override
    public BusMergerWireIn getOptimised(ModelItem<?> source) {
        if (getClass() == BusMergerWireIn.class) {
            return this;
        }
        merger.sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(merger);
            if (triState) {
                destinations[i].triState = true;
            }
        }
        ClassOptimiser<BusMergerWireIn> optimiser = new ClassOptimiser<>(this).unroll(merger.destinations.length).bind("m", mask).bind("nm", nMask);
        if (source != null) {
            optimiser.cut("setters");
        }
        if (!triState) {
            optimiser.cut("iSetter");
        }
        BusMergerWireIn build = optimiser.build();
        merger.sources.add(build);
        build.source = source;
        return build;
    }
}
