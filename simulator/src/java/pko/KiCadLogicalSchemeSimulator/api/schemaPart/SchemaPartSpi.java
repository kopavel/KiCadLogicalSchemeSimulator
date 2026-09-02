/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.schemaPart;
public interface SchemaPartSpi {
    SchemaPart getSchemaPart(String id, String params);
    Class<? extends SchemaPart> getSchemaPartClass();
}
