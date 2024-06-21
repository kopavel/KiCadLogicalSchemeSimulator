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
package lv.pko.KiCadLogicalSchemeSimulator.model.merger;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;

//ToDo No-TriState Merger in pin  (simpler onChange)
public class MergerInPin extends InPin {
    public final long corrMask;
    public final long nCorrMask;
    public final Merger merger;
    public boolean hiImpedance = false;

    public MergerInPin(OutPin src, byte offset, long mask, Merger merger) {
        super(src.id, src.parent);
        this.merger = merger;
        this.offset = offset;
        this.nOffset = (byte) -offset;
        this.mask = mask;
        if (offset < 0) {
            this.corrMask = mask << nOffset;
        } else {
            this.corrMask = mask >> offset;
        }
        this.nCorrMask = ~corrMask;
        this.source = src;
        if (src instanceof TriStateOutPin triStateOutPin) {
            hiImpedance = triStateOutPin.hiImpedance;
        }
    }

    @Override
    public void onChange(long newState, boolean newImpedance) {
        if (newImpedance != hiImpedance) {
            hiImpedance = newImpedance;
            if (newImpedance) {
                merger.hiImpedancePins |= corrMask;
                merger.state &= nCorrMask;
            } else {
                if ((merger.hiImpedancePins | corrMask) != merger.hiImpedancePins) {
                    throw new ShortcutException(merger.inputs);
                }
                merger.hiImpedancePins &= nCorrMask;
                merger.state &= nCorrMask;
                merger.state |= newState;
            }
            merger.state |= merger.pullState & merger.hiImpedancePins;
            merger.dest.state = merger.state;
            merger.dest.onChange(merger.state, (merger.hiImpedancePins & merger.nPullMask) > 0);
        } else if (!newImpedance) {
            merger.state &= nCorrMask;
            merger.state |= newState;
            merger.dest.state = merger.state;
            merger.dest.onChange(merger.state, (merger.hiImpedancePins & merger.nPullMask) > 0);
        }
    }

    public String getHash() {
        return corrMask + ":" + offset + ":" + source.getName();
    }
}
