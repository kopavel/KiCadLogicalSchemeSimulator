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
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

//Fixme cleanup "strong/weak" logic in case on strong only pins
public class BusMergerWireIn extends InPin implements MergerInput<Pin> {
    public final int nMask;
    public final BusMerger merger;
    @Getter
    public int mask;
    public boolean oldStrong;
    public Bus[] destinations;

    public BusMergerWireIn(int mask, BusMerger merger) {
        super(merger.id + ":in", merger.parent);
        variantId = "BMergePIn";
        this.mask = mask;
        nMask = ~mask;
        this.merger = merger;
        destinations = merger.destinations;
        triStateIn = true;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public BusMergerWireIn(BusMergerWireIn oldPin, String variantId) {
        super(oldPin, variantId);
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        merger = oldPin.merger;
        destinations = merger.destinations;
        oldStrong = oldPin.oldStrong;
    }

    @Override
    public void setHi() {
        BusMerger merger = this.merger;
        assert Log.debug(getClass(),
                "Bus merger change. before: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})",
                true,
                getName(),
                getState(),
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        int mergerState = merger.state;
        /*Optimiser line setter*/
        state = true;
        if (strong) { //to strong
            if (
                /*Optimiser line ts*/
                    hiImpedance ||//
                            !oldStrong) { //from hiImpedance or weak
                /*Optimiser bind m:mask*/
                if ((merger.strongPins & mask) != 0) { //strong pins shortcut
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        /*Optimiser line ts*/
                        hiImpedance = false;
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser line ts*/
                if (!hiImpedance) { // from weak
                    /*Optimiser bind nm:nMask*/
                    merger.weakState &= nMask;
                    /*Optimiser bind nm:nMask*/
                    merger.weakPins &= nMask;
                    /*Optimiser block ts*/
                } else {
                    hiImpedance = false;
                }
                /*Optimiser blockEnd ts bind m:mask*/
                merger.strongPins |= mask;
            }
            /*Optimiser bind m:mask*/
            mergerState |= mask;
        } else { //to weak
            /*Optimiser bind m:mask*/
            if ((merger.weakPins & mask) != 0 && ((merger.weakState & mask) == 0)) { //opposite weak state
                if (parent.net.stabilizing) {
                    parent.net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    /*Optimiser line ts*/
                    hiImpedance = false;
                    throw new ShortcutException(merger.sources);
                }
            }
            /*Optimiser block ts*/
            if (hiImpedance) { // from impedance
                /*Optimiser bind m:mask*/
                merger.weakPins |= mask;
                hiImpedance = false;
            } else
                /*Optimiser blockEnd ts*/
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
            /*Optimiser block ar*/
            switch (processing++) {
                case 0: {
                    /*Optimiser blockEnd ar*/
                    for (Bus destination : destinations) {
                        destination.setState(mergerState);
                    }
                    /*Optimiser block r block ar*/
                    while (--processing > 0) {
                        /*Optimiser block ts*/
                        if (hiImpedance) {
                            for (Bus destination : destinations) {
                                destination.setHiImpedance();
                            }
                        } else {
                            /*Optimiser blockEnd ts*/
                            for (Bus destination : destinations) {
                                destination.setState(merger.state);
                            }
                            /*Optimiser line ts*/
                        }
                    }
                    /*Optimiser line nr blockEnd r*/
                    processing = 0;
                    return;
                }
                case 2: {
                    recurseError();
                    return;
                }
            }
            /*Optimiser blockEnd ar*/
        }
        oldStrong = strong;
        assert Log.debug(getClass(),
                "Bus merger change. after: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})",
                true,
                getName(),
                getState(),
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
        BusMerger merger = this.merger;
        assert Log.debug(getClass(),
                "Bus merger change. before: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})",
                false,
                getName(),
                getState(),
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        int mergerState = merger.state;
        /*Optimiser line setter*/
        state = false;
        if (strong) { //to strong
            if (
                /*Optimiser line ts*/
                    hiImpedance ||//
                            !oldStrong) { //from hiImpedance or weak
                /*Optimiser bind m:mask*/
                if ((merger.strongPins & mask) != 0) { //strong pins shortcut
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        /*Optimiser line ts*/
                        hiImpedance = false;
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser line ts*/
                if (!hiImpedance) { // from weak
                    /*Optimiser bind nm:nMask*/
                    merger.weakState &= nMask;
                    /*Optimiser bind nm:nMask*/
                    merger.weakPins &= nMask;
                    /*Optimiser block ts*/
                } else {
                    hiImpedance = false;
                }
                /*Optimiser blockEnd ts bind m:mask*/
                merger.strongPins |= mask;
            }
            /*Optimiser bind nm:nMask*/
            mergerState &= nMask;
        } else { //to weak
            /*Optimiser bind m:mask*/
            if ((merger.weakPins & mask) != 0 && !((merger.weakState & mask) == 0)) { //opposite weak state
                if (parent.net.stabilizing) {
                    parent.net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    /*Optimiser line ts*/
                    hiImpedance = false;
                    throw new ShortcutException(merger.sources);
                }
            }
            /*Optimiser block ts*/
            if (hiImpedance) { // from impedance
                /*Optimiser bind m:mask*/
                merger.weakPins |= mask;
                hiImpedance = false;
            } else
                /*Optimiser blockEnd ts*/
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
            /*Optimiser block ar*/
            switch (processing++) {
                case 0: {
                    /*Optimiser blockEnd ar*/
                    for (Bus destination : destinations) {
                        destination.setState(mergerState);
                    }
                    /*Optimiser block r block ar*/
                    while (--processing > 0) {
                        /*Optimiser block ts*/
                        if (hiImpedance) {
                            for (Bus destination : destinations) {
                                destination.setHiImpedance();
                            }
                        } else {
                            /*Optimiser blockEnd ts*/
                            for (Bus destination : destinations) {
                                destination.setState(merger.state);
                            }
                            /*Optimiser line ts*/
                        }
                    }
                    /*Optimiser line nr blockEnd r*/
                    processing = 0;
                    return;
                }
                case 2: {
                    recurseError();
                    return;
                }
            }
            /*Optimiser blockEnd ar*/
        }
        oldStrong = strong;
        assert Log.debug(getClass(),
                "Bus merger change. after: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{})",
                false,
                getName(),
                getState(),
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        assert !hiImpedance : "Already in hiImpedance:" + this;
        hiImpedance = true;
        BusMerger merger = this.merger;
        assert Log.debug(getClass(),
                "Bus merger setImpedance. before: Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                getName(),
                getState(),
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        int mergerState = merger.state;
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
            /*Optimiser block ar*/
            switch (processing++) {
                case 0: {
                    /*Optimiser blockEnd ar*/
                    for (Bus destination : destinations) {
                        destination.setState(mergerState);
                    }
                    /*Optimiser block r block ar*/
                    while (--processing > 0) {
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
                    /*Optimiser line nr blockEnd r*/
                    processing = 0;
                    return;
                }
                case 2: {
                    recurseError();
                    return;
                }
            }
            /*Optimiser blockEnd ar*/
        }
        assert Log.debug(getClass(),
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
        /*Optimiser blockEnd ts*/
    }

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
        merger.sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(merger);
            triStateIn |= destinations[i].triStateIn;
        }
        ClassOptimiser<BusMergerWireIn> optimiser = new ClassOptimiser<>(this).unroll(merger.destinations.length).bind("m", mask).bind("nm", nMask);
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!isTriState(source)) {
            optimiser.cut("ts");
        }
        if (getRecursionMode() == none) {
            optimiser.cut("ar");
        } else if (getRecursionMode() == warn) {
            optimiser.cut("r");
        } else {
            optimiser.cut("nr");
        }
        BusMergerWireIn build = optimiser.build();
        merger.sources.add(build);
        build.source = source;
        return build;
    }
}
