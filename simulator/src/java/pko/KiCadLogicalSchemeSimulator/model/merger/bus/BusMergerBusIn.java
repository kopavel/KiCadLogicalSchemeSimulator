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
package pko.KiCadLogicalSchemeSimulator.model.merger.bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.model.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.model.merger.wire.WireMergerWireIn;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

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
        assert Log.debug(WireMergerWireIn.class,
                "Bus merger change. before: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})\",",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
        state = newState;
        if (hiImpedance && (merger.strongPins & mask) != 0) {
            throw new ShortcutException(merger.inputs);
        }
        long oldState = merger.state;
        if (hiImpedance) {
            hiImpedance = false;
            merger.strongPins |= mask;
        }
        merger.state &= nMask;
        merger.state |= state;
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
        assert Log.debug(WireMergerWireIn.class,
                "Bus merger change. after: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})\",",
                newState,
                getName(),
                state,
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
                "Bus merger setImpedance. before: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})\",",
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        long oldState = merger.state;
        merger.strongPins &= nMask;
        merger.state &= nMask;
        merger.state |= merger.weakState & mask;
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
                "Bus merger setImpedance. after: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})\",",
                getName(),
                state,
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

    @Override
    public Bus getOptimised() {
        if (merger.inputs.length == 1) {
            OutBus optimised = new OutBus(merger, "Optimised");
            optimised.destinations = merger.destinations;
            optimised.state = state;
            optimised.hiImpedance = hiImpedance;
            return optimised;
        } else {
            return this;
        }
    }
}
