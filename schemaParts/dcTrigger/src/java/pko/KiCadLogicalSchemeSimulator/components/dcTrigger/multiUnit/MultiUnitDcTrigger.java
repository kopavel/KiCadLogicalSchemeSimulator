/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger.multiUnit;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

//FixMe make unittest
public class MultiUnitDcTrigger extends SchemaPart {
    private final int size;
    public MultiUnitDcCPin cPin;
    public MultiUnitDcRPin rPin;
    public boolean clockEnabled = true;

    protected MultiUnitDcTrigger(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " has no parameter \"size\"");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " size must be >1");
        }
        if (size < 2) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " size must be >1");
        }
        InPin[] dPin = new InPin[size];
        Pin[] qOut = new Pin[size];
        Pin[] iqOut = new Pin[size];
        for (int i = 0; i < size; i++) {
            dPin[i] = addInPin("D" + (char) ('a' + i));
            addOutPin("Q" + (char) ('a' + i), false);
            addOutPin("~{Q" + (char) ('a' + i) + "}", true);
            qOut[i] = getOutPin("Q" + (char) ('a' + i));
            iqOut[i] = getOutPin("~{Q" + (char) ('a' + i) + "}");
        }
        rPin = addInPin(new MultiUnitDcRPin("R", this, params.containsKey("setReverse"), qOut, iqOut));
        cPin = addInPin(new MultiUnitDcCPin("C", this, dPin, qOut, iqOut));
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < size; i++) {
            cPin.pins[i].qOut = getOutPin("Q" + (char) ('a' + i));
            cPin.pins[i].iqOut = getOutPin("~{Q" + (char) ('a' + i) + "}");
            rPin.pins[i].qOut = cPin.pins[i].qOut;
            rPin.pins[i].iqOut = cPin.pins[i].iqOut;
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < size; i++) {
            cPin.pins[i].qOut.setLo();
            cPin.pins[i].iqOut.setHi();
        }
    }
}
