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
package pko.KiCadLogicalSchemeSimulator.net.merger.bus;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.javaCompiller.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class BusMergerBusIn extends CorrectedInBus implements MergerInput<Bus> {
    public long mask;
    public long nMask;
    public Bus[] destinations;
    public BusMerger merger;
    public boolean oldImpedance;

    public BusMergerBusIn(Bus source, long mask, BusMerger merger) {
        super(source, "BMergeBIn");
        this.mask = mask;
        this.merger = merger;
        nMask = ~mask;
        oldImpedance = hiImpedance;
        destinations = merger.destinations;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public BusMergerBusIn(BusMergerBusIn oldPin, String variantId) {
        super(oldPin, variantId);
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        merger = oldPin.merger;
        destinations = merger.destinations;
        oldImpedance = hiImpedance;
    }

    @Override
    public void setState(long newState) {
//        assert Log.debug(BusMergerBusIn.class,
//                "Bus merger change. before: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
//                        "hiImpedance:{})\",",
//                newState,
//                getName(),
//                state,
//                hiImpedance,
//                merger.getName(),
//                merger.state,
//                merger.strongPins,
//                merger.weakState,
//                merger.weakPins,
//                merger.hiImpedance);
        state = newState;
        hiImpedance = false;
        long oldState = merger.state;
        if (oldImpedance) {
            if ((merger.strongPins & mask) != 0) {
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    throw new ShortcutException(merger.sources);
                }
            }
            merger.strongPins |= mask;
        }
        merger.state &= nMask;
        merger.state |= state;
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                for (Bus destination : destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            merger.hiImpedance = false;
            for (Bus destination : destinations) {
                destination.setState(merger.state);
            }
        }
        oldImpedance = false;
//        assert Log.debug(BusMergerBusIn.class,
//                "Bus merger change. after: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
//                        "hiImpedance:{})\",",
//                newState,
//                getName(),
//                state,
//                hiImpedance,
//                merger.getName(),
//                merger.state,
//                merger.strongPins,
//                merger.weakState,
//                merger.weakPins,
//                merger.hiImpedance);
    }

    @Override
    public void setHiImpedance() {
//        assert Log.debug(BusMergerBusIn.class,
//                "Bus merger setImpedance. before: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
//                        "hiImpedance:{})\",",
//                getName(),
//                state,
//                hiImpedance,
//                merger.getName(),
//                merger.state,
//                merger.strongPins,
//                merger.weakState,
//                merger.weakPins,
//                merger.hiImpedance);
//        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        long oldState = merger.state;
        merger.strongPins &= nMask;
        merger.state &= nMask;
        merger.state |= merger.weakState & mask;
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                for (Bus destination : destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            for (Bus destination : destinations) {
                destination.setState(merger.state);
            }
            merger.hiImpedance = false;
        }
        hiImpedance = true;
        oldImpedance = true;
//        assert Log.debug(BusMergerBusIn.class,
//                "Bus merger setImpedance. after: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
//                        "hiImpedance:{})\",",
//                getName(),
//                state,
//                hiImpedance,
//                merger.getName(),
//                merger.state,
//                merger.strongPins,
//                merger.weakState,
//                merger.weakPins,
//                merger.hiImpedance);
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else if (!oldImpedance) {
            setHiImpedance();
        }
    }

    @Override
    public BusMergerBusIn getOptimised() {
        //FixMe need replace in merger.sources
        destinations = merger.destinations;
        return new ClassOptimiser<>(this).unroll(merger.destinations.length).build();
    }
}
