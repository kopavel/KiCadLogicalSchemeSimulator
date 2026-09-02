/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.shifter;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;

public class ShifterSpi implements SchemaPartSpi {
    @Override
    public SchemaPart getSchemaPart(String id, String params) {
        return new Shifter(id, params);
    }

    @Override
    public Class<? extends SchemaPart> getSchemaPartClass() {
        return Shifter.class;
    }
}
