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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.net.wire.NCWire;
import pko.KiCadLogicalSchemeSimulator.net.wire.WireToBusAdapter;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

/*Optimiser iterator destinations->destination*/
public class OutPin extends Pin {
    public Pin[] destinations = new Pin[0];

    public OutPin(String id, SchemaPart parent) {
        super(id, parent);
    }

    public OutPin(OutPin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    public void addDestination(Pin pin) {
        if (pin != this) {
            if (destinations.length == 1 && destinations[0] instanceof PassivePin passivePin) {
                passivePin.addDestination(pin);
            } else {
                destinations = Utils.addToArray(destinations, pin);
            }
            if (pin instanceof PassivePin passivePin) {
                passivePin.source = this;
            }
        }
    }

    public void addDestination(Bus bus, byte offset) {
        //FixMe group by offset
        destinations = Utils.addToArray(destinations, new WireToBusAdapter(bus, offset));
    }

    /*Optimiser override*/
    @Override
    public void setState(boolean newState) {
        /*Optimiser iterator unroll*/
        for (Pin destination : destinations) {
            destination.setState(state);
        }
    }

    /*Optimiser override*/
    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this;
        /*Optimiser iterator unroll*/
        for (Pin destination : destinations) {
            destination.setHiImpedance();
        }
    }

    public void resend() {
        if (!hiImpedance) {
            setState(state);
        }
    }

    @Override
    public Pin getOptimised() {
        if (destinations.length == 0) {
            return new NCWire(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised().copyState(this);
        } else if (parent.id.equals("javac")) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            Pin d5 = destinations[4].getOptimised();
            return new OutPin(this, "unroll5") {
                @Override
                public void setState(boolean newState) {
                    d1.setState(state);
                    d2.setState(state);
                    d3.setState(state);
                    d4.setState(state);
                    d5.setState(state);
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                }
            };
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return new ClassOptimiser(OutPin.class).unroll(destinations.length).build(this);
        }
    }
}
