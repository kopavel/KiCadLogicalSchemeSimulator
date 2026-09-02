/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.keyboard;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;

public class KeyboardSpy implements SchemaPartSpi {
    @Override
    public SchemaPart getSchemaPart(String id, String params) {
        return new Keyboard(id, params);
    }

    @Override
    public Class<? extends SchemaPart> getSchemaPartClass() {
        return Keyboard.class;
    }
}
