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
package pko.KiCadLogicalSchemeSimulator.net.bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class NegativeOffsetBus extends OffsetBus {
    public NegativeOffsetBus(OutBus outBus, Bus destination, byte offset) {
        super(outBus, destination, (byte) -offset);
        triState = outBus.triState;
        if (offset == 0) {
            throw new RuntimeException("Offset must not be 0");
        }
        variantId += ":N";
    }

    /*Optimiser constructor unroll destination:destinations*/
    public NegativeOffsetBus(NegativeOffsetBus oldBus, String variantId) {
        super(oldBus, variantId);
        triState = oldBus.triState;
    }

    @Override
    public void setState(long newState) {
        /*Optimiser block setters block iSetter*/
        hiImpedance = false;
        /*Optimiser blockend iSetter*/
        state = newState;
        /*Optimiser blockend setters*/
        for (Bus destination : destinations) {
            /*Optimiser bind offset*/
            destination.setState(newState >> offset);
        }
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public void setHiImpedance() {
        /*Optimiser block setters block iSetter*/
        hiImpedance = true;
        /*Optimiser blockend iSetter blockend setters*/
        for (Bus destination : destinations) {
            destination.setHiImpedance();
        }
    }

    @Override
    public Bus getOptimised(boolean keepSetters) {
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(false);
        }
        ClassOptimiser<NegativeOffsetBus> optimiser = new ClassOptimiser<>(this).unroll(destinations.length).bind("offset", offset);
        if (!keepSetters) {
            optimiser.cut("setters");
        }
        if (!triState) {
            optimiser.cut("iSetter");
        }
        return optimiser.build();
    }
}
