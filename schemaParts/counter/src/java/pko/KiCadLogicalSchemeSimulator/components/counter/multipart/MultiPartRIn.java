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
package pko.KiCadLogicalSchemeSimulator.components.counter.multipart;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class MultiPartRIn extends InPin {
    public final boolean reverse;
    public final MultiPartCIn[] cIns;
    public final MultiPartCounter parent;
    public final long mask;
    public final long nMask;
    public final int no;

    public MultiPartRIn(String id, MultiPartCounter parent, boolean reverse, int no) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        cIns = parent.cIns;
        mask = 1L << no;
        nMask = ~mask;
        this.no = no;
    }

    @SuppressWarnings("unused")
    /*Optimiser constructor unroll cIn:cIns*///
    public MultiPartRIn(MultiPartRIn oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        parent = oldPin.parent;
        cIns = oldPin.cIns;
        mask = oldPin.mask;
        nMask = oldPin.nMask;
        no = oldPin.no;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o block nr*/
        if (!reverse) {
            /*Optimiser bind mask block and*/
            if (parent.resetState == mask) {
                parent.resetState = 0;
                /*Optimiser blockEnd and*/
                for (MultiPartCIn cIn : cIns) {
                    cIn.reset();
                }
                /*Optimiser block and*/
            } else {
                /*Optimiser bind nMask*/
                parent.resetState &= nMask;
            }
            /*Optimiser blockEnd nr line o block r*/
        } else//
        {
            /*Optimiser bind mask*/
            parent.resetState |= mask;
        }
        /*Optimiser blockEnd and blockEnd r*/
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o block r*/
        if (reverse) {
            /*Optimiser bind mask block and*/
            if (parent.resetState == mask) {
                parent.resetState = 0;
                /*Optimiser blockEnd and*/
                for (MultiPartCIn cIn : cIns) {
                    cIn.reset();
                }
                /*Optimiser block and*/
            } else {
                /*Optimiser bind nMask*/
                parent.resetState &= nMask;
            }
            /*Optimiser blockEnd r block nr line o*/
        } else//
        {
            /*Optimiser bind mask*/
            parent.resetState |= mask;
        }

        /*Optimiser blockEnd and blockEnd nr*/
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<MultiPartRIn> optimiser = new ClassOptimiser<>(this).cut("o").unroll(parent.cIns.length);
        if (parent.rIns.size() == 1) {
            optimiser.cut("and");
        } else {
            optimiser.bind("mask", mask).bind("nMask", nMask);
        }
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        MultiPartRIn build = optimiser.build();
        parent.rIns.put(id, build);
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
