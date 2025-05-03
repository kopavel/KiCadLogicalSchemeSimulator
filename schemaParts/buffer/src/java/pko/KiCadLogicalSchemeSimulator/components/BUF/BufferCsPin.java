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

package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class BufferCsPin extends InPin {
    public Bus qBus;
    public Bus dBus;
    public Buffer parent;

    public BufferCsPin(String id, Buffer parent, Bus dBus) {
        super(id, parent);
        this.parent = parent;
        this.qBus = parent.getOutBus("Q");
        this.dBus = dBus;
    }

    /*Optimiser constructor*/
    public BufferCsPin(BufferCsPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        qBus = oldPin.qBus;
        dBus = oldPin.dBus;
    }

    @Override
    public void setHi() {
        state = true;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            if (!qBus.hiImpedance) {
                qBus.setHiImpedance();
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            Bus bus;
            int state;
            if ((bus = qBus).state != (state = dBus.state) || bus.hiImpedance) {
                bus.setState(state);
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        state = false;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            Bus bus;
            if ((bus = qBus).state != dBus.state || bus.hiImpedance) {
                bus.setState(dBus.state);
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            if (!(qBus).hiImpedance) {
                qBus.setHiImpedance();
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<BufferCsPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        BufferCsPin build = optimiser.build();
        build.source = source;
        parent.dBus.csPin = build;
        parent.csPin = build;
        parent.replaceIn(this, build);
        return build;
    }
}
