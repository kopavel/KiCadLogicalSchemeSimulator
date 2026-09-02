/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.mos6532.M6532PullUpFilter;
import pko.KiCadLogicalSchemeSimulator.components.mos6532.Mos6532Spi;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;

module KiCadLogicalSchemeSimulator.components.mos6532RIOT {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    provides SchemaPartSpi with Mos6532Spi;
    requires KiCadLogicalSchemeSimulator.components.power;
    provides NetFilter with M6532PullUpFilter;
}