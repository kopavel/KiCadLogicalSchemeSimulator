/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

//FixME in case of DcTrigger chain on single clock - need C priority in reverse chain order.
public class DcTrigger extends SchemaPart {
    public final InPin dPin;
    public DcRPin rPin;
    public DcSPin sPin;
    public DcCRaisingPin cPin;
    public DcCFallingPin ncPin;
    public Pin qOut;
    public Pin iqOut;
    public boolean clockEnabled = true;

    protected DcTrigger(String id, String sParam) {
        super(id, sParam);
        addOutPin("Q", false);
        addOutPin("~{Q}", true);
        qOut = getOutPin("Q");
        iqOut = getOutPin("~{Q}");
        dPin = addInPin("D");
        rPin = addInPin(new DcRPin("R", this, params.containsKey("setReverse")));
        sPin = addInPin(new DcSPin("S", this, params.containsKey("setReverse")));
        rPin.sPin = sPin;
        if (reverse) {
            ncPin = addInPin(new DcCFallingPin("C", this));
        } else {
            cPin = addInPin(new DcCRaisingPin("C", this));
        }
    }

    @Override
    public void initOuts() {
        qOut = getOutPin("Q");
        iqOut = getOutPin("~{Q}");
        rPin.qOut = qOut;
        rPin.iqOut = iqOut;
        sPin.qOut = qOut;
        sPin.iqOut = iqOut;
        if (reverse) {
            ncPin.qOut = qOut;
            ncPin.iqOut = iqOut;
        } else {
            cPin.qOut = qOut;
            cPin.iqOut = iqOut;
        }
    }

    @Override
    public void reset() {
        if (clockEnabled) {
            qOut.setLo();
            iqOut.setHi();
        }
    }
}
