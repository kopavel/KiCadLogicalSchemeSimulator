/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.resister.PullResisterFilter;
import pko.KiCadLogicalSchemeSimulator.components.resister.ResisterFilter;
import pko.KiCadLogicalSchemeSimulator.components.resister.ResisterSpi;

module KiCadLogicalSchemeSimulator.components.resister {
    exports pko.KiCadLogicalSchemeSimulator.components.resister;
    requires KiCadLogicalSchemeSimulator.components.power;
    requires KiCadLogicalSchemeSimulator.simulator;
    requires KiCadLogicalSchemeSimulator.schemaParts.led.main;
    provides SchemaPartSpi with ResisterSpi;
    provides NetFilter with PullResisterFilter, ResisterFilter;
}