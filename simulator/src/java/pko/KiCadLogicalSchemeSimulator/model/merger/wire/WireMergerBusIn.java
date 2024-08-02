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
package pko.KiCadLogicalSchemeSimulator.model.merger.wire;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.model.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class WireMergerBusIn extends CorrectedInBus implements MergerInput<Bus> {
    public final long mask;
    private final WireMerger merger;

    public WireMergerBusIn(Bus source, long mask, WireMerger merger) {
        super(source, "PMergeBIn");
        this.mask = mask;
        this.merger = merger;
    }

    @Override
    public void setState(long newState) {
        assert Log.debug(WireMergerBusIn.class,
                "Pin merger change. before: newState:{}, Source:{} (state:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        state = newState;
        if (hiImpedance && merger.strong) { //merger already in strong
            throw new ShortcutException(merger.sources);
        }
        if (merger.state != (state > 0)) { //merger state changes
            merger.state = state > 0;
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, true);
            }
        } else if (!merger.strong || merger.hiImpedance) { //merger in weak state or in hiImpedance
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, true);
            }
        }
        hiImpedance = false;
        merger.strong = true;
        merger.hiImpedance = false;
        assert Log.debug(WireMergerBusIn.class,
                "Pin merger change. after: newState:{}, Source:{} (state:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        if (merger.weakState == 0) { //no weak state - go to hiImpedance.
            for (Pin destination : merger.destinations) {
                destination.setHiImpedance();
            }
            merger.hiImpedance = true;
        } else if (merger.state != (merger.weakState > 0)) { // state changes to weak
            merger.state = merger.weakState > 0;
            for (Pin destination : merger.destinations) {
                destination.setState((merger.weakState > 0), false);
            }
        }
        hiImpedance = true;
        merger.strong = false;
    }
}
