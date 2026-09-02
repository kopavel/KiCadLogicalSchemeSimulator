/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
                int l;
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
                                1//
                        ;
                    }
                }
                boolean state;
                /*Optimiser bind om:lParent.outMask*/
                if ((state = out.state) == ((l & lParent.outMask) == 0L)) {
                    if (state) {
                        out.setLo();
                    } else {
                        out.setHi();
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
                int l;
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
                                1//
                        ;
                    }
                }
                boolean state;
                /*Optimiser bind om:lParent.outMask*/
                if ((state = out.state) == ((l & lParent.outMask) == 0L)) {
                    if (state) {
                        out.setLo();
                    } else {
                        out.setHi();
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
        optimiser.bind("om", parent.outMask);
        optimiser.bind("lm", parent.latchMask);
        optimiser.bind("hm", parent.hiDsMask);
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
        build.withState = source == null;
        if (cn) {
            parent.cn = build;
        } else {
            parent.cp = build;
        }
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
