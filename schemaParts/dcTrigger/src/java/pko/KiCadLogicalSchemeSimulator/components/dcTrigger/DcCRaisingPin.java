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
import pko.KiCadLogicalSchemeSimulator.api.wire.RaisingEdgePin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class DcCRaisingPin extends RaisingEdgePin {
    public final DcTrigger parent;
    public final InPin dPin;
    public Pin qOut;
    public Pin iqOut;

    public DcCRaisingPin(String id, DcTrigger parent) {
        super(id, parent);
        this.parent = parent;
        dPin = parent.dPin;
        qOut = parent.qOut;
        iqOut = parent.iqOut;
    }

    /*Optimiser constructor*/
    public DcCRaisingPin(DcCRaisingPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        dPin = oldPin.dPin;
        qOut = oldPin.qOut;
        iqOut = oldPin.iqOut;
    }

    @Override
    public void setHi() {
        Pin lPin;
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line anyRS*/
        if (parent.clockEnabled) {
            if (dPin.state) {
                /*Optimiser block q bind nq:!qOut*/
                if (!(lPin = qOut).state) {
                    lPin.setHi();
                    /*Optimiser line bothRS block nq*/
                }
                /*Optimiser line bothRSQ blockEnd q*/
                if (iqOut.state) {
                    iqOut.setLo();
                    /*Optimiser blockEnd nq*/
                }
                //noinspection UnnecessaryReturnStatement
                return;
            } else {
                /*Optimiser block q*/
                if ((lPin = qOut).state) {
                    lPin.setLo();
                    /*Optimiser line bothRS block nq*/
                }
                /*Optimiser line bothRSQ blockEnd q*/
                if (!iqOut.state) {
                    iqOut.setHi();
                    /*Optimiser blockEnd nq*/
                }
            }
            /*Optimiser line anyRS*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        boolean anyRs = parent.rPin.used || parent.sPin.used;
        boolean bothRs = parent.rPin.used && parent.sPin.used;
        ClassOptimiser<DcCRaisingPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (!anyRs) {
            optimiser.cut("anyRS");
        }
        if (!bothRs) {
            optimiser.cut("bothRS");
            if (qOut.used) {
                optimiser.cut("bothRSQ");
                if (iqOut.used) {
                    optimiser.bind("nq", "iqOut");
                }
            }
        }
        if (source != null) {
            optimiser.cut("setter");
        }
        if (!iqOut.used) {
            optimiser.cut("nq");
        } else if (!qOut.used) {
            optimiser.cut("q");
        }
        DcCRaisingPin build = optimiser.build();
        build.withState = source == null;
        parent.cPin = build;
        parent.replaceIn(this, build);
        build.source = source;
        return build;
    }
}
