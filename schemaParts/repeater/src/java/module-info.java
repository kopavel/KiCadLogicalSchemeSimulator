/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.repeater.RepeaterSpi;

open module KiCadLogicalSchemeSimulator.components.repeater {
    requires KiCadLogicalSchemeSimulator.simulator;
    exports pko.KiCadLogicalSchemeSimulator.components.repeater;
    provides SchemaPartSpi with RepeaterSpi;
}