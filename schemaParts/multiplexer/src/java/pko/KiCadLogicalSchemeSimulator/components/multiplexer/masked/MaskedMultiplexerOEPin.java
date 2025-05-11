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
package pko.KiCadLogicalSchemeSimulator.components.multiplexer.masked;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class MaskedMultiplexerOEPin extends InPin {
    public final MaskedMultiplexer parent;
    public final int mask;
    public final int nMask;
    public Bus outBus;
    public final Bus[] inBuses;

    public MaskedMultiplexerOEPin(String id, MaskedMultiplexer parent, int mask) {
        super(id, parent);
        this.parent = parent;
        this.mask = mask;
        nMask = ~mask;
        outBus = parent.getOutBus("Q");
        inBuses = parent.inBuses;
    }

    /*Optimiser constructor*/
    public MaskedMultiplexerOEPin(MaskedMultiplexerOEPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        outBus = oldPin.outBus;
        inBuses = oldPin.inBuses;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        int state;
        int lMask;
        MaskedMultiplexer parent;
        /*Optimiser block r line o*/
        if (this.parent.reverse) {
            /*Optimiser bind nm:nMask*/
            lMask = ((parent = this.parent).outMask &= nMask);
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser bind m:mask*/
            lMask = ((parent = this.parent).outMask |= mask);
            /*Optimiser line o blockEnd nr*/
        }
        if (outBus.state != (state = (inBuses[parent.nState].state & lMask))) {
            outBus.setState(state);
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = true;
        int state;
        int lMask;
        MaskedMultiplexer parent;
        /*Optimiser block r line o*/
        if (this.parent.reverse) {
            /*Optimiser bind m:mask*/
            lMask = ((parent = this.parent).outMask |= mask);
            /*Optimiser line o blockEnd r block nr*/
        } else {
            /*Optimiser bind nm:nMask*/
            lMask = (parent = this.parent).outMask &= nMask;
            /*Optimiser line o blockEnd nr*/
        }
        if (outBus.state != (state = (inBuses[parent.nState].state & lMask))) {
            outBus.setState(state);
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MaskedMultiplexerOEPin> optimiser = new ClassOptimiser<>(this).bind("m", mask).bind("nm", nMask).cut("o");
        if (source != null) {
            optimiser.cut("setter");
        }
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        MaskedMultiplexerOEPin build = optimiser.build();
        build.source = source;
        parent.replaceIn(this, build);
        if (parent.oePin == this) {
            parent.oePin = build;
        } else {
            parent.oePins.remove(this);
            parent.oePins.add(build);
        }
        return build;
    }
}
