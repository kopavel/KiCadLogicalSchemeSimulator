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
package pko.KiCadLogicalSchemeSimulator.net.merger.bus;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

//Todo with one Bus input and only weak others - use simpler "Weak bus" implementation
public class BusMergerBusIn extends InBus implements MergerInput<Bus> {
    @Getter
    public final int mask;
    public final int nMask;
    public final BusMerger merger;
    public Bus[] destinations;
    public int maskState;

    public BusMergerBusIn(Bus source, int mask, BusMerger merger) {
        super(source, "BMergeBIn");
        this.mask = mask;
        this.merger = merger;
        nMask = ~mask;
        destinations = merger.destinations;
        triStateIn = true;
        triStateOut = source.triStateOut;
        source.used = true;
        used = true;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public BusMergerBusIn(BusMergerBusIn oldPin, String variantId) {
        super(oldPin, variantId);
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        merger = oldPin.merger;
        destinations = merger.destinations;
    }

    @Override
    public void setState(int newState) {
        BusMerger merger = this.merger;
        /*Optimiser line setter*/
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
        /*Optimiser line o*/
        if (applyOffset > 0) {
            /*Optimiser line byOffset line positive bind o:applyOffset*/
            newState = newState << applyOffset;
            /*Optimiser line o*/
        } else if (applyOffset < 0) {
            /*Optimiser line byOffset line negative bind o:-applyOffset*/
            newState = newState >> -applyOffset;
            /*Optimiser line o*/
        }
        assert Log.debug(this.getClass(),
                "Bus merger change. before: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                newState,
                getName(),
                getState(),
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        /*Optimiser line o block sameMask block ts*/
        if (merger.mask == mask) {
            if (hiImpedance) {
                hiImpedance = false;
                if (merger.strongPins != 0) {
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser bind m:mask*/
                merger.strongPins = mask;
            }
            /*Optimiser blockEnd ts*/
            if (newState != merger.state) {
                merger.state = newState;
                /*Optimiser block ar*/
                switch (processing++) {
                    case 0: {
                        /*Optimiser blockEnd ar*/
                        for (Bus destination : destinations) {
                            destination.setState(newState);
                        }
                        /*Optimiser block r block ar*/
                        while (--processing > 0) {
                            for (Bus destination : destinations) {
                                destination.setState(merger.state);
                            }
                        }
                        /*Optimiser line nr blockEnd r*/
                        processing = 0;
                        return;
                    }
                    case 2: {
                        recurseError();
                        return;
                    }
                }
                /*Optimiser blockEnd ar*/
            }
            /*Optimiser blockEnd sameMask block otherMask line o*/
        } else {
            int mergerState;
            /*Optimiser block ts*/
            if (hiImpedance) {
                hiImpedance = false;
                /*Optimiser bind m:mask*/
                if ((merger.strongPins & mask) != 0) {
                    if (parent.net.stabilizing) {
                        parent.net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(merger.sources);
                    }
                }
                /*Optimiser bind m:mask*/
                merger.strongPins |= mask;
            }
            /*Optimiser blockEnd ts bind nm:nMask*/
            if ((mergerState = merger.state & nMask | newState) != merger.state) {
                merger.state = mergerState;
                /*Optimiser block ar*/
                switch (processing++) {
                    case 0: {
                        /*Optimiser blockEnd ar*/
                        for (Bus destination : destinations) {
                            destination.setState(mergerState);
                        }
                        /*Optimiser block r block ar*/
                        while (--processing > 0) {
                            for (Bus destination : destinations) {
                                destination.setState(merger.state);
                            }
                        }
                        /*Optimiser line nr blockEnd r*/
                        processing = 0;
                        return;
                    }
                    case 2: {
                        recurseError();
                        return;
                    }
                }
                /*Optimiser blockEnd ar*/
            }
            /*Optimiser blockEnd otherMask line o*/
        }
        assert Log.debug(this.getClass(),
                "Bus merger change. after: newState:{}, Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                newState,
                getName(),
                getState(),
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
    }

    @Override
    public void setHiImpedance() {
        /*Optimiser block ts*/
        assert !hiImpedance : "Already in hiImpedance:" + this;
        hiImpedance = true;
        BusMerger merger = this.merger;
        int newState;
        assert Log.debug(this.getClass(),
                "Bus merger setImpedance. before: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                getName(),
                getState(),
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        /*Optimiser block sameMask line o*/
        if (mask == merger.mask) {
            merger.strongPins = 0;
            if ((newState = merger.weakState) != merger.state) {
                merger.state = newState;
                /*Optimiser block ar*/
                switch (processing++) {
                    case 0: {
                        /*Optimiser blockEnd ar*/
                        for (Bus destination : destinations) {
                            destination.setState(newState);
                        }
                        /*Optimiser block r block ar*/
                        while (--processing > 0) {
                            for (Bus destination : destinations) {
                                destination.setState(merger.state);
                            }
                        }
                        /*Optimiser line nr blockEnd r*/
                        processing = 0;
                        return;
                    }
                    case 2: {
                        recurseError();
                        return;
                    }
                }
                /*Optimiser blockEnd ar*/
            }
            /*Optimiser blockEnd sameMask block otherMask line o*/
        } else {
            /*Optimiser bind nm:nMask*/
            merger.strongPins &= nMask;
            int mergerState;
            /*Optimiser bind m:mask bind nm:nMask*/
            if ((mergerState = merger.state & nMask | (merger.weakState & mask)) != merger.state) {
                merger.state = mergerState;
                /*Optimiser block ar*/
                switch (processing++) {
                    case 0: {
                        /*Optimiser blockEnd ar*/
                        for (Bus destination : destinations) {
                            destination.setState(mergerState);
                        }
                        /*Optimiser block r block ar*/
                        while (--processing > 0) {
                            for (Bus destination : destinations) {
                                destination.setState(merger.state);
                            }
                        }
                        /*Optimiser line nr blockEnd r*/
                        processing = 0;
                        return;
                    }
                    case 2: {
                        recurseError();
                        return;
                    }
                }
                /*Optimiser blockEnd ar*/
            }
            /*Optimiser line o blockEnd otherMask*/
        }
        assert Log.debug(this.getClass(),
                "Bus merger setImpedance. after: Source:{} (state:{},  hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{})",
                getName(),
                getState(),
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins);
        /*Optimiser blockEnd ts*/
    }

    @Override
    public BusMergerBusIn getOptimised(ModelItem<?> source) {
        //ToDo in case of "no passive pin" weakPins/weakState are known after build phase (incomplete)
        merger.sources.remove(this);
        destinations = merger.destinations;
        ClassOptimiser<BusMergerBusIn> optimiser = new ClassOptimiser<>(this).cut("o").unroll(merger.destinations.length).bind("m", mask);
        if (mask == merger.mask) {
            optimiser.cut("otherMask");
        } else {
            optimiser.cut("sameMask").bind("nm", nMask);
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!triStateOut) {
            optimiser.cut("ts");
        } else {
            merger.triStateOut = true;
        }
        if (applyMask == 0) {
            optimiser.cut("byMask");
        } else {
            optimiser.bind("gm", applyMask);
        }
        if (applyOffset == 0) {
            optimiser.cut("byOffset");
        } else if (applyOffset > 0) {
            optimiser.cut("negative").bind("o", applyOffset);
        } else {
            optimiser.cut("positive").bind("o", -applyOffset);
        }
        if (getRecursionMode() == none) {
            optimiser.cut("ar");
        } else if (getRecursionMode() == warn) {
            optimiser.cut("r");
        } else {
            optimiser.cut("nr");
        }
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised(merger);
        }
        BusMergerBusIn build = optimiser.build();
        merger.sources.add(build);
        build.source = source;
        return build;
    }

    @Override
    public boolean useFullOptimiser() {
        return true;
    }
}
