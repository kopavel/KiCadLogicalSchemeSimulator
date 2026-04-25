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
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class RomABus extends InBus {
    protected final int[] words;
    protected final Rom parent;
    public Bus dBus;
    public int iCsActive;
    public boolean bCsActive;

    public RomABus(String id, Rom parent, int size, String... names) {
        super(id, parent, size, names);
        words = parent.words;
        this.parent = parent;
        iCsActive = parent.csCount;
    }

    /*Optimiser constructor*/
    public RomABus(RomABus oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        words = oldPin.words;
        iCsActive = oldPin.iCsActive;
    }

    @Override
    public void setState(int newState) {
        state = newState;
        int word;
        if (
            /*Optimiser line useI*/
                iCsActive == 0 &&
                        /*Optimiser line useB*/
                        bCsActive &&//
                        dBus.state != (word = words[newState])) {
            dBus.setState(word);
        }
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        ClassOptimiser<RomABus> optimiser = new ClassOptimiser<>(this);
        if (source != null) {
            optimiser.cut("setter");
        }
        if (parent.csCount == 1) {
            optimiser.cut("useI");
        } else {
            optimiser.cut("useB");
        }
        RomABus build = optimiser.build();
        build.source = source;
        build.withState=source!=null;
        parent.replaceIn(this, build);
        parent.aBus = build;
        for (RomCsPin csPin : parent.csPins) {
            csPin.aBus = build;
        }
        return build;
    }
}
