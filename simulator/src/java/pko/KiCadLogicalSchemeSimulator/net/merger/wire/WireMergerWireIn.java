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
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class WireMergerWireIn extends InPin implements MergerInput<Pin> {
    public Pin[] destinations;
    public boolean oldStrong;
    public boolean oldImpedance;

    public WireMergerWireIn(Pin source, WireMerger merger) {
        super(source, "PMergePIn");
        this.merger = merger;
        oldImpedance = source.hiImpedance;
        destinations = merger.destinations;
    }

    @Override
    public void setState(boolean newState) {
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger change. before: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{} hiImpedance:{})",
                newState,
                getName(),
                state,
                oldStrong,
                strong,
                oldImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        boolean oldState = merger.state;
        boolean strengthChange = false;
        state = newState;
        hiImpedance = false;
        if (strong) { //to strong
            if (oldImpedance || !oldStrong) { //from hiImpedance or weak
                if (merger.strong) { //strong pins shortcut
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(((WireMerger) merger).passivePins);
                    }
                }
                if (!oldImpedance) { // from weak
                    merger.weakState -= (merger.weakState > 0 ? 1 : -1);
                }
                strengthChange = true;
                merger.strong = true;
            }
            merger.state = newState;
        } else { //to weak
            if (merger.weakState != 0 && ((merger.weakState > 0) == newState)) { //opposite weak state
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    throw new ShortcutException(((WireMerger) merger).passivePins);
                }
            }
            if (oldImpedance) { // from impedance
                merger.strong = false;
                strengthChange = true;
                merger.weakState += (merger.weakState > 0 ? 1 : -1);
            } else if (oldStrong) { //from strong
                merger.strong = false;
                strengthChange = true;
                merger.weakState += (merger.weakState > 0 ? 1 : -1);
                merger.state = merger.weakState > 0;
            }
        }
        for (PassivePin passivePin : ((WireMerger) merger).passivePins) {
            passivePin.parent.onPassivePinChange(this.merger);
        }
        if (merger.hiImpedance) {
            for (Pin destination : destinations) {
                destination.setHiImpedance();
            }
            merger.hiImpedance = false;
        } else if (oldState != merger.state) {
            for (Pin destination : destinations) {
                destination.setState(merger.state);
            }
        } else if (strengthChange) {
            for (Pin destination : ((WireMerger) merger).mergers) {
                destination.setState(merger.state);
            }
        }
        oldStrong = strong;
        oldImpedance = false;
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger change. after: newState:{}, Source:{} (state:{}, oldStrong:{}, strong:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, " +
                        "strong:{} hiImpedance:{})",
                newState,
                getName(),
                state,
                oldStrong,
                strong,
                oldImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
    }

    @Override
    public void setHiImpedance() {
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger setImpedance. before: Source:{} (state:{}, oldStrong:{}, strong:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{} " +
                        "hiImpedance:{})",
                getName(),
                state,
                oldStrong,
                strong,
                oldImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        boolean oldState = merger.state;
        if (oldStrong) {
            if (merger.weakState == 0) {
                merger.hiImpedance = true;
                for (Pin destination : destinations) {
                    destination.setHiImpedance();
                }
            } else {
                merger.state = merger.weakState > 0;
                if (oldState != merger.state) {
                    for (Pin destination : destinations) {
                        destination.setState(merger.state);
                    }
                }
            }
        } else {
            merger.weakState -= (merger.weakState > 0 ? 1 : -1);
            if (merger.weakState == 0) {
                for (Pin destination : destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        }
        hiImpedance = true;
        oldImpedance = true;
        strong = false;
        oldStrong = false;
        merger.strong = false;
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger setImpedance. after: Source:{} (state:{}, oldStrong:{}, strong:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{} " +
                        "hiImpedance:{})",
                getName(),
                state,
                oldStrong,
                strong,
                oldImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else if (!oldImpedance) {
            setHiImpedance();
        }
    }

    @Override
    public WireMergerWireIn getOptimised() {
        ((WireMerger) merger).sources.remove(this);
        destinations = merger.destinations;
        return this;
    }
}
