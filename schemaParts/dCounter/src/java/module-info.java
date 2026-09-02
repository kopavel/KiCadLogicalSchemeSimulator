/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.dCounter.DCounterSpi;

module KiCadLogicalSchemeSimulator.components.dCounter {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with DCounterSpi;
}