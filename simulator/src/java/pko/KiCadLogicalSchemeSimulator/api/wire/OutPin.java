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
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.wire.NCWire;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class OutPin extends Pin {
    public Pin[] destinations = new Pin[0];
    public int weakState;
    public boolean processing;
    public boolean hasQueue;

    public OutPin(String id, SchemaPart parent) {
        super(id, parent);
    }

    /*Optimiser constructor unroll destination:destinations*/
    public OutPin(OutPin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    public void addDestination(Pin pin) {
        assert pin != this;
        pin.used = true;
        pin.triState = triState;
        if (!(pin instanceof NCWire)) {
            destinations = Utils.addToArray(destinations, pin);
        }
    }

    @Override
    public void setState(boolean newState) {
        state = newState;
        /*Optimiser block allRecurse*/
        if (processing) {
            /*Optimiser line recurse*/
            if (hasQueue) {
                if (recurseError()) {
                    return;
                }
                /*Optimiser block recurse*/
            }
            hasQueue = true;
            /*Optimiser blockEnd recurse*/
        } else {
            processing = true;
            /*Optimiser blockEnd allRecurse*/
            for (Pin destination : destinations) {
                destination.setState(newState);
            }
            /*Optimiser block recurse block allRecurse*/
            while (hasQueue) {
                hasQueue = false;
                for (Pin destination : destinations) {
                    destination.setState(state);
                }
            }
            /*Optimiser blockEnd recurse*/
            processing = false;
        }
        /*Optimiser blockEnd allRecurse*/
    }

    @Override
    public void setHiImpedance() {
        throw new RuntimeException("setImpedance on non tri-state OutPin");
    }

    public void resend() {
        if (!hiImpedance) {
            setState(state);
        }
    }

    @Override
    public Pin getOptimised(boolean keepSetters) {
        if (destinations.length == 0) {
            return new NCWire(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised(true).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(false);
            }
            ClassOptimiser<OutPin> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
            if (Simulator.noRecursive) {
                optimiser.cut("allRecurse");
            } else if (!Simulator.recursive && Utils.notContain(Simulator.recursiveOuts, getName())) {
                optimiser.cut("recurse");
            }
            return optimiser.build();
        }
    }
}
