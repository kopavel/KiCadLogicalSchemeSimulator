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
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class BusToWiresAdapter extends OutBus {
    public Pin[] destinations = new Pin[0];
    public long maskState;

    public BusToWiresAdapter(BusToWiresAdapter oldBus, String variantId) {
        super(oldBus, variantId);
    }

    public BusToWiresAdapter(OutBus outBus, long mask) {
        super(outBus, "BusToWire");
        this.mask = mask;
    }

    @Override
    public void setState(long newState) {
        if (maskState != (newState & mask)) {
            maskState = newState & mask;
            for (Pin destination : destinations) {
                destination.setState(maskState != 0);
            }
        }
    }

    @Override
    public void setHiImpedance() {
        assert !hiImpedance : "Already in hiImpedance:" + this;
        for (Pin destination : destinations) {
            destination.setHiImpedance();
        }
    }

    public void addDestination(Pin pin) {
        destinations = Utils.addToArray(destinations, pin);
    }

    @Override
    public BusToWiresAdapter getOptimised() {
        if (destinations.length == 10) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            Pin d5 = destinations[4].getOptimised();
            Pin d6 = destinations[5].getOptimised();
            Pin d7 = destinations[6].getOptimised();
            Pin d8 = destinations[7].getOptimised();
            Pin d9 = destinations[8].getOptimised();
            Pin d10 = destinations[9].getOptimised();
            return new BusToWiresAdapter(this, "unroll10") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                        d4.setState(newOutState);
                        d5.setState(newOutState);
                        d6.setState(newOutState);
                        d7.setState(newOutState);
                        d8.setState(newOutState);
                        d9.setState(newOutState);
                        d10.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                    d6.setHiImpedance();
                    d7.setHiImpedance();
                    d8.setHiImpedance();
                    d9.setHiImpedance();
                    d10.setHiImpedance();
                }
            };
        } else if (destinations.length == 9) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            Pin d5 = destinations[4].getOptimised();
            Pin d6 = destinations[5].getOptimised();
            Pin d7 = destinations[6].getOptimised();
            Pin d8 = destinations[7].getOptimised();
            Pin d9 = destinations[8].getOptimised();
            return new BusToWiresAdapter(this, "unroll9") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                        d4.setState(newOutState);
                        d5.setState(newOutState);
                        d6.setState(newOutState);
                        d7.setState(newOutState);
                        d8.setState(newOutState);
                        d9.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                    d6.setHiImpedance();
                    d7.setHiImpedance();
                    d8.setHiImpedance();
                    d9.setHiImpedance();
                }
            };
        } else if (destinations.length == 8) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            Pin d5 = destinations[4].getOptimised();
            Pin d6 = destinations[5].getOptimised();
            Pin d7 = destinations[6].getOptimised();
            Pin d8 = destinations[7].getOptimised();
            return new BusToWiresAdapter(this, "unroll8") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                        d4.setState(newOutState);
                        d5.setState(newOutState);
                        d6.setState(newOutState);
                        d7.setState(newOutState);
                        d8.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                    d6.setHiImpedance();
                    d7.setHiImpedance();
                    d8.setHiImpedance();
                }
            };
        } else if (destinations.length == 7) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            Pin d5 = destinations[4].getOptimised();
            Pin d6 = destinations[5].getOptimised();
            Pin d7 = destinations[6].getOptimised();
            return new BusToWiresAdapter(this, "unroll7") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                        d4.setState(newOutState);
                        d5.setState(newOutState);
                        d6.setState(newOutState);
                        d7.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                    d6.setHiImpedance();
                    d7.setHiImpedance();
                }
            };
        } else if (destinations.length == 6) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            Pin d5 = destinations[4].getOptimised();
            Pin d6 = destinations[5].getOptimised();
            return new BusToWiresAdapter(this, "unroll6") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                        d4.setState(newOutState);
                        d5.setState(newOutState);
                        d6.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                    d5.setHiImpedance();
                    d6.setHiImpedance();
                }
            };
        } else if (destinations.length == 5) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            Pin d5 = destinations[4].getOptimised();
            return new BusToWiresAdapter(this, "unroll5") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                        d4.setState(newOutState);
                        d5.setState(newOutState);
                    }
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
        } else if (destinations.length == 4) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            Pin d4 = destinations[3].getOptimised();
            return new BusToWiresAdapter(this, "unroll4") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                        d4.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                    d4.setHiImpedance();
                }
            };
        } else if (destinations.length == 3) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            Pin d3 = destinations[2].getOptimised();
            return new BusToWiresAdapter(this, "unroll3") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                        d3.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                    d3.setHiImpedance();
                }
            };
        } else if (destinations.length == 2) {
            Pin d1 = destinations[0].getOptimised();
            Pin d2 = destinations[1].getOptimised();
            return new BusToWiresAdapter(this, "unroll2") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask)) {
                        maskState = newState & mask;
                        boolean newOutState = maskState != 0;
                        d1.setState(newOutState);
                        d2.setState(newOutState);
                    }
                }

                @Override
                public void setHiImpedance() {
                    d1.setHiImpedance();
                    d2.setHiImpedance();
                }
            };
        } else if (destinations.length == 1) {
            Pin d1 = destinations[0].getOptimised();
            return new BusToWiresAdapter(this, "unroll1") {
                @Override
                public void setState(long newState) {
                    if (maskState != (newState & mask) || hiImpedance) {
                        maskState = newState & mask;
                        hiImpedance = false;
                        d1.setState(maskState != 0);
                    }
                }

                @Override
                public void setHiImpedance() {
                    hiImpedance = true;
                    d1.setHiImpedance();
                }
            };
        } else {
            Log.warn(BusToWiresAdapter.class, "No unroll instance for {} items", destinations.length);
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return this;
        }
    }
}
