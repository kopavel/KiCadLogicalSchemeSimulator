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
package pko.KiCadLogicalSchemeSimulator.components.shifter.singleOut;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class SingleOutShifterCIPin extends InPin {
    public final SingleOutShifter parent;
    public final boolean inhibitReverse;
    public final boolean clearReverse;
    public SingleOutShifterRPin rPin;

    public SingleOutShifterCIPin(String id, SingleOutShifter parent, boolean inhibitReverse, boolean clearReverse) {
        super(id, parent);
        this.parent = parent;
        this.inhibitReverse = inhibitReverse;
        this.clearReverse = clearReverse;
    }

    /*Optimiser constructor*/
    public SingleOutShifterCIPin(SingleOutShifterCIPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        inhibitReverse = oldPin.inhibitReverse;
        clearReverse = oldPin.clearReverse;
        rPin = oldPin.rPin;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o*/
        if (inhibitReverse) {
            /*Optimiser line r*/
            parent.clockEnabled = false;
            /*Optimiser line o*/
        } else {
            /*Optimiser block n*/
            parent.clockEnabled =
                    /*Optimiser line cr*///
                    !//
                            rPin.state
                            /*Optimiser line o*///
                            ^ !clearReverse//
            ;
            /*Optimiser line o blockEnd n*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o*/
        if (inhibitReverse) {
            /*Optimiser block r*/
            parent.clockEnabled =
                    /*Optimiser line cr*///
                    !//
                            rPin.state
                            /*Optimiser line o*///
                            ^ !clearReverse//
            ;
            /*Optimiser line o blockEnd r*/
        } else {
            /*Optimiser line n*/
            parent.clockEnabled = false;
            /*Optimiser line o*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<SingleOutShifterCIPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (!clearReverse) {
            optimiser.cut("cr");
        }
        if (inhibitReverse) {
            optimiser.cut("n");
        } else {
            optimiser.cut("r");
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        SingleOutShifterCIPin build = optimiser.build();
        parent.rPin.ciPin = build;
        parent.replaceIn(this, build);
        return build;
    }
}
