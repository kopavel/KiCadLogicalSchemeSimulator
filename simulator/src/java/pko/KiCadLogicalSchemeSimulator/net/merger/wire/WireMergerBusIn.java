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
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.SupportMask;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

//FixMe where recursion support?
public class WireMergerBusIn extends InBus implements MergerInput<Bus>, SupportMask {
    public final WireMerger merger;
    @Getter
    public int mask;
    public Pin[] destinations;
    public int maskState;

    public WireMergerBusIn(Bus source, int mask, WireMerger merger) {
        super(source, "PMergeBIn");
        this.mask = mask;
        this.merger = merger;
        destinations = merger.destinations;
        triStateIn = true;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public WireMergerBusIn(WireMergerBusIn oldBus, String variantId) {
        super(oldBus, variantId);
        mask = oldBus.mask;
        merger = oldBus.merger;
        destinations = oldBus.destinations;
    }

    @Override
    public void setState(int newState) {
        WireMerger merger = this.merger;
        assert Log.debug(this.getClass(),
                "Pin merger change. before: newState:{}, Source:{} (state:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        /*Optimiser line setters*/
        state = newState;
        /*Optimiser line o*/
        if (applyMask != 0) {
            /*Optimiser block byMask bind gm:applyMask*/
            if (maskState != (newState = newState & applyMask)) {
                maskState = newState;
            } else//
                /*Optimiser line ts*///
                if (!hiImpedance)//
                {
                    return;
                }
            /*Optimiser line o blockEnd byMask*/
        }
        /*Optimiser block ts*/
        if (hiImpedance) {
            hiImpedance = false;
            if (merger.strong) { // merger not in hiImpedance or weak
                if (parent.net.stabilizing) {
                    parent.net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    throw new ShortcutException(merger.sources);
                }
            }
        }
        /*Optimiser blockEnd ts*/
        if (merger.state == (newState == 0)) { // merger state changes
            merger.state = newState != 0;
            if (merger.state) {
                for (Pin destination : destinations) {
                    destination.setHi();
                }
            } else {
                for (Pin destination : destinations) {
                    destination.setLo();
                }
            }
            /*Optimiser block ts*/
        } else if (merger.hiImpedance) {
            if (merger.state) {
                for (Pin destination : destinations) {
                    destination.setHi();
                }
            } else {
                for (Pin destination : destinations) {
                    destination.setLo();
                }
            }
            /*Optimiser block passivePins blockEnd ts*/
        } else if (!merger.strong) {
            if (merger.state) {
                for (Pin destination : destinations) {
                    destination.setHi();
                }
            } else {
                for (Pin destination : destinations) {
                    destination.setLo();
                }
            }
            /*Optimiser blockEnd passivePins*/
        }
        /*Optimiser line passivePins*/
        merger.strong = true;
        /*Optimiser line ts*/
        merger.hiImpedance = false;
        /*Optimiser block passivePins*///
        for (PassivePin passivePin : merger.passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockEnd passivePins*///
        assert Log.debug(this.getClass(),
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
        /*Optimiser block ts*/
        WireMerger merger = this.merger;
        assert Log.debug(this.getClass(),
                "Pin merger setImpedance. before: Source:{} (state:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                getName(),
                state,
                hiImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        assert !hiImpedance : "Already in hiImpedance:" + this;
        /*Optimiser line strongOnly block weakOnly*/
        if (merger.weakState != 0) {
            if (merger.state != (merger.weakState > 0)) {
                merger.state = merger.weakState > 0;
                if (merger.state) {
                    for (Pin destination : destinations) {
                        destination.setHi();
                    }
                } else {
                    for (Pin destination : destinations) {
                        destination.setLo();
                    }
                }
            }
            /*Optimiser block strongOnly*/
        } else {
            /*Optimiser blockEnd weakOnly*/
            for (Pin destination : destinations) {
                destination.setHiImpedance();
            }
            merger.hiImpedance = true;
            /*Optimiser line weakOnly*/
        }
        /*Optimiser blockEnd strongOnly line passivePins*/
        merger.strong = false;
        hiImpedance = true;
        /*Optimiser block passivePins*///
        for (PassivePin passivePin : merger.passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockEnd passivePins*///
        assert Log.debug(this.getClass(),
                "Pin merger setImpedance. after: Source:{} (state:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                getName(),
                state,
                hiImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        /*Optimiser blockEnd ts*/
    }

    @Override
    public WireMergerBusIn getOptimised(ModelItem<?> source) {
        merger.sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(merger);
            triStateIn |= destinations[i].triStateIn;
        }
        ClassOptimiser<WireMergerBusIn> optimiser = new ClassOptimiser<>(this).unroll(destinations.length).cut("o");
        if (merger.passivePins.isEmpty()) {
            optimiser.cut("passivePins");
            if (merger.weakState != 0) {
                optimiser.cut("strongOnly");
            } else {
                optimiser.cut("weakOnly");
            }
        }
        if (source != null) {
            optimiser.cut("setters");
        }
        if (!triStateIn) {
            optimiser.cut("ts");
        }
        if (applyMask == 0) {
            optimiser.cut("byMask");
        } else {
            optimiser.bind("gm", applyMask);
        }
        WireMergerBusIn build = optimiser.build();
        merger.sources.add(build);
        build.source = source;
        return build;
    }
}
