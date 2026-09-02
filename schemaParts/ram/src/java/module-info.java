/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.ram.RamSpi;
import pko.KiCadLogicalSchemeSimulator.components.ram.SingleBitRamSpi;

module KiCadLogicalSchemeSimulator.components.ram {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    provides SchemaPartSpi with RamSpi, SingleBitRamSpi;
}
