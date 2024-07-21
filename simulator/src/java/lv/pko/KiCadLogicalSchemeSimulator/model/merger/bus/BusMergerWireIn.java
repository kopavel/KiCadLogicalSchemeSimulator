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
package lv.pko.KiCadLogicalSchemeSimulator.model.merger.bus;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.ModelOutItem;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.MergerInput;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.wire.WireMerger;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.wire.WireMergerWireIn;
import lv.pko.KiCadLogicalSchemeSimulator.model.wire.WireToBusAdapter;
import lv.pko.KiCadLogicalSchemeSimulator.model.wire.WireToBusesAdapter;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Log;

public class BusMergerWireIn extends InPin implements MergerInput {
    public final long mask;
    public final long nMask;
    private final byte offset;
    private final BusMerger merger;
    public WireMerger input;

    public BusMergerWireIn(OutPin source, long mask, byte offset, BusMerger merger) {
        super(source, "BMergePIn");
        this.mask = mask;
        nMask = ~mask;
        this.offset = offset;
        this.merger = merger;
        this.input = new WireMerger(this);
    }

    @Override
    public void setState(boolean newState, boolean newStrong) {
        assert Log.debug(WireMergerWireIn.class,
                "Bus merger change. before: newState:{}, newStrong:{}, Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{}, hiImpedance:{})",
                newState,
                newStrong,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
        long oldState = merger.state;
        if (newStrong) { //to strong
            if (hiImpedance && (merger.strongPins & mask) != 0) {
                throw new ShortcutException(merger.inputs);
            }
            if (!strong) { //from non-strong
                merger.strongPins |= mask;
                if (!hiImpedance) { //from weak specifically
                    merger.weakState &= nMask;
                    merger.weakPins &= nMask;
                }
            }
            if (newState) {
                merger.state |= mask;
            } else {
                merger.state &= nMask;
            }
        } else { //to weak
            if (((merger.weakState & mask) > 0) != newState) {
                throw new ShortcutException(merger.inputs);
            }
            if (hiImpedance) {
                merger.weakPins |= mask;
            } else if (strong) {
                merger.strongPins &= nMask;
                merger.weakPins |= mask;
            }
            if ((merger.strongPins & mask) == 0) {
                if (newState) {
                    merger.state |= mask;
                } else {
                    merger.state &= nMask;
                }
            }
        }
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                for (Bus destination : merger.destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            merger.hiImpedance = false;
            for (Bus destination : merger.destinations) {
                destination.setState(merger.state);
            }
        }
        strong = newStrong;
        hiImpedance = false;
        assert Log.debug(WireMergerWireIn.class,
                "Bus merger change. after: newState:{}, newStrong:{}, Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{}, hiImpedance:{})",
                newState,
                newStrong,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
    }

    @Override
    public void setHiImpedance() {
        assert Log.debug(WireMergerWireIn.class,
                "Bus merger setImpedance. before: Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})",
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
        assert !hiImpedance : "Already in hiImpedance:" + this;
        long oldState = merger.state;
        if (strong) {
            merger.strongPins &= nMask;
            merger.state &= nMask;
            merger.state |= merger.weakState & mask;
        } else {
            merger.weakPins &= nMask;
            if ((merger.strongPins & mask) > 0) {
                merger.state &= nMask;
            }
        }
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                for (Bus destination : merger.destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            merger.hiImpedance = false;
            for (Bus destination : merger.destinations) {
                destination.setState(merger.state);
            }
        }
        hiImpedance = true;
        assert Log.debug(WireMergerWireIn.class,
                "Bus merger setImpedance. after: Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})",
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
    }

    @Override
    public String getHash() {
        return mask + ":" + getName();
    }

    public void bindSources() {
        input.bindSources();
    }

    public void addSource(ModelOutItem newSource) {
        input.addSource(newSource, 0L, (byte) 0);
    }

    @Override
    public Pin getOptimised() {
        if (merger.inputs.length == 1) {
            if (merger.destinations.length == 1) {
                return new WireToBusAdapter(this, merger.destinations[0], offset);
            } else {
                return new WireToBusesAdapter(this, merger.destinations, offset);
            }
        } else {
            return this;
        }
    }
}
