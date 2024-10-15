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
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MultiPartCIn extends InPin {
    public final boolean reverse;
    public final long countMask;
    public final int partNo;
    public final MultiPartCounter parent;
    public final int size;
    public final long skipMask;
    public Bus outBus;
    public Pin outPin;

    public MultiPartCIn(String id, MultiPartCounter parent, boolean reverse, int size, int partNo, int skipMask) {
        super(id, parent);
        this.parent = parent;
        this.size = size;
        this.skipMask = skipMask;
        if (size == 1) {
            outPin = parent.getOutPin("Q" + (char) ('a' + partNo));
        } else {
            outBus = parent.getOutBus("Q" + (char) ('a' + partNo));
        }
        this.reverse = reverse;
        this.countMask = Utils.getMaskForSize(size);
        this.partNo = partNo;
    }

    @SuppressWarnings("unused")
    /*Optimiser constructor*///
    public MultiPartCIn(MultiPartCIn oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        countMask = oldPin.countMask;
        outBus = oldPin.outBus;
        partNo = oldPin.partNo;
        size = oldPin.size;
        skipMask = oldPin.skipMask;
        parent = oldPin.parent;
    }

    @Override
    public void setHi() {
        state = true;
        /*Optimiser block nr block hasR*///
        if (
            /*Optimiser line o*///
                !reverse &&//
                        parent.resetState != 0//
        ) {
            /*Optimiser line o blockEnd hasR*/
            if (size == 1) {
                /*Optimiser line pin*/
                if (outPin.state) {
                    outPin.setLo();
                } else {
                    outPin.setHi();
                }
                /*Optimiser line o*/
            } else if (skipMask != 0) {
                /*Optimiser line skip bind skip:skipMask*///
                outBus.setState(outBus.state + (((outBus.state & skipMask) == skipMask) ? 2 : 1));
                /*Optimiser line o*/
            } else {
                /*Optimiser line bus bind countMask*///
                outBus.setState((outBus.state + 1) & countMask);
                /*Optimiser line o*/
            }
            /*Optimiser line nasR*/
        }
        /*Optimiser blockEnd nr*/
    }

    @Override
    public void setLo() {
        state = false;
        /*Optimiser block r block hasR*/
        if (
            /*Optimiser line o*///
                reverse &&//
                        parent.resetState != 0//
        ) {
            /*Optimiser line o blockEnd hasR*/
            if (size == 1) {
                /*Optimiser block pin*/
                if (outPin.state) {
                    outPin.setLo();
                } else {
                    outPin.setHi();
                }
                /*Optimiser line o blockEnd pin*/
            } else if (skipMask != 0) {
                /*Optimiser line skip bind skip:skipMask*///
                outBus.setState(outBus.state + (((outBus.state & skipMask) == skipMask) ? 2 : 1));
                /*Optimiser line o*/
            } else {
                /*Optimiser line bus bind countMask*///
                outBus.setState((outBus.state + 1) & countMask);
                /*Optimiser line o*/
            }
            /*Optimiser line hasR*/
        }
        /*Optimiser blockEnd r*/
    }

    public void reset() {
        if (size == 1) {
            outPin.setLo();
        } else {
            outBus.setState(0);
        }
    }

    @Override
    public InPin getOptimised(boolean keepSetters) {
        ClassOptimiser<MultiPartCIn> optimiser = new ClassOptimiser<>(this).cut("o");
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (size == 1) {
            optimiser.cut("bus").cut("skip");
        } else if (skipMask != 0) {
            optimiser.cut("bus").cut("pin").bind("skip", skipMask);
        } else {
            optimiser.bind("countMask", countMask).cut("pin").cut("skip");
        }
        if (parent.rIns.values()
                .stream()
                .noneMatch(p -> p.used)) {
            optimiser.cut("hasR");
        }
        MultiPartCIn build = optimiser.build();
        parent.cIns[partNo] = build;
        parent.inPins.put(id, build);
        for (MultiPartRIn rPin : parent.rIns.values()) {
            rPin.cIns[partNo] = build;
        }
        return build;
    }
}
