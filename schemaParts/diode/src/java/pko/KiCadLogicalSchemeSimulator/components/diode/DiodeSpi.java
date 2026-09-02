/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.diode;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;

public class DiodeSpi implements SchemaPartSpi {
    @Override
    public SchemaPart getSchemaPart(String id, String params) {
        return new pko.KiCadLogicalSchemeSimulator.components.diode.Diode(id, params);
    }

    @Override
    public Class<? extends SchemaPart> getSchemaPartClass() {
        return pko.KiCadLogicalSchemeSimulator.components.diode.Diode.class;
    }
}
