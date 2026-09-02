/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.diode.DiodeSpi;

module KiCadLogicalSchemeSimulator.components.Diode {
    exports pko.KiCadLogicalSchemeSimulator.components.diode;
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with DiodeSpi;
}