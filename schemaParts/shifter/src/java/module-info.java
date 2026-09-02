/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.shifter.ShifterSpi;
import pko.KiCadLogicalSchemeSimulator.components.shifter.singleOut.SingleOutShifterSpi;

open module KiCadLogicalSchemeSimulator.components.shifter {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with SingleOutShifterSpi, ShifterSpi;
}