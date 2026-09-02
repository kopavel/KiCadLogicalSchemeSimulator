/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.Mos6502Spi;

module KiCadLogicalSchemeSimulator.components.mos6502Cpu {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    requires static lombok;
    provides SchemaPartSpi with Mos6502Spi;
}