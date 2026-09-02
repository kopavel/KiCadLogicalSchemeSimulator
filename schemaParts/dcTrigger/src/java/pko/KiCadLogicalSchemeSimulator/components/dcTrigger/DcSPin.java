/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class DcSPin extends InPin {
    public final boolean reverse;
    public final DcTrigger parent;
    public InPin rPin;
    public Pin qOut;
    public Pin iqOut;

    public DcSPin(String id, DcTrigger parent, boolean reverse) {
        super(id, parent);
        this.parent = parent;
        this.reverse = reverse;
        state = reverse;
        rPin = parent.rPin;
        qOut = parent.qOut;
        iqOut = parent.iqOut;
    }

    /*Optimiser constructor*/
    public DcSPin(DcSPin oldPin, String variantId) {
        super(oldPin, variantId);
        reverse = oldPin.reverse;
        parent = oldPin.parent;
        state = oldPin.state;
        rPin = oldPin.rPin;
        qOut = oldPin.qOut;
        iqOut = oldPin.iqOut;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        //noinspection PointlessBooleanExpression
        parent.clockEnabled =
                /*Optimiser line nr*///
                false
                        /*Optimiser line o*///
                        || reverse &&
                        /*Optimiser line r bind false:rPin.state*///
                        rPin.state//
        ;

        /*Optimiser line o block nr*/
        if (!reverse) {
            /*Optimiser block nq*/
            if (
                /*Optimiser line rp*/
                    !rPin.state &&//
                            iqOut.state) {
                iqOut.setLo();
                /*Optimiser line rp block q*/
            }
            /*Optimiser line rq blockEnd nq*/
            if (!qOut.state) {
                /*Optimiser blockEnd rp*/
                qOut.setHi();
                /*Optimiser blockEnd q*/
            }
            /*Optimiser block rp block r blockEnd nr blockEnd q line o*/
        } else//
            if (!rPin.state) {
                /*Optimiser block nq*/
                if (qOut.state) {
                    qOut.setLo();
                }
                /*Optimiser blockEnd nq block q*/
                if (!iqOut.state) {
                    iqOut.setHi();
                }
                /*Optimiser blockEnd q*/
            }
        /*Optimiser blockEnd r blockEnd rp*/
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        //noinspection PointlessBooleanExpression
        parent.clockEnabled =
                /*Optimiser line nr*///
                true
                        /*Optimiser line o*///
                        && !reverse &&
                        /*Optimiser line r bind true:!rPin.state*///
                        !rPin.state//
        ;
        /*Optimiser line o block r*/
        if (reverse) {
            /*Optimiser block nq*/
            if (
                /*Optimiser line rp*/
                    rPin.state &&//
                            iqOut.state) {
                iqOut.setLo();
                /*Optimiser line rp block q*/
            }
            /*Optimiser line rq blockEnd nq*/
            if (!qOut.state) {
                qOut.setHi();
                /*Optimiser blockEnd q*/
            }
            /*Optimiser line o block rp blockEnd r block nr*/
        } else//
            if (rPin.state) {
                if (qOut.state) {
                    qOut.setLo();
                }
                if (!iqOut.state) {
                    iqOut.setHi();
                }
            }
        /*Optimiser blockEnd rp blockEnd nr*/
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<DcSPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        if (!rPin.used) {
            optimiser.cut("rp");
            if (iqOut.used) {
                optimiser.cut("rq");
            }
            if (reverse) {
                optimiser.bind("true", "false");
                optimiser.bind("false", "true");
            } else {
                optimiser.bind("true", "true");
                optimiser.bind("false", "false");
            }
        }
        if (source != null && !rPin.used) {
            optimiser.cut("setter");
        }
        if (!qOut.used) {
            optimiser.cut("q");
        } else if (!iqOut.used) {
            optimiser.cut("nq");
        }
        DcSPin build = optimiser.build();
        build.withState = source == null;
        parent.sPin = build;
        parent.rPin.sPin = build;
        parent.replaceIn(this, build);
        if (source != null && !rPin.used) {
            build.source = source;
        }
        return build;
    }
}
