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
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class SimpleBusToWireAdapter extends OutBus {
    public Pin destination;

    public SimpleBusToWireAdapter(OutBus parent, Pin destination) {
        super(parent, "BusToWire");
        this.destination = destination;
        triState = parent.triState;
    }

    /*Optimiser constructor*/
    public SimpleBusToWireAdapter(SimpleBusToWireAdapter oldPin, String variantId) {
        super(oldPin, variantId);
        destination = oldPin.destination;
        triState = oldPin.triState;
    }

    @Override
    public void setState(long newState) {
        /*Optimiser block setters block iSetter*/
        hiImpedance = false;
        /*Optimiser blockend iSetter*/
        state = newState;
        /*Optimiser blockend setters*/
        destination.setState(newState != 0);
    }

    /*Optimiser block iSetter*/
    @Override
    public void setHiImpedance() {
        /*Optimiser block setters*/
        hiImpedance = true;
        /*Optimiser blockend setters*/
        destination.setHiImpedance();
    }
    /*Optimiser blockend iSetter*/

    @Override
    public SimpleBusToWireAdapter getOptimised(boolean keepSetters) {
        destination = destination.getOptimised(false);
        ClassOptimiser<SimpleBusToWireAdapter> optimiser = new ClassOptimiser<>(this);
        if (!keepSetters) {
            optimiser.cut("setters");
        }
        if (!triState) {
            optimiser.cut("iSetter");
        }
        return optimiser.build();
    }
}
