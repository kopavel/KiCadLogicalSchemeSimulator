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
package pko.KiCadLogicalSchemeSimulator.net.merger.wire;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

//FixMe where recursion support?
public class WireMergerWireIn extends InPin implements MergerInput<Pin> {
    public Pin[] destinations;
    public boolean oldStrong;

    public WireMergerWireIn(Pin source, WireMerger merger) {
        super(source, "PMergePIn");
        this.merger = merger;
        oldStrong = source.strong;
        hiImpedance = source.hiImpedance;
        destinations = merger.destinations;
        triStateIn=true;
        triStateOut = source.triStateOut;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public WireMergerWireIn(WireMergerWireIn oldPin, String variantId) {
        super(oldPin, variantId);
        destinations = oldPin.destinations;
        oldStrong = oldPin.oldStrong;
        hiImpedance = oldPin.hiImpedance;
        merger = oldPin.merger;
    }

    @Override
    public void setHi() {
        OutPin merger = this.merger;
        assert Log.debug(getClass(),
                "Pin merger change. before: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{}, hiImpedance:{}, weakState:{})",
                true,
                getName(),
                state,
                oldStrong,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance,
                merger.weakState);
        boolean oldState = merger.state;
        /*Optimiser line passivePins*/
        boolean strengthChange = false;
        /*Optimiser line setters*/
        state = true;
        /*Optimiser line passivePins*/
        if (strong) { //to strong
            /*Optimiser block hiAndPassive*/
            if (hiImpedance
                    /*Optimiser line passivePins*///
                    || !oldStrong//
            ) { //from hiImpedance or weak
                if (merger.strong) { //strong pins shortcut
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        /*Optimiser line ts*/
                        hiImpedance = false;
                        throw new ShortcutException(((WireMerger) merger).sources);
                    }
                } else {
                    merger.strong=true;
                }
                /*Optimiser block passivePins line ts*/
                if (!hiImpedance) { // from weak
                    merger.weakState -= (merger.weakState > 0 ? 1 : -1);
                    /*Optimiser block ts*/
                } else {
                    /*Optimiser blockEnd passivePins*/
                    hiImpedance = false;
                    /*Optimiser block passivePins*/
                }
                /*Optimiser blockEnd ts*/
                strengthChange = true;
                /*Optimiser blockEnd passivePins*/
            }
            /*Optimiser blockEnd hiAndPassive*/
            merger.state = true;
            /*Optimiser block passivePins*/
        } else { //to weak
            if (merger.weakState != 0 && (!(merger.weakState > 0))) { //opposite weak state
                if (parent.net.stabilizing) {
                    parent.net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Weak state shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    /*Optimiser line ts*/
                    hiImpedance = false;
                    throw new ShortcutException(((WireMerger) merger).sources);
                }
            }
            /*Optimiser block ts*/
            if (hiImpedance) { // from impedance
                hiImpedance = false;
                if (merger.hiImpedance) {
                    merger.strong = false;
                    merger.state = true;
                }
                merger.weakState += (1);
            } else
                /*Optimiser blockEnd ts*/
                if (oldStrong) { //from strong
                    merger.strong = false;
                    strengthChange = true;
                    merger.weakState += (1);
                    merger.state = true;
                }
        }
        /*Optimiser blockEnd passivePins*/
        if (merger.hiImpedance || !oldState
                /*Optimiser line passivePins*///
                == merger.state//
        ) {
            merger.hiImpedance = false;
            for (Pin destination : destinations) {
                destination.strong = strong;
                /*Optimiser line passivePins*/
                if (merger.state) {
                    destination.setHi();
                    /*Optimiser block passivePins*/
                } else {
                    destination.setLo();
                }
                /*Optimiser blockEnd passivePins*/
            }
            /*Optimiser block passivePins*/
        } else if (strengthChange) {
            for (Pin destination : ((WireMerger) merger).mergers) {
                destination.strong = strong;
                if (merger.state) {
                    destination.setHi();
                } else {
                    destination.setLo();
                }
            }
            /*Optimiser blockEnd passivePins*/
        }
        /*Optimiser block passivePins*/
        oldStrong = strong;
        for (PassivePin passivePin : ((WireMerger) merger).passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockEnd passivePins*/
        assert Log.debug(getClass(),
                "Pin merger change. after: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{}, hiImpedance:{}, weakState:{})",
                true,
                getName(),
                state,
                oldStrong,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance,
                merger.weakState);
    }

    @Override
    public void setLo() {
        OutPin merger = this.merger;
        assert Log.debug(getClass(),
                "Pin merger change. before: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{}, hiImpedance:{}, weakState:{})",
                false,
                getName(),
                state,
                oldStrong,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance,
                merger.weakState);
        boolean oldState = merger.state;
        /*Optimiser line passivePins*/
        boolean strengthChange = false;
        /*Optimiser line setters*/
        state = false;
        /*Optimiser line passivePins*/
        if (strong) { //to strong
            /*Optimiser block hiAndPassive*/
            if (hiImpedance
                    /*Optimiser line passivePins*///
                    || !oldStrong//
            ) { //from hiImpedance or weak
                if (merger.strong) { //strong pins shortcut
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        /*Optimiser line ts*/
                        hiImpedance = false;
                        throw new ShortcutException(((WireMerger) merger).sources);
                    }
                } else {
                    merger.strong=true;
                }
                /*Optimiser block passivePins line ts*/
                if (!hiImpedance) { // from weak
                    merger.weakState -= (merger.weakState > 0 ? 1 : -1);
                    /*Optimiser block ts*/
                } else {
                    /*Optimiser blockEnd passivePins*/
                    hiImpedance = false;
                    /*Optimiser block passivePins*/
                }
                /*Optimiser blockEnd ts*/
                strengthChange = true;
                /*Optimiser blockEnd passivePins*/
            }
            /*Optimiser blockEnd hiAndPassive*/
            merger.state = false;
            /*Optimiser block passivePins*/
        } else { //to weak
            if (merger.weakState > 0) { //opposite weak state
                if (parent.net.stabilizing) {
                    parent.net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Weak state shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    /*Optimiser line ts*/
                    hiImpedance = false;
                    throw new ShortcutException(((WireMerger) merger).sources);
                }
            }
            /*Optimiser block ts*/
            if (hiImpedance) { // from impedance
                hiImpedance = false;
                if (merger.hiImpedance) {
                    merger.strong = false;
                    merger.state = false;
                }
                merger.weakState += (-1);
            } else
                /*Optimiser blockEnd ts*/
                if (oldStrong) { //from strong
                    merger.strong = false;
                    strengthChange = true;
                    merger.weakState += (-1);
                    merger.state = false;
                }
        }
        /*Optimiser blockEnd passivePins*/
        if (merger.hiImpedance || oldState
                /*Optimiser line passivePins*///
                != merger.state//
        ) {
            merger.hiImpedance = false;
            for (Pin destination : destinations) {
                destination.strong = strong;
                /*Optimiser block passivePins*/
                if (merger.state) {
                    destination.setHi();
                } else {
                    /*Optimiser blockEnd passivePins*/
                    destination.setLo();
                    /*Optimiser line passivePins*/
                }
            }
            /*Optimiser block passivePins*/
        } else if (strengthChange) {
            for (Pin destination : ((WireMerger) merger).mergers) {
                destination.strong = strong;
                if (merger.state) {
                    destination.setHi();
                } else {
                    destination.setLo();
                }
            }
            /*Optimiser blockEnd passivePins*/
        }
        /*Optimiser block passivePins*/
        oldStrong = strong;
        for (PassivePin passivePin : ((WireMerger) merger).passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockEnd passivePins*/
        assert Log.debug(getClass(),
                "Pin merger change. after: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{}, hiImpedance:{}, weakState:{})",
                false,
                getName(),
                state,
                oldStrong,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance,
                merger.weakState);
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        OutPin merger = this.merger;
        assert Log.debug(getClass(),
                "Pin merger setImpedance. before: Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{} " +
                        "hiImpedance:{}, weakState:{})",
                getName(),
                state,
                oldStrong,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance,
                merger.weakState);
        if (hiImpedance) {
            return;
        }
        boolean oldState = merger.state;
        if (oldStrong) {
            if (merger.weakState == 0) {
                merger.hiImpedance = true;
                merger.strong = false;
                for (Pin destination : destinations) {
                    destination.setHiImpedance();
                }
            } else {
                merger.state = merger.weakState > 0;
                merger.strong = false;
                if (oldState != merger.state) {
                    for (Pin destination : destinations) {
                        if (merger.state) {
                            destination.setHi();
                        } else {
                            destination.setLo();
                        }
                    }
                }
            }
        } else {
            merger.weakState -= (merger.weakState > 0 ? 1 : -1);
            if (merger.weakState == 0 && !merger.strong) {
                for (Pin destination : destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        }
        hiImpedance = true;
        strong = false;
        /*Optimiser block passivePins*/
        oldStrong = false;
        for (PassivePin passivePin : ((WireMerger) merger).passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockEnd passivePins*/
        assert Log.debug(getClass(),
                "Pin merger setImpedance. after: Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{} " +
                        "hiImpedance:{}, weakState:{})",
                getName(),
                state,
                oldStrong,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance,
                merger.weakState);
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
    public WireMergerWireIn getOptimised(ModelItem<?> source) {
        ((WireMerger) merger).sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(merger);
            triStateIn |= destinations[i].triStateIn;
        }
        ClassOptimiser<WireMergerWireIn> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
        if (((WireMerger) merger).passivePins.isEmpty()) {
            optimiser.cut("passivePins");
            if (!triStateIn) {
                optimiser.cut("hiAndPassive");
            }
        }
        if (source != null) {
            optimiser.cut("setters");
        }
        if (!triStateIn) {
            optimiser.cut("ts");
        }
        WireMergerWireIn build = optimiser.build();
        ((WireMerger) merger).sources.add(build);
        build.source = source;
        return build;
    }

    @Override
    public long getMask() {
        return 0;
    }
}
