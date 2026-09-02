/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.display.DisplaySpi;

module KiCadLogicalSchemeSimulator.components.display {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    provides SchemaPartSpi with DisplaySpi;
}