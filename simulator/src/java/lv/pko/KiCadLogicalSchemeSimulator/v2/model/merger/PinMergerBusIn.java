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
package lv.pko.KiCadLogicalSchemeSimulator.v2.model.merger;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.Bus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.in.CorrectedInBus;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;

public class PinMergerBusIn extends CorrectedInBus implements MergerInput {
    public final long mask;
    private final PinMerger merger;

    public PinMergerBusIn(Bus source, long mask, PinMerger merger) {
        super(source, "PMergeBIn");
        this.mask = mask;
        this.merger = merger;
    }

    @Override
    public void setState(long newState) {
        if (hiImpedance && merger.strong) {
            throw new ShortcutException(merger.mergerInputs);
        }
        if (merger.state != (newState > 0)) {
            merger.state = newState > 0;
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, true);
            }
        } else if (!merger.strong || merger.hiImpedance) {
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, true);
            }
        }
        merger.strong = true;
        merger.hiImpedance = false;
    }

    @Override
    public void setHiImpedance() {
        if (merger.weakState == 0) {
            merger.hiImpedance = true;
            for (Pin destination : merger.destinations) {
                destination.setHiImpedance();
            }
        } else if (merger.state != (merger.weakState > 0)) {
            for (Pin destination : merger.destinations) {
                destination.setState((merger.weakState > 0), false);
            }
        }
    }

    @Override
    public String getHash() {
        return mask + ":" + getName();
    }
}
