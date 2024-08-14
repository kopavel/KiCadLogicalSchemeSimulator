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
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class OffsetBus extends OutBus {
    protected byte offset;

    public OffsetBus(OutBus outBus, CorrectedInBus destination, byte offset) {
        super(outBus, "offset" + offset);
        if (offset == 0) {
            throw new RuntimeException("Offset must not be 0");
        }
        destinations = new Bus[]{destination};
        this.offset = offset;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public OffsetBus(OffsetBus oldBus, String variantId) {
        super(oldBus, variantId);
        offset = oldBus.offset;
        destinations = oldBus.destinations;
    }

    @Override
    public void setState(long newState) {
        for (Bus destination : destinations) {
            /*Optimiser bind offset*/
            destination.setState(newState << offset);
        }
    }

    @Override
    public void setHiImpedance() {
        for (Bus destination : destinations) {
            destination.setHiImpedance();
        }
    }

    public void addDestination(Bus item) {
        destinations = Utils.addToArray(destinations, item);
    }

    @Override
    public Bus getOptimised() {
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised();
        }
        return new ClassOptimiser<>(this).unroll(destinations.length).bind("offset", offset).build();
    }
}
