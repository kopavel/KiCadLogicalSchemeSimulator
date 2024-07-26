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
package pko.KiCadLogicalSchemeSimulator.model.bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MaskGroupBus extends OutBus {
    private long maskState;

    public MaskGroupBus(OutBus source, long mask, String id) {
        super(source, id);
        this.mask = mask;
    }

    public MaskGroupBus(MaskGroupBus source, String id) {
        super(source, id);
    }

    public void addDestination(Bus bus) {
        destinations = Utils.addToArray(destinations, bus);
    }

    @Override
    public void setState(long newState) {
        long maskState = newState & mask;
        if (this.maskState != maskState || hiImpedance) {
            this.maskState = maskState;
            hiImpedance = false;
            for (Bus destination : destinations) {
                destination.setState(maskState);
            }
        }
    }

    @Override
    public void setHiImpedance() {
        if (!hiImpedance) {
            super.setHiImpedance();
            hiImpedance = true;
        }
    }

    @Override
    public Bus getOptimised() {
        if (destinations.length == 0) {
            return new NCBus(this);
        } else if (destinations.length == 1) {
            Bus destination = destinations[0];
            if (destination instanceof BusToWireAdapter) {
                destination.state = state;
                destination.hiImpedance = hiImpedance;
                return destination;
            } else {
                return new MaskGroupBus(MaskGroupBus.this, "SingleDestination") {
                    @Override
                    public void setHiImpedance() {
                        if (!hiImpedance) {
                            destination.setHiImpedance();
                            hiImpedance = true;
                        }
                    }

                    @Override
                    public void setState(long newState) {
                        long newMaskState = newState & mask;
                        if (maskState != newMaskState || hiImpedance) {
                            maskState = newMaskState;
                            hiImpedance = false;
                            destination.setState(newMaskState);
                        }
                    }
                };
            }
        } else {
            for (int i = 0; i < destinations.length; i++) {
                if (destinations[i] instanceof BusToWireAdapter adapter) {
                    destinations[i] = new BusToWireAdapter(adapter.destination, 0) {
                        @Override
                        public void setState(long newState) {
                            destination.setState(newState != 0, true);
                        }
                    };
                }
            }
            return this;
        }
    }
}
