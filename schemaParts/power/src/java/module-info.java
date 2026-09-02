/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;

module KiCadLogicalSchemeSimulator.components.power {
    exports pko.KiCadLogicalSchemeSimulator.components.power;
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with pko.KiCadLogicalSchemeSimulator.components.power.PowerSpi;
}