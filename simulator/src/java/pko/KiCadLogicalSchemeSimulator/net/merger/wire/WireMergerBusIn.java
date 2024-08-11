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
package pko.KiCadLogicalSchemeSimulator.net.merger.wire;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.util.Arrays;

public class WireMergerBusIn extends CorrectedInBus implements MergerInput<Bus> {
    public long mask;
    public WireMerger merger;
    public Pin[] destinations;
    public boolean oldImpedance;

    public WireMergerBusIn(Bus source, long mask, WireMerger merger) {
        super(source, "PMergeBIn");
        this.mask = mask;
        this.merger = merger;
        oldImpedance = hiImpedance;
        destinations = merger.destinations;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public WireMergerBusIn(WireMergerBusIn oldPin, String variantId) {
        super(oldPin, variantId);
        this.mask = oldPin.mask;
        this.merger = oldPin.merger;
        oldImpedance = hiImpedance;
        destinations = merger.destinations;
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
        hiImpedance = false;
        if (oldImpedance
                /*Optimiser block weak*///
                && merger.strong
            /*Optimiser blockend weak*///
        ) { // merger not in hiImpedance or weak
            if (Net.stabilizing) {
                Net.forResend.add(this);
                assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                return;
            } else {
                throw new ShortcutException(merger.sources);
            }
        }
        if (merger.state == (state == 0)) { // merger state changes
            merger.state = state != 0;
            for (Pin destination : destinations) {
                destination.setState(merger.state);
            }
        } else if (merger.hiImpedance
                /*Optimiser block weak*///
                /*Optimiser block passive*///
                || (destinations[0] instanceof PassivePin && !merger.strong)
            /*Optimiser blockend weak*///
            /*Optimiser blockend passive*///
        ) {
            for (Pin destination : destinations) {
                destination.setState(merger.state);
            }
        }
        merger.strong = true;
        merger.hiImpedance = false;
        oldImpedance = false;
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
        /*Optimiser block weak*/
        if (merger.hasWeak) {
            if (merger.state != merger.weakState) {
                merger.state = merger.weakState;
                for (Pin destination : destinations) {
                    destination.setState(merger.weakState);
                }
            }
        } else {
            /*Optimiser blockend weak*/
            for (Pin destination : destinations) {
                destination.setHiImpedance();
            }
            merger.hiImpedance = true;
            /*Optimiser block weak*/
        }
        merger.strong = false;
        /*Optimiser blockend weak*/
        hiImpedance = true;
        oldImpedance = true;
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
    public WireMergerBusIn getOptimised() {
        merger.sources.remove(this);
        destinations = merger.destinations;
        ClassOptimiser<WireMergerBusIn> optimiser = new ClassOptimiser<>(this).unroll(merger.destinations.length);
        if (!merger.hasWeak) {
            optimiser.cut("weak");
        }
        if (Arrays.stream(destinations)
                .noneMatch(d -> d instanceof PassivePin)) {
            optimiser.cut("passive");
        }
        WireMergerBusIn optimised = optimiser.build();
        merger.sources.add(optimised);
        return optimised;
    }
}
