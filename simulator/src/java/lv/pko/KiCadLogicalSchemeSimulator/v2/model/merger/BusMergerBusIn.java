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

public class BusMergerBusIn extends CorrectedInBus implements MergerInput {
    public final long mask;
    public final long nMask;
    private final BusMerger merger;

    public BusMergerBusIn(Bus source, long mask, BusMerger merger) {
        super(source, "BMergeBIn");
        this.mask = mask;
        this.merger = merger;
        nMask = ~mask;
    }

    @Override
    public void setState(long newState) {
        if ((merger.strongPins & mask) != 0) {
            throw new ShortcutException(this);
        }
        long oldState = merger.state;
        if (hiImpedance) {
            hiImpedance = false;
            merger.strongPins |= mask;
        }
        merger.state &= nMask;
        merger.state |= newState;
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                merger.hiImpedance = true;
                for (Bus destination : merger.destinations) {
                    destination.setHiImpedance();
                }
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            merger.hiImpedance = false;
            for (Bus destination : merger.destinations) {
                destination.setState(merger.state);
            }
        }
    }

    @Override
    public void setHiImpedance() {
        long oldState = merger.state;
        merger.strongPins &= nMask;
        merger.state &= nMask;
        merger.state |= merger.weakState & mask;
        hiImpedance = true;
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                merger.hiImpedance = true;
                for (Bus destination : merger.destinations) {
                    destination.setHiImpedance();
                }
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            merger.hiImpedance = false;
            for (Bus destination : merger.destinations) {
                destination.setState(merger.state);
            }
        }
    }

    @Override
    public String getHash() {
        return mask + ":" + getName();
    }
}
