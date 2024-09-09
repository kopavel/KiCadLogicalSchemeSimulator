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
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.CorrectedInBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class WireMergerBusIn extends CorrectedInBus implements MergerInput<Bus> {
    @Getter
    public long mask;
    public WireMerger merger;
    public Pin[] destinations;
    public boolean oldImpedance;

    public WireMergerBusIn(Bus source, long mask, WireMerger merger) {
        super(source, "PMergeBIn");
        this.mask = mask;
        this.merger = merger;
        oldImpedance = hiImpedance;
        destinations = merger.destinations;
    }

    /*Optimiser constructor unroll destination:destinations*/
    public WireMergerBusIn(WireMergerBusIn oldBus, String variantId) {
        super(oldBus, variantId);
        mask = oldBus.mask;
        merger = oldBus.merger;
        oldImpedance = hiImpedance;
        destinations = oldBus.destinations;
    }

    @Override
    public void setState(long newState) {
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
        state = newState;
        hiImpedance = false;
        if (oldImpedance && merger.strong) { // merger not in hiImpedance or weak
            if (Net.stabilizing) {
                Net.forResend.add(this);
                assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                return;
            } else {
                throw new ShortcutException(merger.sources);
            }
        }
        if (merger.state == (newState == 0)) { // merger state changes
            merger.state = newState != 0;
            for (Pin destination : destinations) {
                destination.setState(merger.state);
            }
        } else if (merger.hiImpedance) {
            for (Pin destination : destinations) {
                destination.setState(merger.state);
            }
            /*Optimiser block passivePins*/
        } else if (!merger.strong) {
            for (Pin destination : destinations) {
                destination.setState(merger.state);
            }
            /*Optimiser blockend passivePins*/
        }
        merger.strong = true;
        merger.hiImpedance = false;
        oldImpedance = false;
        /*Optimiser block passivePins*///
        for (PassivePin passivePin : merger.passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockend passivePins*///
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
        assert Log.debug(this.getClass(),
                "Pin merger setImpedance. before: Source:{} (state:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                getName(),
                state,
                oldImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        /*Optimiser block strongOnly*/
        /*Optimiser block weakOnly*/
        if (merger.weakState != 0) {
            /*Optimiser blockend weakOnly*/
            /*Optimiser blockend strongOnly*/
            /*Optimiser block weakOnly*/
            if (merger.state != (merger.weakState > 0)) {
                merger.state = merger.weakState > 0;
                for (Pin destination : destinations) {
                    destination.setState(merger.state);
                }
            }
            /*Optimiser blockend weakOnly*/
            /*Optimiser block strongOnly*/
            /*Optimiser block weakOnly*/
        } else {
            /*Optimiser blockend weakOnly*/
            /*Optimiser blockend strongOnly*/
            /*Optimiser block strongOnly*/
            for (Pin destination : destinations) {
                destination.setHiImpedance();
            }
            merger.hiImpedance = true;
            /*Optimiser blockend strongOnly*/
            /*Optimiser block strongOnly*/
            /*Optimiser block weakOnly*/
        }
        /*Optimiser blockend weakOnly*/
        /*Optimiser blockend strongOnly*/
        merger.strong = false;
        hiImpedance = true;
        oldImpedance = true;
        /*Optimiser block passivePins*///
        for (PassivePin passivePin : merger.passivePins) {
            passivePin.parent.onPassivePinChange(merger);
        }
        /*Optimiser blockend passivePins*///
        assert Log.debug(this.getClass(),
                "Pin merger setImpedance. after: Source:{} (state:{}, oldImpedance:{}, hiImpedance:{}), Merger:{} (state:{}, strong:{}, hiImpedance:{})",
                getName(),
                state,
                oldImpedance,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strong,
                merger.hiImpedance);
    }

    @Override
    public void resend() {
        if (!hiImpedance) {
            setState(state);
        } else if (!oldImpedance) {
            setHiImpedance();
        }
    }

    @Override
    public WireMergerBusIn getOptimised() {
        merger.sources.remove(this);
        destinations = merger.destinations;
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised();
        }
        ClassOptimiser<WireMergerBusIn> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
        if (merger.passivePins.isEmpty()) {
            optimiser.cut("passivePins");
            if (merger.weakState != 0) {
                optimiser.cut("strongOnly");
            } else {
                optimiser.cut("weakOnly");
            }
        }
        WireMergerBusIn pin = optimiser.build();
        merger.sources.add(pin);
        return pin;
    }
}
