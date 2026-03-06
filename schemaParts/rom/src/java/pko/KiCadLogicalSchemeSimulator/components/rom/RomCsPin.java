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
package pko.KiCadLogicalSchemeSimulator.components.rom;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class RomCsPin extends InPin {
    protected final int[] words;
    protected final Rom parent;
    public RomABus aBus;
    public Bus dBus;

    public RomCsPin(String id, Rom parent) {
        super(parent.reverse ? "~{" + id + "}" : id, parent);
        this.parent = parent;
        aBus = parent.aBus;
        words = parent.words;
        priority = 1;
    }

    /*Optimiser constructor*/
    public RomCsPin(RomCsPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        words = oldPin.words;
        dBus = oldPin.dBus;
        aBus = oldPin.aBus;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line useI*/
        aBus.iCsActive += parent.reverse ? 1 : -1;
        /*Optimiser line useB bind bHi:aBus.iCsActive\s==\s0*/
        aBus.bCsActive = aBus.iCsActive == 0;
        /*Optimiser line o block reverse*/
        if (parent.reverse) {
            if (!dBus.hiImpedance) {
                dBus.setHiImpedance();
            }
            /*Optimiser line o bockEnd reverse block nReverse*/
        } else {
            int word;
            Bus bus;
            if ((bus = dBus).state != (word = words[aBus.state]) && aBus.bCsActive || bus.hiImpedance) {
                bus.setState(word);
            }
            /*Optimiser line o blockEnd nReverse*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line useI*/
        aBus.iCsActive += parent.reverse ? -1 : 1;
        /*Optimiser line useB bLo bState:aBus.iCsActive\s==\s0*/
        aBus.bCsActive = aBus.iCsActive == 0;
        Bus bus;
        int word;
        /*Optimiser line o block reverse*/
        if (parent.reverse) {
            if ((bus = dBus).state != (word = words[aBus.state]) && aBus.bCsActive || bus.hiImpedance) {
                bus.setState(word);
            }
            /*Optimiser line o bockEnd reverse block nReverse*/
        } else {
            if (!(bus = dBus).hiImpedance) {
                bus.setHiImpedance();
            }
            /*Optimiser line o blockEnd nReverse*/
        }
    }

    @Override
    public RomCsPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<RomCsPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (source != null) {
            optimiser.cut("setter");
        }
        if (parent.csCount == 1) {
            optimiser.cut("useI");
            optimiser.bind("bHi", !parent.reverse);
            optimiser.bind("bLo", parent.reverse);
        } else {
            optimiser.cut("useB");
        }
        optimiser.cut(parent.reverse ? "nReverse" : "reverse");
        RomCsPin build = optimiser.build();
        build.source = source;
        parent.replaceIn(this, build);
        RomCsPin[] csPins = parent.csPins;
        for (int i = 0; i < csPins.length; i++) {
            if (csPins[i] == this) {
                csPins[i] = build;
            }
        }
        return build;
    }
}
