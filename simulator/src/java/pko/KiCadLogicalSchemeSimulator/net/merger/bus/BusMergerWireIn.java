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
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class BusMergerWireIn extends InPin implements MergerInput<Pin> {
    public final long mask;
    public final long nMask;
    private final BusMerger merger;
    public boolean oldStrong;
    public boolean oldImpedance;
    Bus[] destinations;

    public BusMergerWireIn(long mask, BusMerger merger) {
        super(merger.id + ":in", merger.parent);
        variantId = "BMergePIn";
        this.mask = mask;
        nMask = ~mask;
        this.merger = merger;
        destinations = merger.destinations;
    }

    @Override
    public void setState(boolean newState) {
/*
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger change. before: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
*/
        long oldState = merger.state;
        state = newState;
        hiImpedance = false;
        if (strong) { //to strong
            if (oldImpedance) { //from hiImpedance
                if ((merger.strongPins & mask) != 0) { //strong pins shortcut
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                        return;
                    } else {
                        throw new ShortcutException(merger.sources);
                    }
                }
                merger.strongPins |= mask;
            } else if (!oldStrong) { // from weak
                merger.strongPins |= mask;
                merger.weakState &= nMask;
                merger.weakPins &= nMask;
            }
            if (state) {
                merger.state |= mask;
            } else {
                merger.state &= nMask;
            }
        } else { //to weak
            if ((merger.weakPins & mask) != 0 && ((merger.weakState & mask) == 0) == state) { //opposite weak state
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this);
                    return;
                } else {
                    throw new ShortcutException(merger.sources);
                }
            }
            if (oldImpedance) { // from impedance
                merger.weakPins |= mask;
            } else if (oldStrong) { //from strong
                merger.strongPins &= nMask;
                merger.weakPins |= mask;
            }
            if ((merger.strongPins & mask) == 0) {
                if (state) {
                    merger.state |= mask;
                } else {
                    merger.state &= nMask;
                }
            }
        }
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                for (Bus destination : destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            merger.hiImpedance = false;
            for (Bus destination : destinations) {
                destination.setState(merger.state);
            }
        }
        oldStrong = strong;
        oldImpedance = false;
/*
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger change. after: newState:{},  Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, " +
                        "weakState:{}, weakPins:{}, hiImpedance:{})",
                newState,
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
*/
    }

    @Override
    public void setHiImpedance() {
/*
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger setImpedance. before: Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})",
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
*/
//        assert !hiImpedance : "Already in hiImpedance:" + this + "; merger=" + merger.getName();
        long oldState = merger.state;
        if (oldStrong) {
            merger.strongPins &= nMask;
            merger.state &= nMask;
            merger.state |= merger.weakState & mask;
        } else {
            merger.weakPins &= nMask;
            if ((merger.strongPins & mask) != 0) {
                merger.state &= nMask;
            }
        }
        if ((merger.strongPins | merger.weakPins) != merger.mask) {
            if (!merger.hiImpedance) {
                for (Bus destination : destinations) {
                    destination.setHiImpedance();
                }
                merger.hiImpedance = true;
            }
        } else if (oldState != merger.state || merger.hiImpedance) {
            merger.hiImpedance = false;
            for (Bus destination : destinations) {
                destination.setState(merger.state);
            }
        }
        hiImpedance = true;
/*
        assert Log.debug(BusMergerWireIn.class,
                "Bus merger setImpedance. after: Source:{} (state:{}, strong:{}, hiImpedance:{}), Merger:{} (state:{}, strongPins:{}, weakState:{}, weakPins:{}, " +
                        "hiImpedance:{})",
                getName(),
                state,
                strong,
                hiImpedance,
                merger.getName(),
                merger.state,
                merger.strongPins,
                merger.weakState,
                merger.weakPins,
                merger.hiImpedance);
*/
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
    public BusMergerWireIn getOptimised() {
        destinations = merger.destinations;
        //FixMe need replace in merger.sources
        return this;
    }
}
