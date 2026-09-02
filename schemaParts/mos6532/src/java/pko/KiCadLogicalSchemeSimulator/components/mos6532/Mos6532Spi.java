/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.mos6532;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;

public class Mos6532Spi implements SchemaPartSpi {
    @Override
    public SchemaPart getSchemaPart(String id, String params) {
        return new Mos6532(id, params);
    }

    @Override
    public Class<? extends SchemaPart> getSchemaPartClass() {
        return Mos6532.class;
    }
}
