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
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class WireMergerWireIn extends InPin implements MergerInput<Pin> {
    public Pin[] destinations;
    public boolean oldStrong;

    public WireMergerWireIn(Pin source, WireMerger merger) {
        super(source, "PMergePIn");
        this.merger = merger;
        hiImpedance = source.hiImpedance;
        destinations = merger.destinations;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public WireMergerWireIn(WireMergerWireIn oldPin, String variantId) {
        super(oldPin, variantId);
        destinations = oldPin.destinations;
        oldStrong = oldPin.oldStrong;
        hiImpedance = oldPin.hiImpedance;
        merger = oldPin.merger;
        triState = oldPin.triState;
    }

    @Override
    public void setState(boolean newState) {
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger change. before: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{}, hiImpedance:{}, weakState:{})",
                newState,
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
        /*Optimiser block passivePins*/
        boolean strengthChange = false;
        /*Optimiser blockend passivePins*/
        /*Optimiser block setters*/
        state = newState;
        /*Optimiser blockend setters*/
        /*Optimiser block passivePins*/
        if (strong) { //to strong
            /*Optimiser blockend passivePins*/
            /*Optimiser block hiAndPassive*/
            if (hiImpedance
                    /*Optimiser block passivePins*///
                    || !oldStrong
                /*Optimiser blockend passivePins*///
            ) { //from hiImpedance or weak
                if (merger.strong) { //strong pins shortcut
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        /*Optimiser block iSetter*/
                        hiImpedance = false;
                        /*Optimiser blockend iSetter*/
                        throw new ShortcutException(((WireMerger) merger).sources);
                    }
                }
                /*Optimiser block passivePins*/
                /*Optimiser block iSetter*/
                if (!hiImpedance) { // from weak
                    /*Optimiser blockend iSetter*/
                    merger.weakState -= (merger.weakState > 0 ? 1 : -1);
                    /*Optimiser block iSetter*/
                } else {
                    hiImpedance = false;
                }
                /*Optimiser blockend iSetter*/
                strengthChange = true;
                /*Optimiser blockend passivePins*/
                merger.strong = true;
            }
            /*Optimiser blockend hiAndPassive*/
            merger.state = newState;
            /*Optimiser block passivePins*/
        } else { //to weak
            if (merger.weakState != 0 && ((merger.weakState > 0) != newState)) { //opposite weak state
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Weak state shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    /*Optimiser block iSetter*/
                    hiImpedance = false;
                    /*Optimiser blockend iSetter*/
                    throw new ShortcutException(((WireMerger) merger).sources);
                }
            }
            /*Optimiser block iSetter*/
            if (hiImpedance) { // from impedance
                hiImpedance = false;
                if (merger.hiImpedance) {
                    merger.strong = false;
                    merger.state = newState;
                }
                merger.weakState += (newState ? 1 : -1);
            } else
                /*Optimiser blockend iSetter*/
                if (oldStrong) { //from strong
                    merger.strong = false;
                    strengthChange = true;
                    merger.weakState += (newState ? 1 : -1);
                    merger.state = newState;
                }
        }
        /*Optimiser blockend passivePins*/
        if (merger.hiImpedance || oldState != merger.state) {
            merger.hiImpedance = false;
            for (Pin destination : destinations) {
                destination.strong = strong;
                destination.setState(merger.state);
            }
            /*Optimiser block passivePins*/
        } else if (strengthChange) {
            for (Pin destination : ((WireMerger) merger).mergers) {
                destination.strong = strong;
                destination.setState(merger.state);
            }
            /*Optimiser blockend passivePins*/
        }
        /*Optimiser block passivePins*/
        oldStrong = strong;
        for (PassivePin passivePin : ((WireMerger) merger).passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockend passivePins*/
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger change. after: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{}, hiImpedance:{}, weakState:{})",
                newState,
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

    /*Optimiser block iSetter*/
    @Override
    public void setHiImpedance() {
        assert Log.debug(WireMergerWireIn.class,
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
                        destination.setState(merger.state);
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
        /*Optimiser blockend passivePins*/
        assert Log.debug(WireMergerWireIn.class,
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
    public WireMergerWireIn getOptimised(boolean keepSetters) {
        ((WireMerger) merger).sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(false);
            if (triState) {
                destinations[i].triState = true;
            }
        }
        ClassOptimiser<WireMergerWireIn> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
        if (((WireMerger) merger).passivePins.isEmpty()) {
            optimiser.cut("passivePins");
            if (!triState) {
                optimiser.cut("hiAndPassive");
            }
        }
        if (!keepSetters) {
            optimiser.cut("setters");
        }
        if (!triState) {
            optimiser.cut("iSetter");
        }
        WireMergerWireIn pin = optimiser.build();
        ((WireMerger) merger).sources.add(pin);
        return pin;
    }

    @Override
    public long getMask() {
        return 0;
    }
}
