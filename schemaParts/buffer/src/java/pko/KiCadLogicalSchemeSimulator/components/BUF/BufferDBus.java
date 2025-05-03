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
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class BufferDBus extends InBus {
    private final Buffer parent;
    public BufferCsPin csPin;
    public Bus qBus;

    public BufferDBus(String id, Buffer parent, int size, String... names) {
        super(id, parent, size, names);
        this.qBus = parent.getOutBus("Q");
        this.parent = parent;
    }

    /*Optimiser constructor*/
    public BufferDBus(BufferDBus oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        qBus = oldPin.qBus;
        csPin = oldPin.csPin;
    }

    @Override
    public void setState(int newState) {
        state = newState;
        Bus bus;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            if (!csPin.state && ((bus = qBus).state != newState || bus.hiImpedance)) {
                bus.setState(newState);
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            if (csPin.state && ((bus = qBus).state != newState || bus.hiImpedance)) {
                bus.setState(newState);
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        ClassOptimiser<BufferDBus> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        BufferDBus build = optimiser.build();
        build.source = source;
        parent.dBus = build;
        parent.csPin.dBus = build;
        parent.replaceIn(this, build);
        return build;
    }
}
