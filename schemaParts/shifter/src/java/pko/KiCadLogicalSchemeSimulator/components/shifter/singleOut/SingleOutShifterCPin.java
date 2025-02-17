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
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class SingleOutShifterCPin extends InPin {
    public final SingleOutShifter parent;
    public final InPin dsPins;
    public final InBus dBus;
    private final boolean reverse;
    private final boolean cn;
    public Pin out;

    public SingleOutShifterCPin(String id, SingleOutShifter parent, boolean reverse, boolean cn) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        dsPins = parent.dsPins;
        dBus = parent.dBus;
        out = parent.out;
        this.cn = cn;
    }

    /*Optimiser constructor*/
    public SingleOutShifterCPin(SingleOutShifterCPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        reverse = oldPin.reverse;
        dsPins = oldPin.dsPins;
        dBus = oldPin.dBus;
        out = oldPin.out;
        cn = oldPin.cn;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser block nr line o*/
        if (!reverse) {
            SingleOutShifter lParent;
            if ((lParent = parent).clockEnabled) {
                long l;
                if (lParent.parallelLoad) {
                    l = dBus.state;
                } else {
                    if ((l = lParent.latch) != 0) {
                        l = //
                                /*Optimiser line o*/
                                cn ?//
                                        /*Optimiser line cn*///
                                l >> 1//
                                        /*Optimiser line o*///
                                   :
                                           /*Optimiser line cp bind lm:lParent.latchMask*///
                                (l << 1) & lParent.latchMask //
                        ;
                    }
                    if (dsPins.state) {
                        l |= //
                                /*Optimiser line o*/
                                cn ?//
                                        /*Optimiser line cn bind hm:lParent.hiDsMask*///
                                lParent.hiDsMask//
                                        /*Optimiser line o*///
                                   ://
                                           /*Optimiser line cp*///
                                1L//
                        ;
                    }
                }
                boolean state;
                Pin lOut;
                /*Optimiser bind om:lParent.outMask*/
                if ((state = (lOut = out).state) == ((l & lParent.outMask) == 0L)) {
                    if (state) {
                        lOut.setLo();
                    } else {
                        lOut.setHi();
                    }
                }
                lParent.latch = l;
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser block r line o*/
        if (reverse) {
            SingleOutShifter lParent;
            if ((lParent = parent).clockEnabled) {
                long l;
                if (lParent.parallelLoad) {
                    l = dBus.state;
                } else {
                    if ((l = lParent.latch) != 0) {
                        l = //
                                /*Optimiser line o*/
                                cn ?//
                                        /*Optimiser line cn*///
                                l >> 1//
                                        /*Optimiser line o*///
                                   :
                                           /*Optimiser line cp bind lm:lParent.latchMask*///
                                (l << 1) & lParent.latchMask //
                        ;
                    }
                    if (dsPins.state) {
                        l |= //
                                /*Optimiser line o*/
                                cn ?//
                                        /*Optimiser line cn bind hm:lParent.hiDsMask*///
                                lParent.hiDsMask//
                                        /*Optimiser line o*///
                                   ://
                                           /*Optimiser line cp*///
                                1L//
                        ;
                    }
                }
                boolean state;
                Pin lOut;
                /*Optimiser bind om:lParent.outMask*/
                if ((state = (lOut = out).state) == ((l & lParent.outMask) == 0L)) {
                    if (state) {
                        lOut.setLo();
                    } else {
                        lOut.setHi();
                    }
                }
                lParent.latch = l;
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<SingleOutShifterCPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (source != null) {
            optimiser.cut("setter");
        }
        optimiser.bind("om", parent.outMask + "L");
        optimiser.bind("lm", parent.latchMask + "L");
        optimiser.bind("hm", parent.hiDsMask + "L");
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (cn) {
            optimiser.cut("cp");
        } else {
            optimiser.cut("cn");
        }
        SingleOutShifterCPin build = optimiser.build();
        if (cn) {
            parent.cn = build;
        } else {
            parent.cp = build;
        }
        parent.replaceIn(this, build);
        return build;
    }
}
