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
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class WireMergerWireIn extends InPin implements MergerInput<Pin> {
    private final WireMerger merger;
    private boolean oldImpedance;

    public WireMergerWireIn(Pin source, WireMerger merger) {
        super(source, "PMergePIn");
        this.merger = merger;
        oldImpedance = source.hiImpedance;
    }

    @Override
    public void setState(boolean newState) {
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger change. before: newState:{}, Source:{} (state:{}, hiImpedance:{}), Merger:{} (state:{},  hiImpedance:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.hiImpedance);
        state = newState;
        hiImpedance = false;
        if (oldImpedance && merger.strong) { //merger not in hiImpedance ow weak
            if (Net.stabilizing) {
                Net.forResend.add(this);
                assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                return;
            } else {
                throw new ShortcutException(merger.sources);
            }
        }
        if (state != merger.state) { // merger state changes
            merger.state = state;
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state);
            }
        } else if (!merger.strong || merger.hiImpedance) {
            for (Pin destination : merger.destinations) {
//                if (destination instanceof PassivePin) { //FixMe known in Net build time
                destination.setState(merger.state);
//                }
            }
        }
        merger.strong = true;
        merger.hiImpedance = false;
        oldImpedance = false;
        assert Log.debug(WireMergerWireIn.class, "Pin merger change. after: newState:{}, Source:{} (state:{}, hiImpedance:{}), Merger:{} (state:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.hiImpedance);
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        if (merger.hasWeak) {
            if (merger.state != merger.weakState) {
                merger.state = merger.weakState;
                for (Pin destination : merger.destinations) {
                    destination.setState(merger.weakState);
                }
            }
        } else {
            for (Pin destination : merger.destinations) {
                destination.setHiImpedance();
            }
            merger.hiImpedance = true;
        }
        hiImpedance = true;
        oldImpedance = true;
        merger.strong = false;
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else if (!oldImpedance) {
            setHiImpedance();
        }
    }
}
