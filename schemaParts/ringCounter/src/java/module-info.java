/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.ringCounter.RingCounterSpi;

module KiCadLogicalSchemeSimulator.components.ringCounter {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with RingCounterSpi;
}