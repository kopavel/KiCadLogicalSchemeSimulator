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
package lv.pko.KiCadLogicalSchemeSimulator.model.merger.wire;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.MergerInput;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Log;

public class WireMergerWireIn extends InPin implements MergerInput {
    private final WireMerger merger;

    public WireMergerWireIn(Pin source, WireMerger merger) {
        super(source, "PMergePIn");
        this.merger = merger;
    }

    @Override
    public void setState(boolean newState, boolean newStrong) {
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger change. before: newState:{}, newStrong:{}, Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, " +
                        "hiImpedance:{})",
                newState,
                newStrong,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        boolean oldState = merger.state;
        boolean oldStrong = merger.strong;
        if (newStrong) { // to strong
            if (!strong) { // from weak
                if (merger.strong) {
                    throw new ShortcutException(merger.mergerInputs); //but merger already strong
                }
                if (!hiImpedance) {
                    merger.weakState -= (byte) (merger.weakState > 0 ? 1 : -1); //count down weak states
                }
            } else if (hiImpedance && merger.strong) { //from hiImpedance but merger already strong
                throw new ShortcutException(merger.mergerInputs);
            }
            merger.state = newState;
            merger.strong = true;
        } else { //to weak
            if (merger.weakState != 0 && (merger.weakState > 0 ^ newState)) {
                throw new ShortcutException(merger.mergerInputs); // merger in opposite weak state
            }
            if (strong) { // from string
                merger.weakState += (byte) (newState ? 1 : -1); // count up weak state
                merger.state = newState;
                merger.strong = false;
            } else if (hiImpedance) { // from hiImpedance
                merger.weakState += (byte) (newState ? 1 : -1);// count up weak state
            }
        }
        if (merger.hiImpedance) { // from merger hiImpedance
            merger.state = newState;
            merger.hiImpedance = false;
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, merger.strong);
            }
        } else if (oldState != merger.state || oldStrong != merger.strong) { // in case of merger state changed
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, merger.strong);
            }
        }
        hiImpedance = false;
        strong = newStrong;
        assert Log.debug(WireMergerWireIn.class,
                "Pin merger change. after: newState:{}, newStrong:{}, Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, " +
                        "hiImpedance:{})",
                newState,
                newStrong,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        if (!strong) { //from weak
            merger.weakState -= (byte) (merger.weakState > 0 ? 1 : -1); //count down weak states
        } else {
            merger.strong = false;
        }
        if (!merger.strong) { //merger in weak state
            if (merger.weakState == 0) {
                for (Pin destination : merger.destinations) {
                    destination.setHiImpedance(); //no weak state anymore - go to hiImpedance
                }
            }
        }
        hiImpedance = true;
    }

    @Override
    public String getHash() {
        return getName();
    }
}
