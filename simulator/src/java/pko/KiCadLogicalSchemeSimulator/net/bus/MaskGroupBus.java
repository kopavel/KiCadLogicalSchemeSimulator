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
import pko.KiCadLogicalSchemeSimulator.net.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

/*Optimiser iterator destinations->destination*/
public class MaskGroupBus extends OutBus {
    protected long maskState;

    public MaskGroupBus(OutBus source, long mask, String variantId) {
        super(source, variantId);
        this.mask = mask;
        id += ":mask" + mask;
    }

    public MaskGroupBus(MaskGroupBus source, String variantId) {
        super(source, variantId);
    }

    public void addDestination(Bus bus) {
        destinations = Utils.addToArray(destinations, bus);
    }

    /*Optimiser override*/
    @Override
    public void setState(long newState) {
        /*Optimiser bind mask*/
        final long maskState = newState & mask;
        /*Optimiser bind destinations[0] d*/
        if (this.maskState != maskState || destinations[0].hiImpedance) {
            this.maskState = maskState;
            /*Optimiser iterator unroll*/
            for (Bus destination : destinations) {
                destination.setState(maskState);
            }
        }
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser iterator unroll*/
        for (Bus destination : destinations) {
            destination.setHiImpedance();
        }
    }

    @Override
    public Bus getOptimised() {
        if (destinations.length == 0) {
            throw new RuntimeException("unconnected MaskGroupBus " + getName());
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return new ClassOptimiser(MaskGroupBus.class).unroll(destinations.length).bind("mask", String.valueOf(mask)).bind("d", "destination0").build(this);
        }
    }
}
