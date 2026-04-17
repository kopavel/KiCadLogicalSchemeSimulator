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
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.SupportMask;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

//FixMe where recursion support?
public class WireMergerBusIn extends InBus implements MergerInput<Bus>, SupportMask {
    public final WireMerger merger;
    public Pin[] destinations;
    public int maskState;

    public WireMergerBusIn(Bus source, WireMerger merger) {
        super(source, "PMergeBIn");
        this.merger = merger;
        destinations = merger.destinations;
        triStateIn = true;
    }

    @SuppressWarnings("unused")
    /*Optimiser constructor unroll destination:destinations*///
    public WireMergerBusIn(WireMergerBusIn oldBus, String variantId) {
        super(oldBus, variantId);
        merger = oldBus.merger;
        destinations = oldBus.destinations;
    }

    @Override
    public int getMask() {
        return applyMask;
    }

    @Override
    public void setState(int newState) {
        WireMerger merger = this.merger;
        //region assert
        assert Log.debug(getClass(),
                "Pin merger change. before: newState:{}, Source:{} (state:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        //endregion
        /*Optimiser line setters*/
        state = newState;
        /*Optimiser line o block byMask*/
        if (applyMask != 0) {
            /*Optimiser bind gm:applyMask*/
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
                //region shortcut
                if (parent.net.stabilizing) {
                    parent.net.forResend.add(this);
                    assert Log.debug(getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    throw new ShortcutException(merger.sources);
                }
                //endregion
            }
        }
        /*Optimiser blockEnd ts*/
        if (merger.state == (newState == 0)
                /*Optimiser line ts*///
                || merger.hiImpedance//
        ) { // merger state changes or from hiImpedance
            /*Optimiser line ts*/
            merger.hiImpedance = false;
            if (merger.state) {
                merger.state = false;
                for (Pin destination : destinations) {
                    destination.setLo();
                }
            } else {
                merger.state = true;
                for (Pin destination : destinations) {
                    destination.setHi();
                }
            }
            /*Optimiser block passivePins*/
            merger.strong = true;
        } else if (!merger.strong) {
            merger.strong = true;
            /*Optimiser blockEnd passivePins*/
        }
        /*Optimiser line passivePins*/
        merger.recalculatePassivePins();
        //region assert
        assert Log.debug(getClass(),
                "Pin merger change. after: newState:{}, Source:{} (state:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        //endregion
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        WireMerger merger = this.merger;
        //region assert
        assert Log.debug(getClass(),
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
        //endregion
        /*Optimiser line weakOnly block strongOnly*/
        if (merger.weakState == 0) {
            for (Pin destination : destinations) {
                destination.setHiImpedance();
            }
            merger.hiImpedance = true;
            /*Optimiser  block weakOnly*/
        } else {
            /*Optimiser blockEnd strongOnly line passivePins*/
            merger.strong = false;
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
            /*Optimiser line strongOnly*/
        }
        /*Optimiser line passivePins blockEnd weakOnly*/
        merger.recalculatePassivePins();
        hiImpedance = true;
        //region assert
        assert Log.debug(getClass(),
                "Pin merger setImpedance. after: Source:{} (state:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                getName(),
                state,
                hiImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        //endregion
        /*Optimiser blockEnd ts*/
    }

    @Override
    public WireMergerBusIn getOptimised(ModelItem<?> source) {
        merger.sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(merger);
            triStateIn = triStateIn || destinations[i].triStateIn;
        }
        ClassOptimiser<WireMergerBusIn> optimiser = new ClassOptimiser<>(this).unroll(destinations.length).cut("o");
        if (merger.passivePins.isEmpty()) {
            optimiser.cut("passivePins");
            if (merger.weakState == 0) {
                optimiser.cut("weakOnly");
            } else {
                optimiser.cut("strongOnly");
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
