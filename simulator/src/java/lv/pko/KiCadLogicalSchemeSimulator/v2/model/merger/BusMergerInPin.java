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
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.in.InPin;

public class BusMergerInPin extends InPin implements MergerInput {
    public final long mask;
    public final long nMask;
    private final byte offset;
    private final BusMerger merger;
    private boolean strong;

    public BusMergerInPin(Pin source, long mask, byte offset, BusMerger merger) {
        super(source, "BMergerPIn");
        this.mask = mask;
        nMask = ~mask;
        this.offset = offset;
        this.merger = merger;
        strong = source.strong;
    }

    @Override
    public void setState(boolean newState, boolean strong) {
        long oldState = merger.state;
        boolean oldHiImpedance = merger.hiImpedancePins > 0;
        if (strong) {
            if ((merger.hiImpedancePins & mask) != mask) {
                throw new ShortcutException(this);
            }
            if (!this.strong && !hiImpedance) {
                merger.weakState &= nMask;
                merger.weakStates[offset] -= (byte) (merger.weakStates[offset] > 0 ? 1 : -1);
            }
            if (newState) {
                merger.state |= mask;
            } else {
                merger.state &= nMask;
            }
        } else {
            final byte oldWeakState = merger.weakStates[offset];
            if (oldWeakState != 0 && (oldWeakState > 0 ^ newState)) {
                throw new ShortcutException(merger.inputs);
            }
            merger.weakStates[offset] += (byte) (newState ? 1 : -1);
            if (this.strong || (merger.hiImpedancePins & mask) > 0) {
                if (newState) {
                    merger.state |= mask;
                } else {
                    merger.state &= nMask;
                }
            }
        }
        merger.hiImpedancePins &= nMask;
        this.strong = strong;
        if (merger.hiImpedancePins > 0) {
            if (!oldHiImpedance) {
                for (Bus destination : merger.destinations) {
                    destination.setHiImpedance();
                }
            }
        } else if (oldState != merger.state || oldHiImpedance) {
            for (Bus destination : merger.destinations) {
                destination.setState(merger.state);
            }
        }
    }

    @Override
    public void setHiImpedance() {
        long oldState = merger.state;
        boolean oldHiImpedance = merger.hiImpedancePins > 0;
        if (strong) {
            merger.hiImpedancePins |= mask;
            if (merger.weakStates[offset] > 0) {
                merger.state |= mask;
            } else if (merger.weakStates[offset] < 0) {
                merger.state &= nMask;
            }
        } else {
            merger.weakStates[offset] -= (byte) (merger.weakStates[offset] > 0 ? 1 : -1);
            merger.weakState &= nMask;
            if (merger.weakStates[offset] == 0) {
                merger.state &= nMask;
                merger.hiImpedancePins |= mask;
            }
        }
        hiImpedance = true;
        if (merger.hiImpedancePins > 0) {
            if (!oldHiImpedance) {
                for (Bus destination : merger.destinations) {
                    destination.setHiImpedance();
                }
            }
        } else if (oldState != merger.state || oldHiImpedance) {
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
