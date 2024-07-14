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

    public PinMergerPinIn(Pin source, PinMerger merger) {
        super(source, "PMergePIn");
        this.merger = merger;
    }

    @Override
    public void setState(boolean newState, boolean newStrong) {
        boolean oldState = merger.state;
        boolean oldStrong = merger.strong;
        if (newStrong) { //to strong
            if (!strong) {
                if (merger.strong) {
                    throw new ShortcutException(merger.mergerInputs);
                }
                if (!hiImpedance) {
                    merger.weakState -= (byte) (merger.weakState > 0 ? 1 : -1);
                }
            } else {
                if (hiImpedance && merger.strong) {
                    throw new ShortcutException(merger.mergerInputs);
                }
            }
            merger.state = newState;
            merger.strong = true;
        } else { //to weak
            if (merger.weakState != 0 && (merger.weakState > 0 ^ newState)) {
                throw new ShortcutException(merger.mergerInputs);
            }
            if (strong) {
                merger.weakState += (byte) (newState ? 1 : -1);
                merger.state = newState;
                merger.strong = false;
            } else if (hiImpedance) {
                merger.weakState += (byte) (newState ? 1 : -1);
            }
        }
        if (merger.hiImpedance) {
            merger.state = newState;
            merger.hiImpedance = false;
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, merger.strong);
            }
        } else if (oldState != merger.state || oldStrong != merger.strong) {
            for (Pin destination : merger.destinations) {
                destination.setState(merger.state, merger.strong);
            }
        }
        hiImpedance = false;
        strong = newStrong;
    }

    @Override
    public void setHiImpedance() {
        if (!strong) {
            merger.weakState -= (byte) (merger.weakState > 0 ? 1 : -1);
        } else {
            merger.strong = false;
        }
        if (!merger.strong) {
            if (merger.weakState == 0) {
                for (Pin destination : merger.destinations) {
                    destination.setHiImpedance();
                }
            } else {
                for (Pin destination : merger.destinations) {
                    destination.setState(merger.weakState > 0, merger.strong);
                }
            }
        }
    }

    @Override
    public String getHash() {
        return getName();
    }
}
