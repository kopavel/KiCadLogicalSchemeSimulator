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
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.wire.NCWire;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Arrays;
import java.util.Comparator;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

//FixMe - No optimiser on passive pin, sore strength only fot passive pin out.
public class OutPin extends Pin {
    public Pin[] destinations = new Pin[0];
    public Pin[] toImp = new Pin[0];
    public Pin[] toLow = new Pin[0];
    public Pin[] toHi = new Pin[0];

    public OutPin(String id, SchemaPart parent) {
        super(id, parent);
    }

    /*Optimiser constructor unroll low:toLow:l unroll hi:toHi:h unroll imp:toImp:i*/
    public OutPin(OutPin oldPin, String variantId) {
        super(oldPin, variantId);
    }

    public void addDestination(Pin pin) {
        assert pin != this;
        used = true;
        pin.used = true;
        pin.source = this;
        pin.state = state;
        pin.hiImpedance = hiImpedance;
        triStateIn = triStateIn || pin.triStateIn;
        if (!(pin instanceof NCWire)) {
            priority += pin.priority;
            destinations = Utils.addToArray(destinations, pin);
            split();
        } else {
            throw new RuntimeException("what??");
        }
    }

    @Override
    public void setHi() {
        /*Optimiser line ts*/
        hiImpedance = false;
        state = true;
        /*Optimiser block ar*/
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar*/
                for (Pin hi : toHi) {
                    hi.strong = strong;
                    hi.setHi();
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
                    /*Optimiser block ts*/
                    if (hiImpedance) {
                        for (Pin imp : toImp) {
                            imp.setHiImpedance();
                        }
                    } else {
                        /*Optimiser blockEnd ts*/
                        if (state) {
                            for (Pin hi : toHi) {
                                hi.strong = strong;
                                hi.setHi();
                            }
                        } else {
                            for (Pin low : toLow) {
                                low.strong = strong;
                                low.setLo();
                            }
                        }
                        /*Optimiser line ts*/
                    }
                }
                /*Optimiser line nr blockEnd r*/
                processing = 0;
                return;
            }
            case 2: {
                recurseError();
            }
        }
        /*Optimiser blockEnd ar*/
    }

    @Override
    public void setLo() {
        /*Optimiser line ts*/
        hiImpedance = false;
        state = false;
        /*Optimiser block ar*/
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar*/
                for (Pin low : toLow) {
                    low.strong = strong;
                    low.setLo();
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
                    /*Optimiser block ts*/
                    if (hiImpedance) {
                        for (Pin imp : toImp) {
                            imp.setHiImpedance();
                        }
                    } else {
                        /*Optimiser blockEnd ts*/
                        if (state) {
                            for (Pin hi : toHi) {
                                hi.strong = strong;
                                hi.setHi();
                            }
                        } else {
                            for (Pin low : toLow) {
                                low.strong = strong;
                                low.setLo();
                            }
                        }
                        /*Optimiser line ts*/
                    }
                }
                /*Optimiser line nr blockEnd r*/
                processing = 0;
                return;
            }
            case 2: {
                recurseError();
            }
        }
        /*Optimiser blockEnd ar*/
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        assert !hiImpedance : "Already in hiImpedance:" + this;
        hiImpedance = true;
        /*Optimiser block ar*/
        switch (processing++) {
            case 0: {
                /*Optimiser blockEnd ar*/
                for (Pin imp : toImp) {
                    imp.setHiImpedance();
                }
                /*Optimiser block r block ar*/
                while (--processing > 0) {
                    if (hiImpedance) {
                        for (Pin imp : toImp) {
                            imp.setHiImpedance();
                        }
                    } else {
                        if (state) {
                            for (Pin hi : toHi) {
                                hi.setHi();
                            }
                        } else {
                            for (Pin low : toLow) {
                                low.setLo();
                            }
                        }
                    }
                }
                /*Optimiser line nr blockEnd r*/
                processing = 0;
                return;
            }
            case 2: {
                recurseError();
            }
        }
        /*Optimiser blockEnd ar blockEnd ts*/
    }

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        if (destinations.length == 0) {
            return new NCWire(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised(source).copyState(this);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(this);
            }
            split();
            ClassOptimiser<OutPin> optimiser = new ClassOptimiser<>(this, OutPin.class).unroll("l", toLow.length).unroll("h", toHi.length);
            if (getRecursionMode() == none) {
                optimiser.cut("ar");
            } else if (getRecursionMode() == warn) {
                optimiser.cut("r");
            } else {
                optimiser.cut("nr");
            }
            if (isTriState(source)) {
                optimiser.unroll("i", toImp.length);
            } else {
                optimiser.cut("ts");
            }
            OutPin build = optimiser.build();
            build.source = source;
            for (Pin destination : destinations) {
                destination.source = build;
            }
            return build;
        }
    }

    protected void split() {
        toHi = new Pin[0];
        toLow = new Pin[0];
        toImp = new Pin[0];
        for (Pin destination : destinations) {
            if (!(destination instanceof FallingEdgePin)) {
                toHi = Utils.addToArray(toHi, destination);
                if (!(destination instanceof RaisingEdgePin)) {
                    toImp = Utils.addToArray(toImp, destination);
                }
            }
            if (!(destination instanceof RaisingEdgePin)) {
                toLow = Utils.addToArray(toLow, destination);
            }
        }
        Arrays.sort(toLow, Comparator.comparingInt(p -> p.priority));
        Arrays.sort(toHi, Comparator.comparingInt(p -> -p.priority));
    }
}
