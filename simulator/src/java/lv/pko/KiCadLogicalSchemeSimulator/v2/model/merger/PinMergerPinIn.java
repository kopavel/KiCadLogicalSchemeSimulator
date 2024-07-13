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
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.in.InPin;

public class PinMergerPinIn extends InPin implements MergerInput {
    private final PinMerger merger;
    private boolean strong;

    public PinMergerPinIn(Pin source, PinMerger merger) {
        super(source, "PMergePIn");
        this.merger = merger;
    }

    @Override
    public void setState(boolean newState, boolean newStrong) {
        boolean oldState = merger.state;
        boolean oldHiImpedance = merger.hiImpedance;
        boolean oldStrong = merger.strong;
        if (newStrong) {
            if (merger.strong) {
                throw new ShortcutException(this);
            }
            if (!strong && !hiImpedance) {
                merger.weakState -= (byte) (merger.weakState > 0 ? 1 : -1);
            }
            merger.state = newState;
            merger.strong = true;
        } else {
            final byte oldWeakState = merger.weakState;
            if (oldWeakState != 0 && (oldWeakState > 0 ^ newState)) {
                throw new ShortcutException(merger.mergerInputs);
            }
            merger.weakState += (byte) (newState ? 1 : -1);
            if (strong) {
                merger.state = newState;
                merger.strong = false;
            } else if (merger.hiImpedance) {
                merger.state = newState;
            }
        }
        merger.hiImpedance = false;
        strong = newStrong;
        if (oldState != merger.state || oldStrong != merger.strong || oldHiImpedance) {
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, merger.strong);
            }
        }
    }

    @Override
    public void setHiImpedance() {
/*
        boolean oldState = merger.state;
        boolean oldHiImpedance = merger.hiImpedance;
        boolean oldStrong = merger.strong;
        if (strong) {
            merger.hiImpedance=true;
            if (merger.weakState > 0) {
                merger.state |= mask;
            } else if (merger.weakState < 0) {
                merger.state &= nMask;
            }
        } else {
            merger.weakState -= (byte) (merger.weakState > 0 ? 1 : -1);
            merger.weakState &= nMask;
            if (merger.weakState == 0) {
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
*/
    }

    @Override
    public String getHash() {
        return getName();
    }
}
