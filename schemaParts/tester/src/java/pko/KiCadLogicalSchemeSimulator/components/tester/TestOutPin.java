/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.tester;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class TestOutPin extends SchemaPart {
    public Pin pin;

    protected TestOutPin(String id, String sParam) {
        super(id, sParam);
        addTriStateOutPin("Out");
    }

    @Override
    public void initOuts() {
        pin = getOutPin("Out");
    }
}
