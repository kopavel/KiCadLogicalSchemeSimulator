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
package pko.KiCadLogicalSchemeSimulator.components.counter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class CInPin extends InPin {
    public final boolean reverse;
    public final long countMask;
    public Bus out;

    public CInPin(String id, Counter parent, boolean reverse, long countMask) {
        super(id, parent);
        out = parent.getOutBus("Q");
        this.reverse = reverse;
        this.countMask = countMask;
    }

    /*Optimiser constructor*/
    public CInPin(CInPin oldPin, String variantId) {
        super(oldPin, variantId);
        this.reverse = oldPin.reverse;
        this.countMask = oldPin.countMask;
        this.out = oldPin.out;
    }

    @Override
    public void setHi() {
        state = true;
        /*Optimiser line o*/
        if (!reverse) {
            /*Optimiser bind countMask line nr*/
            out.setState((out.state + 1) & countMask);
            /*Optimiser line o*/
        }
    }

    @Override
    public void setLo() {
        state = false;
        /*Optimiser line o*/
        if (reverse) {
            /*Optimiser bind countMask line r*/
            out.setState((out.state + 1) & countMask);
            /*Optimiser line o*/
        }
    }

    @Override
    public InPin getOptimised(boolean keepSetters) {
        ClassOptimiser<CInPin> optimiser = new ClassOptimiser<>(this).cut("o").bind("countMask", countMask);
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        CInPin build = optimiser.build();
        ((Counter) parent).in = build;
        parent.inPins.put(id, build);
        return build;
    }
}
