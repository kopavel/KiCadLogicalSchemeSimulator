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
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.RaisingEdgePin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class CInRaisingPin extends RaisingEdgePin {
    public final int countMask;
    private final Counter parent;
    public Bus out;

    public CInRaisingPin(String id, Counter parent, int countMask) {
        super(id, parent);
        out = parent.getOutBus("Q");
        this.countMask = countMask;
        this.parent = parent;
    }

    /*Optimiser constructor*/
    public CInRaisingPin(CInRaisingPin oldPin, String variantId) {
        super(oldPin, variantId);
        this.countMask = oldPin.countMask;
        this.out = oldPin.out;
        parent = oldPin.parent;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line r*/
        if (parent.enabled) {
            /*Optimiser bind countMask*/
            out.setState((out.state + 1) & countMask);
            /*Optimiser line r*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<CInRaisingPin> optimiser = new ClassOptimiser<>(this).bind("countMask", countMask);
        if (source != null) {
            optimiser.cut("setter");
        } else {
            optimiser.byteCodeManipulator("setLo").dup(1);
        }
        if (!parent.rPin.used) {
            optimiser.cut("r");
        }
        CInRaisingPin build = optimiser.build();
        ((Counter) parent).in = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
